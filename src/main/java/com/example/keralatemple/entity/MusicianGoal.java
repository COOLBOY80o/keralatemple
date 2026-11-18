package com.example.keralatemple.entity;

import com.example.keralatemple.ritual.KeralaClock;
import com.example.keralatemple.ritual.RitualSchedule;
import com.example.keralatemple.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.EnumSet;

/**
 * Plays while a ritual is on: softly and slowly during a pooja, and during a seeveli slow at first, then medium,
 * then fast, while following the melshanthi round the temple. All musicians keep time from the game clock so they
 * stay together.
 */
public class MusicianGoal extends Goal {
    private final MusicianEntity musician;
    private RitualSchedule.Ritual active;
    private long lastCheck = -100;

    public MusicianGoal(MusicianEntity musician) {
        this.musician = musician;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private void refresh() {
        long now = this.musician.level().getGameTime();
        if (now - this.lastCheck < 20) {
            return;
        }
        this.lastCheck = now;
        RitualSchedule.Ritual r = RitualSchedule.current(RitualSchedule.forTemple(this.musician.getTemple()),
                KeralaClock.now().toLocalTime());
        this.active = r != null && r.kind() != RitualSchedule.Kind.CLOSE ? r : null;
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
        this.musician.getNavigation().stop();
    }

    @Override
    public void tick() {
        refresh();
        if (this.active == null || !(this.musician.level() instanceof ServerLevel level)) {
            return;
        }
        boolean seeveli = this.active.kind() == RitualSchedule.Kind.SEEVELI;
        double progress = progress();
        if (seeveli) {
            follow(level);
        } else {
            gather(level);
        }

        // Beat length in ticks: slow, then medium, then fast through the seeveli.
        int interval = !seeveli ? 12 : progress < 0.35D ? 8 : progress < 0.7D ? 5 : 3;
        long now = level.getGameTime();
        if (now % interval != 0 || progress > 0.985D) {
            return;
        }
        int step = (int) ((now / interval) % 16);
        char beat = this.musician.instrument().pattern.charAt(step);
        if (beat == '.') {
            return;
        }
        float loud = beat == 'X' ? 1.0F : 0.6F;
        if (!seeveli) {
            loud *= 0.6F;
        }
        // More musicians together sound fuller.
        int count = level.getEntitiesOfClass(MusicianEntity.class, this.musician.getBoundingBox().inflate(16.0D)).size();
        float volume = Math.min(3.0F, (1.2F + 0.15F * count) * loud);
        play(level, beat == 'X', volume);
        if (beat == 'X') {
            this.musician.swing(this.musician.tickCount % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
            level.sendParticles(ParticleTypes.NOTE, this.musician.getX(), this.musician.getY() + 2.2D,
                    this.musician.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
    }

    private void play(ServerLevel level, boolean accent, float volume) {
        SoundEvent sound;
        float pitch;
        switch (this.musician.instrument()) {
            case CHENDA -> {
                sound = accent ? SoundEvents.NOTE_BLOCK_SNARE.value() : SoundEvents.NOTE_BLOCK_HAT.value();
                pitch = accent ? 0.9F : 1.3F;
            }
            case THIMILA -> {
                sound = SoundEvents.NOTE_BLOCK_BASEDRUM.value();
                pitch = accent ? 1.1F : 1.4F;
            }
            default -> {
                sound = SoundEvents.NOTE_BLOCK_COW_BELL.value();
                pitch = accent ? 1.6F : 1.9F;
            }
        }
        level.playSound(null, this.musician.blockPosition(), sound, SoundSource.RECORDS, volume, pitch);
    }

    /** 0 at the start of the ritual, 1 at its end. */
    private double progress() {
        LocalTime now = KeralaClock.now().toLocalTime();
        double elapsed = (now.toSecondOfDay() - this.active.start().toSecondOfDay() + 86400) % 86400;
        return Math.max(0.0D, Math.min(1.0D, elapsed / (this.active.minutes() * 60.0D)));
    }

    /** During a seeveli, trail the melshanthi, each musician at a slightly different distance. */
    private void follow(ServerLevel level) {
        PriestEntity lead = level.getEntitiesOfClass(PriestEntity.class,
                        this.musician.getBoundingBox().inflate(48.0D), p -> !p.isAssistant() && !(p instanceof MusicianEntity)
                                && p.getClass() == PriestEntity.class)
                .stream().min(Comparator.comparingDouble(p -> p.distanceToSqr(this.musician))).orElse(null);
        if (lead == null) {
            return;
        }
        double reach = 9.0D + (this.musician.getId() % 4) * 3.0D;
        if (NpcNav.goTo(this.musician, lead.blockPosition(), 0.6D, reach)) {
            this.musician.getLookControl().setLookAt(lead);
        }
    }

    /** During a pooja, stand at the Music Station that made this musician (or near the pooja station). */
    private void gather(ServerLevel level) {
        BlockPos station = null;
        for (String tag : this.musician.getTags()) {
            if (tag.startsWith("ktstation:")) {
                try {
                    BlockPos p = BlockPos.of(Long.parseLong(tag.substring("ktstation:".length())));
                    if (level.getBlockState(p).getBlock() instanceof com.example.keralatemple.block.MusicStationBlock) {
                        station = p;
                    }
                } catch (NumberFormatException ignored) {
                    // not one of ours
                }
            }
        }
        int slot = this.musician.getId() % 5 - 2;
        if (station == null) {
            BlockPos here = this.musician.blockPosition();
            station = this.musician.findMarkers(ModBlocks.POOJA_STATION.get()).stream()
                    .min(Comparator.comparingDouble(p -> p.distSqr(here))).orElse(null);
            if (station == null) {
                return;
            }
            station = station.offset(0, 0, 5);
        }
        NpcNav.goTo(this.musician, station.offset(slot, 0, this.musician.getId() % 2), 0.5D, 2.5D);
    }
}
