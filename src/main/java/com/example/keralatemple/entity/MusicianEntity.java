package com.example.keralatemple.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * A temple musician. Every temple with a ritual going on needs at least one; during the seeveli the musicians
 * walk behind the melshanthi and play the procession beats, slow at first and then faster.
 */
public abstract class MusicianEntity extends PriestEntity {

    public enum Instrument {
        /** Cylindrical drum played with two sticks. */
        CHENDA("X.xxX.x.X.xxXxx.", null, null),
        /** Hourglass drum: the soft, deep timila. */
        THIMILA("x...X...x...Xx..", null, null),
        /** Bronze gong: the chengila marks the count. */
        CHENGILA("X...x...X...x...", null, null);

        public final String pattern;
        final net.minecraft.world.item.Item hand;
        final net.minecraft.world.item.Item other;

        Instrument(String pattern, net.minecraft.world.item.Item hand, net.minecraft.world.item.Item other) {
            this.pattern = pattern;
            this.hand = hand;
            this.other = other;
        }
    }

    protected MusicianEntity(EntityType<? extends PriestEntity> type, Level level, Instrument instrument) {
        super(type, level);
        // the real instrument in one hand and the beating stick in the other
        net.minecraft.world.item.Item drum = switch (instrument) {
            case CHENDA -> com.example.keralatemple.registry.ModItems.CHENDA.get();
            case THIMILA -> com.example.keralatemple.registry.ModItems.THIMILA.get();
            default -> com.example.keralatemple.registry.ModItems.CHENGILA.get();
        };
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(com.example.keralatemple.registry.ModItems.CHENDA_STICK.get()));
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(drum));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    public abstract Instrument instrument();

    private static final String[] MUSICIAN_FIRST = {"Unnikrishnan", "Sasidharan", "Radhakrishnan", "Mohanan", "Sivadasan",
            "Padmanabhan", "Raghavan", "Gopalakrishnan", "Sreekumar", "Prakashan", "Biju", "Anil", "Sunil", "Ramesh",
            "Venugopal", "Harikumar"};
    private static final String[] MUSICIAN_FAMILY = {"Marar", "Poduval", "Pothuval", "Marar"};

    /** Musicians have their own kind of names (Marar, Poduval), not brahmin names. */
    @Override
    protected String generateName() {
        return MUSICIAN_FIRST[this.random.nextInt(MUSICIAN_FIRST.length)] + " "
                + MUSICIAN_FAMILY[this.random.nextInt(MUSICIAN_FAMILY.length)];
    }

    @Override
    public String roleId() {
        return "musician";
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MusicianGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }
}
