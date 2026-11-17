package com.example.keralatemple.entity;

import com.example.keralatemple.registry.ModBlocks;
import com.example.keralatemple.ritual.KeralaClock;
import com.example.keralatemple.ritual.RitualSchedule;
import com.example.keralatemple.temple.Temple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Drives a priest through the temple's schedule.
 *  - OPEN / POOJA / CLOSE: go to the Pooja Station (or the temple centre) and perform the ritual.
 *    The keezhshanthi stands beside the sanctum and assists.
 *  - SEEVELI: walk three clockwise circuits through every bali stone, valiya balikkal and Seeveli Waypoint of the
 *    temple, with drumming and bells, then return to the sanctum. The keezhshanthi follows a little behind and
 *    scatters the bali offerings at the stones.
 */
public class RitualGoal extends Goal {
    private static final int LAPS = 3;

    private enum StopType { WAYPOINT, BALI_STONE, VALIYA }

    /** One stop on the procession. min/max are only set for a valiya balikkal. */
    private record Stop(BlockPos target, StopType type, BlockPos min, BlockPos max) {}

    private final PriestEntity priest;

    private List<RitualSchedule.Ritual> schedule = List.of();
    private RitualSchedule.Ritual active;
    private String currentRitualId = "";
    private long lastCheck = -100;
    private long lastScan = -1000;
    private BlockPos station;
    private BlockPos helperSpot;
    private List<Stop> route = List.of();
    private int routeIndex;
    private BlockPos approach;
    private int startDelay;
    private int arrivedTicks;
    private int stuckTicks;

    public RitualGoal(PriestEntity priest) {
        this.priest = priest;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        refresh();
        return this.active != null;
    }

    @Override
    public boolean canContinueToUse() {
        refresh();
        return this.active != null;
    }

    @Override
    public void stop() {
        this.priest.getNavigation().stop();
    }

    /** Re-reads the schedule at most once per second. */
    private void refresh() {
        long now = this.priest.level().getGameTime();
        if (now - this.lastCheck < 20) {
            return;
        }
        this.lastCheck = now;
        this.schedule = RitualSchedule.forTemple(this.priest.getTemple());
        this.active = RitualSchedule.current(this.schedule, KeralaClock.now().toLocalTime());

        if (this.active == null) {
            this.priest.resetAnnouncement();
            this.currentRitualId = "";
            return;
        }
        if (!this.active.id().equals(this.currentRitualId)) {
            this.currentRitualId = this.active.id();
            this.route = List.of();
            this.routeIndex = 0;
            this.approach = null;
            this.arrivedTicks = 0;
            this.stuckTicks = 0;
            this.lastScan = -1000;
            this.startDelay = this.priest.isAssistant() ? 100 : 0;
        }
        this.priest.announce(this.active);
    }

    @Override
    public void tick() {
        refresh();
        if (this.active == null || !(this.priest.level() instanceof ServerLevel level)) {
            return;
        }
        scan(level);

        if (this.active.kind() == RitualSchedule.Kind.SEEVELI) {
            seeveliTick(level);
        } else {
            stationTick(level);
        }
    }

    // ------------------------------------------------------------------ discovery

