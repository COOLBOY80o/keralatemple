package com.example.keralatemple.entity;

import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.config.Config;
import com.example.keralatemple.registry.ModBlocks;
import com.example.keralatemple.ritual.Flower;
import com.example.keralatemple.ritual.KeralaClock;
import com.example.keralatemple.ritual.Pantry;
import com.example.keralatemple.ritual.RitualSchedule;
import com.example.keralatemple.temple.Temple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * At each pooja the kazhakam brings the temple's chosen flowers from the chests beside the pooja station and
 * hangs them as a garland on the idols near it. At nada adakkal the garlands are taken down.
 */
public class KazhakamGoal extends Goal {
    private final PriestEntity me;
    private long lastCheck = -100;
    private long lastScan = -1000;
    private RitualSchedule.Ritual active;
    private BlockPos station;
    private String localKey = "";

    public KazhakamGoal(PriestEntity me) {
        this.me = me;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        refresh();
        return this.active != null && this.station != null;
    }

    @Override
    public boolean canContinueToUse() {
        refresh();
        return this.active != null && this.station != null;
    }

    @Override
    public void stop() {
        this.me.getNavigation().stop();
    }

    private void refresh() {
        long now = this.me.level().getGameTime();
        if (now - this.lastCheck < 20) {
            return;
        }
        this.lastCheck = now;
        RitualSchedule.Ritual current = RitualSchedule.current(RitualSchedule.forTemple(this.me.getTemple()),
                KeralaClock.now().toLocalTime());
        this.active = current != null && (current.kind() == RitualSchedule.Kind.POOJA
                || current.kind() == RitualSchedule.Kind.CLOSE) ? current : null;
        if (this.active != null && now - this.lastScan > 200) {
            this.lastScan = now;
            BlockPos here = this.me.blockPosition();
            BlockPos found = this.me.findMarkers(ModBlocks.POOJA_STATION.get()).stream()
                    .min(Comparator.comparingDouble(p -> p.distSqr(here))).orElse(null);
            Temple t = this.me.getTemple();
            this.station = found != null ? found : (t != null ? t.center.above() : null);
        }
    }

    @Override
    public void tick() {
        refresh();
        if (this.active == null || this.station == null || !(this.me.level() instanceof ServerLevel level)) {
            return;
        }
        if (!NpcNav.goTo(this.me, this.station, 0.5D, 6.0D)) {
            return;
        }
        Temple temple = this.me.getTemple();
        String key = KeralaClock.now().toLocalDate() + "/" + this.active.id();
        String done = temple != null ? temple.garlandKey : this.localKey;
        if (key.equals(done)) {
            return;
        }
        if (temple != null) {
            temple.garlandKey = key;
        } else {
            this.localKey = key;
        }

        if (this.active.kind() == RitualSchedule.Kind.CLOSE) {
            for (IdolBlockEntity idol : idolsNear(level)) {
                idol.setGarland(new byte[0]);
            }
            return;
        }

        List<Byte> chosen = new ArrayList<>();
        for (Flower f : Flower.values()) {
            if (temple != null && !temple.flowers.contains(f.id)) {
                continue;
            }
            if (!Config.FLOWERS_NEED_STOCK.get() || Pantry.takeOne(level, this.station, f.item())) {
                chosen.add(f.code());
            }
        }
        if (chosen.isEmpty()) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.me.getX(), this.me.getY() + 2.0D, this.me.getZ(),
                    2, 0.2D, 0.1D, 0.2D, 0.0D);
            return;
        }
        byte[] codes = new byte[chosen.size()];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = chosen.get(i);
        }
        for (IdolBlockEntity idol : idolsNear(level)) {
            idol.setGarland(codes);
        }
        this.me.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.station.getX() + 0.5D, this.station.getY() + 1.5D,
                this.station.getZ() + 0.5D, 8, 0.4D, 0.3D, 0.4D, 0.0D);
    }

    private List<IdolBlockEntity> idolsNear(ServerLevel level) {
        List<IdolBlockEntity> list = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(this.station.offset(-6, -3, -6), this.station.offset(6, 3, 6))) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof IdolBlockEntity idol && idol.getData() != null) {
                list.add(idol);
            }
        }
        return list;
    }
}
