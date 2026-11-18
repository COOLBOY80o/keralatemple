package com.example.keralatemple.item;

import com.example.keralatemple.blockentity.CarvedBlockEntity;
import com.example.keralatemple.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Carving Chisel: cuts a block into 8 x 8 x 8 small cubes so you can shape drain spouts (pranala) and pipes.
 * Sneak + right-click changes the mode. Right-click an ordinary block to start carving it.
 */
public class CarvingChiselItem extends Item {
    public enum Mode {
        REMOVE("Carve away a small cube"),
        ADD("Add a small cube back"),
        MATERIAL("Change the material"),
        PRANALA("Make a pranala (drain spout), pointing the way you face"),
        CHANNEL("Make an open drain channel, the way you face"),
        PIPE("Make a round pipe, the way you face"),
        RESET("Make the block whole again");

        public final String label;

        Mode(String label) {
            this.label = label;
        }

        Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final String[] MATERIAL_NAMES = {"granite", "laterite", "wood", "gold"};

    public CarvingChiselItem(Properties properties) {
        super(properties);
    }

    public static Mode mode(ItemStack stack) {
        int i = stack.getOrCreateTag().getInt("Mode");
        return Mode.values()[Math.floorMod(i, Mode.values().length)];
    }

    private static void cycle(Player player, ItemStack stack) {
        Mode next = mode(stack).next();
        stack.getOrCreateTag().putInt("Mode", next.ordinal());
        player.displayClientMessage(Component.literal("Carving Chisel: " + next.label).withStyle(ChatFormatting.GOLD), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycle(player, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        Level level = ctx.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                cycle(player, ctx.getItemInHand());
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(level.getBlockEntity(pos) instanceof CarvedBlockEntity)) {
            if (state.hasBlockEntity() || !state.isCollisionShapeFullBlock(level, pos) || state.getDestroySpeed(level, pos) < 0) {
                return InteractionResult.PASS;
            }
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath();
            int material = id.contains("laterite") ? 1
                    : (id.contains("planks") || id.contains("log") || id.contains("wood")) ? 2
                    : id.contains("gold") ? 3 : 0;
            level.setBlock(pos, ModBlocks.CARVED_BLOCK.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof CarvedBlockEntity fresh) {
                fresh.setMaterial(material);
            }
        }
        if (!(level.getBlockEntity(pos) instanceof CarvedBlockEntity be)) {
            return InteractionResult.PASS;
        }

        Direction face = ctx.getClickedFace();
        Vec3 local = ctx.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction facing = player.getDirection();
        Mode mode = mode(ctx.getItemInHand());
        switch (mode) {
            case REMOVE -> {
                int[] v = voxel(local, face, -1);
                if (be.has(v[0], v[1], v[2])) {
                    be.set(v[0], v[1], v[2], false);
                    level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5F, 1.6F);
                    if (be.isEmpty()) {
                        level.removeBlock(pos, false);
                    }
                }
            }
            case ADD -> {
                int[] v = voxel(local, face, 1);
                if (!be.has(v[0], v[1], v[2]) && v[0] >= 0 && v[1] >= 0 && v[2] >= 0
                        && v[0] < CarvedBlockEntity.N && v[1] < CarvedBlockEntity.N && v[2] < CarvedBlockEntity.N) {
                    be.set(v[0], v[1], v[2], true);
                    level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.5F, 1.4F);
                }
            }
            case MATERIAL -> {
                be.setMaterial(be.material() + 1);
                player.displayClientMessage(Component.literal("Material: " + MATERIAL_NAMES[be.material()])
                        .withStyle(ChatFormatting.GOLD), true);
            }
            case PRANALA -> be.applyPreset(0, facing);
            case CHANNEL -> be.applyPreset(1, facing);
            case PIPE -> be.applyPreset(2, facing);
            case RESET -> be.fill();
        }
        return InteractionResult.CONSUME;
    }

    /** The small cube touched on the clicked face; dir -1 = the cube behind the face, +1 = the cube in front of it. */
    private static int[] voxel(Vec3 local, Direction face, int dir) {
        double nudge = 0.01D * dir;
        double x = local.x + face.getStepX() * nudge;
        double y = local.y + face.getStepY() * nudge;
        double z = local.z + face.getStepZ() * nudge;
        return new int[]{(int) Math.floor(x * CarvedBlockEntity.N), (int) Math.floor(y * CarvedBlockEntity.N),
                (int) Math.floor(z * CarvedBlockEntity.N)};
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Mode: " + mode(stack).label).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Sneak + right-click: change mode. Right-click a block to carve it.")
                .withStyle(ChatFormatting.GRAY));
    }
}