    private void scan(ServerLevel level) {
        long now = level.getGameTime();
        if (now - this.lastScan < 200) {
            return;
        }
        this.lastScan = now;

        Temple temple = this.priest.getTemple();
        BlockPos here = this.priest.blockPosition();

        BlockPos found = this.priest.findMarkers(ModBlocks.POOJA_STATION.get()).stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(here)))
                .orElse(null);
        if (found == null && temple != null) {
            found = temple.center.above();   // no mat placed: use the marked temple centre
        }
        this.station = found;
        this.helperSpot = this.station == null ? null : findHelperSpot(level, this.station);

        if (this.active.kind() == RitualSchedule.Kind.SEEVELI && this.route.isEmpty()) {
            buildRoute(here, temple);
        }
    }

    /** A free spot one or two blocks from the station where the keezhshanthi can stand. */
    private static BlockPos findHelperSpot(ServerLevel level, BlockPos stationPos) {
        for (int d = 2; d >= 1; d--) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos p = stationPos.relative(dir, d);
                BlockPos below = p.below();
                if (level.getBlockState(p).isAir()
                        && level.getBlockState(p.above()).isAir()
                        && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) {
                    return p;
                }
            }
        }
        return stationPos;
    }

    private void buildRoute(BlockPos here, Temple temple) {
        List<Stop> stops = new ArrayList<>();
        if (temple != null) {
            for (BlockPos p : temple.baliStones) {
                stops.add(new Stop(p, StopType.BALI_STONE, null, null));
            }
            for (Temple.Valiya v : temple.valiya) {
                stops.add(new Stop(v.topCenter(), StopType.VALIYA, v.min(), v.max()));
            }
        }
        for (BlockPos p : this.priest.findMarkers(ModBlocks.SEEVELI_WAYPOINT.get())) {
            stops.add(new Stop(p, StopType.WAYPOINT, null, null));
        }
        if (stops.isEmpty()) {
            this.route = List.of();
            return;
        }

        double cx;
        double cz;
        if (temple != null) {
            cx = temple.center.getX();
            cz = temple.center.getZ();
        } else if (this.station != null) {
            cx = this.station.getX();
            cz = this.station.getZ();
        } else {
            cx = stops.stream().mapToInt(s -> s.target().getX()).average().orElse(here.getX());
            cz = stops.stream().mapToInt(s -> s.target().getZ()).average().orElse(here.getZ());
        }
        final double centreX = cx;
        final double centreZ = cz;

        // Increasing atan2(dz, dx) runs east -> south -> west -> north: clockwise when seen from above.
        stops.sort(Comparator.comparingDouble(s ->
                Math.atan2(s.target().getZ() - centreZ, s.target().getX() - centreX)));

        // Start the circuit at the stop nearest the priest.
        int start = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < stops.size(); i++) {
            double d = stops.get(i).target().distSqr(here);
            if (d < best) {
                best = d;
                start = i;
            }
        }

        List<Stop> full = new ArrayList<>();
        for (int lap = 0; lap < LAPS; lap++) {
            for (int i = 0; i < stops.size(); i++) {
                full.add(stops.get((start + i) % stops.size()));
            }
        }
        this.route = full;
        this.routeIndex = 0;
    }

    // ------------------------------------------------------------------ behaviours

    private void stationTick(ServerLevel level) {
        BlockPos spot = this.priest.isAssistant() && this.helperSpot != null ? this.helperSpot : this.station;
        if (spot == null) {
            return;
        }
        if (!goTo(spot, 0.5D, 1.6D, true)) {
            this.arrivedTicks = 0;
            return;
        }

        this.arrivedTicks++;
        RitualSchedule.Kind kind = this.active.kind();

        if (this.priest.isAssistant()) {
            if (kind != RitualSchedule.Kind.CLOSE && this.arrivedTicks % 60 == 30) {
                this.priest.swing(InteractionHand.MAIN_HAND);
            }
            if (kind != RitualSchedule.Kind.CLOSE && this.arrivedTicks % 120 == 60) {
                play(level, spot, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
            }
            return;
        }

        BlockPos pos = this.station;
        if (this.arrivedTicks == 1) {
            if (kind == RitualSchedule.Kind.OPEN) {
                play(level, pos, SoundEvents.WOODEN_DOOR_OPEN, 1.0F, 0.8F);
            } else if (kind == RitualSchedule.Kind.CLOSE) {
                play(level, pos, SoundEvents.WOODEN_DOOR_CLOSE, 1.0F, 0.8F);
            }
        }
        if (kind != RitualSchedule.Kind.CLOSE && this.arrivedTicks % 80 == 1) {
            play(level, pos, SoundEvents.BELL_BLOCK, 1.2F, 1.1F);
        }
        if (kind != RitualSchedule.Kind.CLOSE && this.arrivedTicks % 40 == 20) {
            this.priest.swing(InteractionHand.MAIN_HAND);
        }
        if (kind != RitualSchedule.Kind.CLOSE && this.arrivedTicks % 10 == 0) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 1, 0.05D, 0.05D, 0.05D, 0.005D);
        }
    }

    private void seeveliTick(ServerLevel level) {
        if (this.startDelay > 0) {
            // The keezhshanthi waits a few seconds so he trails the melshanti.
            this.startDelay--;
            this.priest.getNavigation().stop();
            return;
        }
        if (this.route.isEmpty() || this.routeIndex >= this.route.size()) {
            // No stops, or the procession is complete: return to the sanctum.
            stationTick(level);
            return;
        }

        if (!this.priest.isAssistant()) {
            playRhythm(level);
        }

        Stop stop = this.route.get(this.routeIndex);
        if (this.approach == null) {
            this.approach = approachFor(stop);
        }
        double reachSq = stop.type() == StopType.BALI_STONE ? 3.4D : 1.6D;
        boolean arrived = goTo(this.approach, 0.55D, reachSq, false);

        // Give up on a stop that cannot be reached after about 20 seconds rather than stalling the procession.
        if (arrived || this.stuckTicks > 400) {
            offer(level, stop);
            this.routeIndex++;
            this.approach = null;
            this.stuckTicks = 0;
        }
    }

    /** Where to stand to serve a stop. Bali stones are solid, so the priest stops beside them. */
    private BlockPos approachFor(Stop stop) {
        if (stop.type() != StopType.VALIYA) {
            return stop.target();
        }
        BlockPos min = stop.min();
        BlockPos max = stop.max();
        BlockPos from = this.priest.blockPosition();
        int x = Mth.clamp(from.getX(), min.getX() - 1, max.getX() + 1);
        int z = Mth.clamp(from.getZ(), min.getZ() - 1, max.getZ() + 1);
        boolean insideX = x >= min.getX() && x <= max.getX();
        boolean insideZ = z >= min.getZ() && z <= max.getZ();
        if (insideX && insideZ) {
            x = min.getX() - 1;   // the priest is standing inside the footprint: step out on one side
        }
        return new BlockPos(x, min.getY(), z);
    }

    /** The offering at a stop: bell, hand gesture, and (for bali stones) scattered rice by the keezhshanthi. */
    private void offer(ServerLevel level, Stop stop) {
        BlockPos t = stop.target();
        play(level, t, SoundEvents.BELL_BLOCK, 1.0F, 1.3F);
        this.priest.swing(InteractionHand.MAIN_HAND);

        if (stop.type() == StopType.WAYPOINT) {
            return;
        }
        // If a keezhshanthi is nearby, he makes the offering; otherwise the melshanti does.
        boolean assistantNearby = !this.priest.isAssistant() && !level.getEntitiesOfClass(KeezhshanthiEntity.class,
                this.priest.getBoundingBox().inflate(32.0D)).isEmpty();
        if (assistantNearby) {
            return;
        }
        int count = stop.type() == StopType.VALIYA ? 40 : 14;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.WHEAT_SEEDS)),
                t.getX() + 0.5D, t.getY() + 1.1D, t.getZ() + 0.5D, count, 0.3D, 0.1D, 0.3D, 0.04D);
        play(level, t, SoundEvents.COMPOSTER_FILL, 1.0F, 1.0F);
    }

    /** A simple chenda-style beat while the procession is moving. */
    private void playRhythm(ServerLevel level) {
        long beat = this.priest.tickCount % 16;
        BlockPos pos = this.priest.blockPosition();
        if (beat == 0 || beat == 6 || beat == 10) {
            play(level, pos, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 1.4F, 0.8F);
        } else if (beat == 4 || beat == 12) {
            play(level, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), 1.0F, 1.5F);
        }
    }

    /** Walks towards the block; true once within reachSq. Optionally teleports there if stuck for a minute. */
    private boolean goTo(BlockPos pos, double speed, double reachSq, boolean teleportIfStuck) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;

        if (this.priest.distanceToSqr(x, y, z) < reachSq) {
            this.priest.getNavigation().stop();
            this.stuckTicks = 0;
            return true;
        }

        if (this.priest.getNavigation().isDone() || this.priest.tickCount % 40 == 0) {
            this.priest.getNavigation().moveTo(x, y, z, speed);
        }

        this.stuckTicks++;
        if (teleportIfStuck && this.stuckTicks > 1200) {
            this.priest.getNavigation().stop();
            this.priest.moveTo(x, y, z, this.priest.getYRot(), this.priest.getXRot());
            this.stuckTicks = 0;
        }
        return false;
    }

    private static void play(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, pos, sound, SoundSource.NEUTRAL, volume, pitch);
    }
}
