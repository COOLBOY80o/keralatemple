package com.example.keralatemple.item;

import com.example.keralatemple.idol.OrnamentData;
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

/** A crown, necklace, armlets or waist belt made by the achari. Right-click an idol with it to dress the idol. */
public class OrnamentItem extends Item {

    public OrnamentItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(OrnamentData data) {
        ItemStack stack = new ItemStack(ModItems.ORNAMENT.get());
        stack.getOrCreateTag().put("Ornament", data.save());
        return stack;
    }

    @Nullable
    public static OrnamentData getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Ornament", 10)) {
            return null;
        }
        return OrnamentData.load(tag.getCompound("Ornament"));
    }

    @Override
    public Component getName(ItemStack stack) {
        OrnamentData d = getData(stack);
        return d == null ? super.getName(stack) : Component.literal(d.displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        OrnamentData d = getData(stack);
        if (d == null) {
            return;
        }
        lines.add(Component.literal(d.type.plain).withStyle(ChatFormatting.GOLD));
        for (int j = 1; j < OrnamentData.JEWEL_NAMES.length; j++) {
            int n = d.jewelCount(j);
            if (n > 0) {
                lines.add(Component.literal(n + " x " + OrnamentData.JEWEL_NAMES[j]).withStyle(ChatFormatting.AQUA));
            }
        }
        lines.add(Component.literal("Right-click an idol to dress it").withStyle(ChatFormatting.GRAY));
    }
}
