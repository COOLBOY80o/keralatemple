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
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

    /** Packed look: hair colour (0-4) + 5 * (facial hair (0-3) + 4 * tilak (0-4)). */
    private static final EntityDataAccessor<Integer> LOOK =
            SynchedEntityData.defineId(PriestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VASTRAM =
            SynchedEntityData.defineId(PriestEntity.class, EntityDataSerializers.INT);
    private static final String[] FIRST = {"Krishnan", "Narayanan", "Vishnu", "Madhavan", "Govindan", "Raman", "Sankaran",
            "Damodaran", "Vasudevan", "Parameswaran", "Subrahmanian", "Neelakantan", "Divakaran", "Hariharan"};
    private static final String[] FAMILY = {"Namboothiri", "Potti", "Embrandiri", "Elayathu", "Muthathu", "Nambudiri"};
    private static final EntityDataAccessor<Integer> CARRY =
            SynchedEntityData.defineId(PriestEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<CompoundTag> CARRIED =
            SynchedEntityData.defineId(PriestEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private com.example.keralatemple.idol.IdolData carriedIdol;
    private boolean carriedLoaded;
    public static final int HAIRS = 5;
    public static final int FACIALS = 4;
    public static final int TILAKS = 3;

    public PriestEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (!level.isClientSide) {
            this.entityData.set(LOOK, randomLook());
            this.setCustomName(Component.literal(FIRST[this.random.nextInt(FIRST.length)] + " "
                    + FAMILY[this.random.nextInt(FAMILY.length)]));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(LOOK, 0);
        this.entityData.define(VASTRAM, 0);
        this.entityData.define(CARRY, 0);
        this.entityData.define(CARRIED, new CompoundTag());
    }

    /** 0 not carrying, 1 idol in the arms, 2 idol on the head (on a cushion). */
    public int carryMode() {
        return this.entityData.get(CARRY);
    }

    @javax.annotation.Nullable
    public com.example.keralatemple.idol.IdolData carriedIdol() {
        if (!this.carriedLoaded) {
            CompoundTag tag = this.entityData.get(CARRIED);
            this.carriedIdol = tag.isEmpty() ? null : com.example.keralatemple.idol.IdolData.load(tag);
            this.carriedLoaded = true;
        }
        return this.carriedIdol;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (CARRIED.equals(key)) {
            this.carriedLoaded = false;
        }
    }

    /** During a seeveli the lowest-numbered keezhshanthi near the temple carries the idol, as the temple chose. */
    private void updateCarry() {
        int mode = 0;
        Temple t = getTemple();
        if (t != null && this.isAssistant() && !"none".equals(t.carry)) {
            com.example.keralatemple.ritual.RitualSchedule.Ritual r = com.example.keralatemple.ritual.RitualSchedule.current(
                    com.example.keralatemple.ritual.RitualSchedule.forTemple(t),
                    com.example.keralatemple.ritual.KeralaClock.now().toLocalTime());
            boolean seeveli = r != null && r.kind() == com.example.keralatemple.ritual.RitualSchedule.Kind.SEEVELI;
            boolean lowest = this.level().getEntitiesOfClass(PriestEntity.class, this.getBoundingBox().inflate(48.0D),
                    e -> e.isAssistant() && e.getId() < this.getId()).isEmpty();
            if (seeveli && lowest) {
                mode = "arms".equals(t.carry) ? 1 : 2;
            }
        }
        if (mode != 0 && this.entityData.get(CARRIED).isEmpty()) {
            com.example.keralatemple.idol.IdolData found = findTempleIdol();
            if (found == null) {
                mode = 0;
            } else {
                this.entityData.set(CARRIED, found.save());
            }
        }
        if (mode != getCarryRaw()) {
            this.entityData.set(CARRY, mode);
        }
        if (mode == 0 && !this.entityData.get(CARRIED).isEmpty()) {
            this.entityData.set(CARRIED, new CompoundTag());
        }
    }

    private int getCarryRaw() {
        return this.entityData.get(CARRY);
    }

    /** The idol in the shrine (the angi if it wears one). */
    @javax.annotation.Nullable
    private com.example.keralatemple.idol.IdolData findTempleIdol() {
        BlockPos home = getHome();
        BlockPos best = null;
        com.example.keralatemple.idol.IdolData data = null;
        double bestD = Double.MAX_VALUE;
        for (Block b : new Block[]{com.example.keralatemple.registry.ModBlocks.ANJANA_SHILA.get(),
                com.example.keralatemple.registry.ModBlocks.KRISHNA_SHILA.get(),
                com.example.keralatemple.registry.ModBlocks.IDOL_PEDESTAL.get()}) {
            for (BlockPos p : findMarkers(b)) {
                if (this.level().getBlockEntity(p) instanceof com.example.keralatemple.blockentity.IdolBlockEntity be
                        && be.getData() != null && p.distSqr(home) < bestD) {
                    bestD = p.distSqr(home);
                    best = p;
                    data = be.getAngi() != null ? be.getAngi() : be.getData();
                }
            }
        }
        return best == null ? null : data;
    }

    public int getVastram() {
        return this.entityData.get(VASTRAM);
    }

    /** The role name used for the angavastram setting of the temple. */
    public String roleId() {
        if (this instanceof CookEntity) {
            return "cook";
        }
        if (this instanceof KazhakamEntity) {
            return "kazhakam";
        }
        return this.isAssistant() ? "keezhshanthi" : "melshanthi";
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 20 == 3) {
            updateCarry();
        }
        if (!this.level().isClientSide && this.tickCount % 100 == 7) {
            Temple t = getTemple();
            String role = roleId();
            int v = t != null && t.vastram.containsKey(role)
                    ? com.example.keralatemple.ritual.Vastram.parse(t.vastram.get(role)) : 0;
            if (v == 0) {
                v = com.example.keralatemple.ritual.Vastram.defaultFor(role);
            }
            // The upper cloth is ritual dress: put on a few minutes before a ritual, taken off after it.
            java.time.LocalTime now = com.example.keralatemple.ritual.KeralaClock.now().toLocalTime();
            java.util.List<RitualSchedule.Ritual> all = RitualSchedule.forTemple(t);
            boolean dressed = RitualSchedule.current(all, now) != null;
            if (!dressed) {
                RitualSchedule.Ritual next = RitualSchedule.next(all, now);
                if (next != null) {
                    long until = (next.start().toSecondOfDay() - now.toSecondOfDay() + 86400) % 86400;
                    dressed = until <= 180;
                }
            }
            if (!dressed) {
                v = 0;
            }
            if (v != getVastram()) {
                this.entityData.set(VASTRAM, v);
                this.level().playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, v == 0 ? 0.8F : 1.1F);
            }
        }
    }

    public int getLook() {
        return this.entityData.get(LOOK);
    }

    private int randomLook() {
        int roll = this.random.nextInt(100);
        int hair = roll < 55 ? 0 : roll < 75 ? 1 : roll < 88 ? 2 : roll < 96 ? 3 : 4;
        int facial = this.random.nextInt(FACIALS);
        int tilak = this.random.nextInt(TILAKS);
        return hair + HAIRS * (facial + FACIALS * tilak);
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
        tag.putInt("Look", getLook());
        if (this.homePos != null) {
            tag.putInt("HomeX", this.homePos.getX());
            tag.putInt("HomeY", this.homePos.getY());
            tag.putInt("HomeZ", this.homePos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Look")) {
            this.entityData.set(LOOK, tag.getInt("Look"));
        }
        if (tag.contains("HomeX")) {
            this.homePos = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            this.restrictTo(this.homePos, HOME_RADIUS);
        }
    }
}
