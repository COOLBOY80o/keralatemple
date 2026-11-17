package com.example.keralatemple.entity;

import com.example.keralatemple.config.Config;
import com.example.keralatemple.ritual.RitualSchedule;
import com.example.keralatemple.temple.Temple;
import com.example.keralatemple.temple.TempleData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/** The temple priest (melshanti). Follows the temple's ritual schedule around its marker blocks. */
public class PriestEntity extends PathfinderMob {
    private static final int HOME_RADIUS = 24;

    private BlockPos homePos;
    private String lastAnnounced = "";
    private Temple cachedTemple;
    private long lastTempleLookup = -1000;

    public PriestEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    /** True for the keezhshanthi (assistant). */
    public boolean isAssistant() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RitualGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** The spot the priest was first placed; markers are searched for around it. */
    public BlockPos getHome() {
        if (this.homePos == null) {
            this.homePos = this.blockPosition();
            this.restrictTo(this.homePos, HOME_RADIUS);
        }
        return this.homePos;
    }

    /** The registered temple this priest serves (re-checked every few seconds), or null. */
    public Temple getTemple() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        long now = serverLevel.getGameTime();
        if (now - this.lastTempleLookup > 100) {
            this.lastTempleLookup = now;
            this.cachedTemple = TempleData.get(serverLevel).findFor(this.getHome());
        }
        return this.cachedTemple;
    }

    /** All loaded blocks of the given type near the temple centre (or near home when there is no temple). */
    public List<BlockPos> findMarkers(Block block) {
        Temple temple = this.getTemple();
        BlockPos centre = temple != null ? temple.center : this.getHome();
        int r = temple != null ? Math.min(temple.radius, 64) : Config.MARKER_SEARCH_RADIUS.get();
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -6, -r), centre.offset(r, 6, r))) {
            if (this.level().isLoaded(pos) && this.level().getBlockState(pos).is(block)) {
                found.add(pos.immutable());
            }
        }
        return found;
    }

    /** Tells nearby players that a ritual has begun (once per ritual). The keezhshanthi stays quiet if a melshanti is near. */
    public void announce(RitualSchedule.Ritual ritual) {
        if (this.level().isClientSide || ritual.id().equals(this.lastAnnounced)) {
            return;
        }
        if (this.isAssistant()) {
            boolean melshantiNearby = !this.level().getEntitiesOfClass(PriestEntity.class,
                    this.getBoundingBox().inflate(48.0D), e -> !e.isAssistant()).isEmpty();
            if (melshantiNearby) {
                this.lastAnnounced = ritual.id();
                return;
            }
        }
        this.lastAnnounced = ritual.id();
        if (!Config.ANNOUNCE_RITUALS.get()) {
            return;
        }
        Component message = Component.literal(ritual.displayName() + " has begun").withStyle(ChatFormatting.GOLD);
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0D))) {
            player.displayClientMessage(message, true);
        }
    }

    public void resetAnnouncement() {
        this.lastAnnounced = "";
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.homePos != null) {
            tag.putInt("HomeX", this.homePos.getX());
            tag.putInt("HomeY", this.homePos.getY());
            tag.putInt("HomeZ", this.homePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) {
            this.homePos = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            this.restrictTo(this.homePos, HOME_RADIUS);
        }
    }
}
