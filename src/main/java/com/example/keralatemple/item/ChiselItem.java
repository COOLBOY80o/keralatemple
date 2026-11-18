package com.example.keralatemple.item;

import com.example.keralatemple.blockentity.IdolBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    /** Right-click an idol to take its measurements for an angi. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof IdolBlockEntity idol) || idol.getData() == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockPos pos = context.getClickedPos();
            CompoundTag mould = new CompoundTag();
            mould.putInt("X", pos.getX());
            mould.putInt("Y", pos.getY());
            mould.putInt("Z", pos.getZ());
            mould.putString("Dim", level.dimension().location().toString());
            context.getItemInHand().getOrCreateTag().put("Mould", mould);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Measurements taken. Show this chisel to the achari to order an angi."), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The idol position this chisel has measured, if it is in this level. */
    @Nullable
    public static BlockPos getMould(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("Mould", 10)) {
            return null;
        }
        CompoundTag m = tag.getCompound("Mould");
        if (!m.getString("Dim").equals(level.dimension().location().toString())) {
            return null;
        }
        return new BlockPos(m.getInt("X"), m.getInt("Y"), m.getInt("Z"));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.literal("Left-click an Anjana Shila or Krishna Shila").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("to design and carve an idol.").withStyle(ChatFormatting.GRAY));
        lines.add(Component.literal("Right-click an idol to measure it for an angi.").withStyle(ChatFormatting.GRAY));
        if (stack.getTag() != null && stack.getTag().contains("Mould", 10)) {
            lines.add(Component.literal("Measurements taken").withStyle(ChatFormatting.GOLD));
        }
    }
}
