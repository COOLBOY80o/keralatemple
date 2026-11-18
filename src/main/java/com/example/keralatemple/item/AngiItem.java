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

/** A metal cover cast from an idol's measurements (an angi, or thiruvabharanam). Right-click the idol to fit it. */
public class AngiItem extends Item {

    public AngiItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(IdolData data) {
        ItemStack stack = new ItemStack(ModItems.ANGI.get());
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
        IdolData d = getData(stack);
        return d == null ? super.getName(stack) : Component.literal(d.material.label + " Angi");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Right-click the idol it was cast for").withStyle(ChatFormatting.GRAY));
    }
}
