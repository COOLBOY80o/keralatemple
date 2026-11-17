package com.example.keralatemple.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** The chisel (uli). Left-click an Anjana Shila or Krishna Shila with it to open the idol designer. */
public class ChiselItem extends Item {

    public ChiselItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Left-click an Anjana Shila or Krishna Shila").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("to design and carve an idol.").withStyle(ChatFormatting.GRAY));
    }
}
