package com.example.keralatemple.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A shankham (conch) filled with a liquid. It works like the kalasam: right-click an idol, sneak to change the liquid. */
public class ShankhamItem extends KalasamItem {
    public ShankhamItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Shankham of " + liquid(stack).label);
    }
}
