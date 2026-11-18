package com.example.keralatemple.entity;

import com.example.keralatemple.config.Config;
import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.item.CastIdolItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

/**
 * The achari: a metal-casting craftsman. Left-click him to design (or import) an idol and pick a metal.
 * He then casts it (it takes a little while) and hands you a finished idol to set on an Idol Pedestal.
 */
public class AchariEntity extends PathfinderMob {
    /** What he is making: the finished item, and a name for messages. */
    @Nullable
    private ItemStack order;
    private String orderLabel = "";
    @Nullable
    private UUID customer;
    private int castTicks;
    private int castTotal;

    public AchariEntity(EntityType<? extends AchariEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // He stays at his work while casting.
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.4D) {
            @Override
            public boolean canUse() {
                return AchariEntity.this.order == null && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Players cannot hurt him (a left-click opens his menu instead); only /kill and the void can. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(source, amount);
    }

    public boolean isBusy() {
        return this.order != null;
    }

    /** How long a job takes: the config time for a 2-block idol, times a factor. */
    public static int ticksFor(float factor) {
        return Math.max(40, (int) (Config.ACHARI_CAST_SECONDS.get() * 20 * factor));
    }

    /** An idol order: longer for taller idols. */
    public boolean takeOrder(ServerPlayer player, IdolData data) {
        return takeOrder(player, CastIdolItem.create(data), ticksFor(data.size / 2.0F),
                String.format(Locale.ROOT, "%.1f block %s idol", data.size, data.material.label));
    }

    /** Takes any order from a player. Returns false if he is already busy. */
    public boolean takeOrder(ServerPlayer player, ItemStack product, int ticks, String label) {
        if (this.order != null) {
            int percent = this.castTotal <= 0 ? 0 : 100 - (this.castTicks * 100 / this.castTotal);
            player.sendSystemMessage(Component.literal("The achari is busy with another order (" + percent + "% done)."));
            return false;
        }
        this.castTotal = ticks;
        this.castTicks = ticks;
        this.order = product;
        this.orderLabel = label;
        this.customer = player.getUUID();
        player.sendSystemMessage(Component.literal("The achari takes your order: " + label
                + ". It will be ready in about " + (ticks / 20) + " seconds."));
        this.playSound(SoundEvents.ANVIL_USE, 1.0F, 0.8F);
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.order == null || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.getNavigation().stop();
        this.castTicks--;

        if (this.castTicks % 20 == 0) {
            this.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.6F, 1.3F);
        }
        if (this.castTicks % 5 == 0) {
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.1D, this.getZ() + 0.6D,
                    4, 0.25D, 0.05D, 0.25D, 0.01D);
            level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.6D, this.getZ() + 0.6D,
                    2, 0.25D, 0.1D, 0.25D, 0.01D);
        }
        if (this.castTicks % 100 == 0 && this.castTicks > 0 && this.customer != null) {
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.customer);
            if (owner != null && owner.distanceToSqr(this) < 24.0D * 24.0D) {
                int percent = 100 - (this.castTicks * 100 / this.castTotal);
                owner.displayClientMessage(Component.literal("Working on your " + this.orderLabel + "... " + percent + "%"), true);
            }
        }
        if (this.castTicks <= 0) {
            finishCasting(level);
        }
    }

    private void finishCasting(ServerLevel level) {
        if (this.order == null) {
            return;
        }
        ItemStack idol = this.order.copy();
        ServerPlayer owner = this.customer == null ? null : level.getServer().getPlayerList().getPlayer(this.customer);

        level.playSound(null, this.blockPosition(), SoundEvents.LAVA_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.8F, 1.2F);
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.8D, this.getZ(), 20, 0.4D, 0.3D, 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.2D, this.getZ(), 12, 0.5D, 0.4D, 0.5D, 0.0D);

        if (owner != null && owner.level() == level && owner.distanceToSqr(this) < 32.0D * 32.0D) {
            if (!owner.getInventory().add(idol)) {
                this.spawnAtLocation(idol);
            }
            owner.sendSystemMessage(Component.literal("Your " + this.orderLabel + " is ready."));
        } else {
            this.spawnAtLocation(idol);   // the customer is away: it waits at his feet
        }
        this.order = null;
        this.orderLabel = "";
        this.customer = null;
        this.castTicks = 0;
        this.castTotal = 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.order != null) {
            tag.put("Product", this.order.save(new CompoundTag()));
            tag.putString("Label", this.orderLabel);
            tag.putInt("CastTicks", this.castTicks);
            tag.putInt("CastTotal", this.castTotal);
            if (this.customer != null) {
                tag.putUUID("Customer", this.customer);
            }
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Product", 10)) {
            this.order = ItemStack.of(tag.getCompound("Product"));
            this.orderLabel = tag.getString("Label");
            this.castTicks = tag.getInt("CastTicks");
            this.castTotal = tag.getInt("CastTotal");
            this.customer = tag.hasUUID("Customer") ? tag.getUUID("Customer") : null;
        }
    }
}
