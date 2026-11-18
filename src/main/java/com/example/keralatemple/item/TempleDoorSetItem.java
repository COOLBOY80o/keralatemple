package com.example.keralatemple.item;

import com.example.keralatemple.block.TempleLeafBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * A temple door set. Right-click the floor inside a door frame whose opening is 3, 5 or 7 blocks wide (an odd number,
 * so the meeting line of the two leaves is the middle of the opening) and the whole opening is filled, whatever its height.
 */
public class TempleDoorSetItem extends Item {
    private final Supplier<Block> leaf;

    public TempleDoorSetItem(Supplier<Block> leaf, Properties properties) {
        super(properties);
        this.leaf = leaf;
    }

    private static boolean free(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.canBeReplaced();
    }

    private static void say(Player player, String text) {
        if (player != null) {
            player.displayClientMessage(Component.literal(text), true);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = ctx.getClickedPos();
        BlockPos base = ctx.getClickedFace() == Direction.UP ? clicked.above() : clicked.relative(ctx.getClickedFace());
        if (!free(level, base)) {
            say(player, "Click the floor inside the door frame.");
            return InteractionResult.CONSUME;
        }
        // The door spans across the way you are looking through it.
        Direction look = player.getDirection();
        Direction.Axis span = look.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
        Direction neg = Direction.get(Direction.AxisDirection.NEGATIVE, span);
        Direction pos = Direction.get(Direction.AxisDirection.POSITIVE, span);

        int left = 0;
        while (left < 4 && free(level, base.relative(neg, left + 1))) {
            left++;
        }
        int right = 0;
        while (right < 4 && free(level, base.relative(pos, right + 1))) {
            right++;
        }
        int width = left + right + 1;
        if (width > 7) {
            say(player, "That opening is too wide. A temple door set fits openings of 3, 5 or 7 blocks.");
            return InteractionResult.CONSUME;
        }
        if (width % 2 == 0) {
            say(player, "The opening is " + width + " blocks wide. It must be an odd width (3, 5 or 7) so the idol is in the middle.");
            return InteractionResult.CONSUME;
        }
        int height = 1;
        while (height < 8 && free(level, base.above(height))) {
            height++;
        }
        if (height > 7 || height < 2) {
            say(player, "The opening must be between 2 and 7 blocks high.");
            return InteractionResult.CONSUME;
        }
        BlockPos start = base.relative(neg, left);
        for (int c = 0; c < width; c++) {
            for (int r = 0; r < height; r++) {
                if (!free(level, start.relative(pos, c).above(r))) {
                    say(player, "The opening is not a clean rectangle: something is in the way.");
                    return InteractionResult.CONSUME;
                }
            }
        }
        int mid = (width - 1) / 2;
        for (int c = 0; c < width; c++) {
            for (int r = 0; r < height; r++) {
                TempleLeafBlock.Role role = c == mid ? TempleLeafBlock.Role.CENTRE
                        : c == 0 ? TempleLeafBlock.Role.EDGE_L
                        : c == width - 1 ? TempleLeafBlock.Role.EDGE_R : TempleLeafBlock.Role.PANEL;
                if (width == 1) {
                    role = TempleLeafBlock.Role.CENTRE;
                }
                TempleLeafBlock.Row row = r == 0 ? TempleLeafBlock.Row.BOTTOM
                        : r == height - 1 ? TempleLeafBlock.Row.TOP : TempleLeafBlock.Row.MID;
                level.setBlock(start.relative(pos, c).above(r), leaf.get().defaultBlockState()
                        .setValue(TempleLeafBlock.AXIS, span == Direction.Axis.X ? Direction.Axis.X : Direction.Axis.Z)
                        .setValue(TempleLeafBlock.ROLE, role).setValue(TempleLeafBlock.ROW, row), 3);
            }
        }
        level.playSound(null, base, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (!player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        say(player, "Door set placed: " + width + " wide, " + height + " high. Right-click it to open.");
        return InteractionResult.CONSUME;
    }
}
