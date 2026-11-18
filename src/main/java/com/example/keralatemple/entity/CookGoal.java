package com.example.keralatemple.entity;

import com.example.keralatemple.block.AduppuBlock;
import com.example.keralatemple.block.UruliBlock;
import com.example.keralatemple.config.Config;
import com.example.keralatemple.registry.ModBlocks;
import com.example.keralatemple.ritual.KeralaClock;
import com.example.keralatemple.ritual.Nivedyam;
import com.example.keralatemple.ritual.Pantry;
import com.example.keralatemple.ritual.RitualSchedule;
import com.example.keralatemple.temple.Temple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;

/**
 * Cooks the nivedyam the temple assigned to a ritual, starting cookMinutes before it begins:
 * fill the uruli with water and light the fire (stage 1), boil, add the ingredients taken from the nearby
 * chests (stage 2), stir until the ritual starts (stage 3 = ready). The melshanthi then offers it (stage 4).
 * Progress lives in the Temple object, so a second cook does not start over.
 */
public class CookGoal extends Goal {
    private record Plan(RitualSchedule.Ritual ritual, Nivedyam food, int secondsUntil, boolean ongoing) {}

    private final PriestEntity cook;
    private long lastCheck = -100;
    private long lastScan = -1000;
    private Plan plan;
    private BlockPos uruli;

    public CookGoal(PriestEntity cook) {
        this.cook = cook;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        refresh();
        return this.plan != null && this.uruli != null;
    }

    @Override
    public boolean canContinueToUse() {
        refresh();
        return this.plan != null && this.uruli != null;
    }

    @Override
    public void stop() {
        this.cook.getNavigation().stop();
    }

    private void refresh() {
        long now = this.cook.level().getGameTime();
        if (now - this.lastCheck < 20) {
            return;
        }
        this.lastCheck = now;
        this.plan = null;
        Temple temple = this.cook.getTemple();
        if (temple == null || temple.nivedyam.isEmpty()) {
            return;
        }
        LocalTime nowT = KeralaClock.now().toLocalTime();
        int cookSeconds = Config.COOK_MINUTES.get() * 60;
        Plan best = null;
        for (RitualSchedule.Ritual r : RitualSchedule.forTemple(temple)) {
            Nivedyam food = Nivedyam.byId(temple.nivedyam.getOrDefault(r.id(), ""));
            if (food == null) {
                continue;
            }
            boolean on = r.isActive(nowT);
            int until = ((r.start().toSecondOfDay() - nowT.toSecondOfDay()) + 86400) % 86400;
            if (on) {
                until = 0;
            } else if (until > cookSeconds) {
                continue;
            }
            if (best == null || until < best.secondsUntil()) {
                best = new Plan(r, food, until, on);
            }
        }
        this.plan = best;

        if (now - this.lastScan > 200 || this.uruli == null) {
            this.lastScan = now;
            BlockPos here = this.cook.blockPosition();
            this.uruli = this.cook.findMarkers(ModBlocks.URULI.get()).stream()
                    .min(Comparator.comparingDouble(p -> p.distSqr(here))).orElse(null);
        }
    }

    @Override
    public void tick() {
        refresh();
        Temple temple = this.cook.getTemple();
        if (this.plan == null || this.uruli == null || temple == null || !(this.cook.level() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = level.getBlockState(this.uruli);
        if (!(state.getBlock() instanceof UruliBlock)) {
            this.uruli = null;
            return;
        }

        String key = KeralaClock.now().toLocalDate() + "/" + this.plan.ritual().id();
        if (!key.equals(temple.cookKey)) {
            temple.cookKey = key;
            temple.cookStage = 0;
            temple.cookMissing = "";
        }
        if (temple.cookStage >= 3) {
            this.cook.getNavigation().stop();
            return;
        }
        if (!NpcNav.goTo(this.cook, this.uruli, 0.5D, 5.0D)) {
            return;
        }

        double progress = this.plan.ongoing() ? 1.0D
                : 1.0D - this.plan.secondsUntil() / (double) (Config.COOK_MINUTES.get() * 60);
        double x = this.uruli.getX() + 0.5D;
        double y = this.uruli.getY() + 0.8D;
        double z = this.uruli.getZ() + 0.5D;
        this.cook.getLookControl().setLookAt(x, y, z);

        switch (temple.cookStage) {
            case 0 -> {
                setContent(level, UruliBlock.Content.WATER);
                setFire(level, true);
                level.playSound(null, this.uruli, SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.0F);
                this.cook.swing(InteractionHand.MAIN_HAND);
                temple.cookStage = 1;
            }
            case 1 -> {
                if (progress >= 0.3D && this.cook.tickCount % 6 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 3, 0.25D, 0.02D, 0.25D, 0.01D);
                }
                if (progress >= 0.5D) {
                    startCooking(level, temple);
                }
            }
            case 2 -> {
                if (this.cook.tickCount % 20 == 0) {
                    this.cook.swing(InteractionHand.MAIN_HAND);
                    level.playSound(null, this.uruli, SoundEvents.COMPOSTER_FILL, SoundSource.NEUTRAL, 0.7F, 0.9F);
                }
                if (this.cook.tickCount % 8 == 0) {
                    level.sendParticles(ParticleTypes.SMOKE, x, y + 0.2D, z, 1, 0.2D, 0.05D, 0.2D, 0.01D);
                }
                if (progress >= 1.0D) {
                    setContent(level, UruliBlock.Content.DONE);
                    setFire(level, false);
                    level.playSound(null, this.uruli, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.5F);
                    temple.cookStage = 3;
                    temple.cookMissing = "";
                }
            }
            default -> { }
        }
    }

    /** Adds the ingredients from the nearby chests; waits (and shows an angry cloud) if something is missing. */
    private void startCooking(ServerLevel level, Temple temple) {
        Nivedyam food = this.plan.food();
        boolean ok = !Config.COOK_NEEDS_INGREDIENTS.get() || Pantry.takeAll(level, this.uruli, food.ingredients());
        if (!ok) {
            temple.cookMissing = food.label + " needs: " + Pantry.missing(level, this.uruli, food.ingredients());
            if (this.cook.tickCount % 40 == 0) {
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.cook.getX(), this.cook.getY() + 2.0D,
                        this.cook.getZ(), 2, 0.2D, 0.1D, 0.2D, 0.0D);
            }
            return;
        }
        temple.cookMissing = "";
        temple.cookStage = 2;
        setContent(level, UruliBlock.Content.COOKING);
        level.playSound(null, this.uruli, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.NEUTRAL, 1.0F, 0.8F);
    }

    private void setContent(ServerLevel level, UruliBlock.Content content) {
        BlockState s = level.getBlockState(this.uruli);
        if (s.getBlock() instanceof UruliBlock) {
            level.setBlock(this.uruli, s.setValue(UruliBlock.CONTENT, content), 3);
        }
    }

    private void setFire(ServerLevel level, boolean lit) {
        BlockPos hearth = this.uruli.below();
        BlockState s = level.getBlockState(hearth);
        if (s.getBlock() instanceof AduppuBlock && s.getValue(AduppuBlock.LIT) != lit) {
            level.setBlock(hearth, s.setValue(AduppuBlock.LIT, lit), 3);
        }
    }
}
