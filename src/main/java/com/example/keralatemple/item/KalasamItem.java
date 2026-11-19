package com.example.keralatemple.item;

import com.example.keralatemple.ritual.Liquid;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The abhishekam kalasam (pot). Right-click an idol to pour what is in it over the idol. Sneak and right-click
 * (not at an idol) to change the liquid: water, milk, yogurt, ghee, honey, kalabham, panchamritam, panchagavyam,
 * tender coconut.
 */
public class KalasamItem extends Item {
    public KalasamItem(Properties properties) {
        super(properties);
    }

    public static Liquid liquid(ItemStack stack) {
        return Liquid.byOrdinal(stack.hasTag() ? stack.getTag().getInt("Liquid") : 0);
    }

    public static ItemStack of(Liquid liquid, Item item) {
        ItemStack stack = new ItemStack(item);
        set(stack, liquid);
        return stack;
    }

    private static void set(ItemStack stack, Liquid liquid) {
        stack.getOrCreateTag().putInt("Liquid", liquid.ordinal());
        stack.getOrCreateTag().putInt("CustomModelData", liquid.ordinal() + 1);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Kalasam of " + liquid(stack).label);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            Liquid[] all = Liquid.values();
            Liquid next = all[(liquid(stack).ordinal() + 1) % all.length];
            if (!level.isClientSide) {
                set(stack, next);
                player.displayClientMessage(Component.literal("Kalasam now holds: " + next.label), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }
}
