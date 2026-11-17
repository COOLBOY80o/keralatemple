package com.example.keralatemple.item;

import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/** A finished metal idol from the achari. Right-click it onto an Idol Pedestal (peetham) to set it up. */
public class CastIdolItem extends Item {

    public CastIdolItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(IdolData data) {
        ItemStack stack = new ItemStack(ModItems.CAST_IDOL.get());
        stack.getOrCreateTag().put("Idol", data.save());
        return stack;
    }

    @Nullable
    public static IdolData getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Idol", 10)) {
            return null;
        }
        return IdolData.load(tag.getCompound("Idol"));
    }

    @Override
    public Component getName(ItemStack stack) {
        IdolData data = getData(stack);
        return data == null ? super.getName(stack) : Component.literal(data.material.label + " Idol");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        IdolData data = getData(stack);
        if (data == null) {
            return;
        }
        lines.add(Component.literal(String.format(Locale.ROOT, "%.1f blocks tall", data.size)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Right-click an Idol Pedestal to place it").withStyle(ChatFormatting.GRAY));
    }
}
