package com.example.keralatemple.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * One block of a temple door set. A set fills a door frame whose opening is an odd number of blocks wide (3, 5 or 7):
 * two leaves meet exactly in the middle column, in line with the idol. Opening slides the leaves into the sides.
 */
public class TempleLeafBlock extends Block {
    public enum Role implements StringRepresentable {
        PANEL("panel"), CENTRE("centre"), EDGE_L("edge_l"), EDGE_R("edge_r");

        private final String id;

        Role(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public enum Row implements StringRepresentable {
        BOTTOM("bottom"), MID("mid"), TOP("top");

        private final String id;

        Row(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    /** The horizontal direction the door spans along. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<Role> ROLE = EnumProperty.create("role", Role.class);
    public static final EnumProperty<Row> ROW = EnumProperty.create("row", Row.class);

    private static final VoxelShape CLOSED_X = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape CLOSED_Z = Block.box(6, 0, 0, 10, 16, 16);
    private static final VoxelShape STACK_L_X = Block.box(0, 0, 6, 3, 16, 10);
    private static final VoxelShape STACK_R_X = Block.box(13, 0, 6, 16, 16, 10);
    private static final VoxelShape STACK_L_Z = Block.box(6, 0, 0, 10, 16, 3);
    private static final VoxelShape STACK_R_Z = Block.box(6, 0, 13, 10, 16, 16);

    private final Supplier<Item> dropItem;

    public TempleLeafBlock(Properties properties, Supplier<Item> dropItem) {
        super(properties);
        this.dropItem = dropItem;
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(OPEN, false)
                .setValue(ROLE, Role.PANEL).setValue(ROW, Row.MID));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, OPEN, ROLE, ROW);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean x = state.getValue(AXIS) == Direction.Axis.X;
        if (!state.getValue(OPEN)) {
            return x ? CLOSED_X : CLOSED_Z;
        }
        return switch (state.getValue(ROLE)) {
            case EDGE_L -> x ? STACK_L_X : STACK_L_Z;
            case EDGE_R -> x ? STACK_R_X : STACK_R_Z;
            default -> Shapes.empty();
        };
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return type == PathComputationType.LAND && state.getValue(OPEN);
    }

    /** All leaf blocks of this door set (joined side by side and above each other). */
    public static Set<BlockPos> group(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockState first = level.getBlockState(start);
        if (!(first.getBlock() instanceof TempleLeafBlock)) {
            return seen;
        }
        Direction.Axis axis = first.getValue(AXIS);
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && seen.size() < 80) {
            BlockPos p = queue.poll();
            for (Direction d : new Direction[]{Direction.UP, Direction.DOWN,
                    Direction.get(Direction.AxisDirection.POSITIVE, axis), Direction.get(Direction.AxisDirection.NEGATIVE, axis)}) {
                BlockPos q = p.relative(d);
                BlockState s = level.getBlockState(q);
                if (!seen.contains(q) && s.getBlock() == first.getBlock() && s.getValue(AXIS) == axis) {
                    seen.add(q);
                    queue.add(q);
                }
            }
        }
        return seen;
    }

    public static void setGroupOpen(Level level, BlockPos start, boolean open) {
        for (BlockPos p : group(level, start)) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof TempleLeafBlock) {
                level.setBlock(p, s.setValue(OPEN, open), 3);
            }
        }
        level.playSound(null, start, open ? SoundEvents.WOODEN_DOOR_OPEN : SoundEvents.WOODEN_DOOR_CLOSE,
                SoundSource.BLOCKS, 1.0F, 0.9F);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            setGroupOpen(level, pos, !state.getValue(OPEN));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Breaking one block takes the whole door set down and gives back one door set. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            Set<BlockPos> all = group(level, pos);
            for (BlockPos p : all) {
                if (!p.equals(pos)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
            }
            if (!player.isCreative()) {
                Block.popResource(level, pos, new ItemStack(dropItem.get()));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
