package com.example.keralatemple.block;

import com.example.keralatemple.blockentity.WellPulleyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A well pulley: two posts, a beam, a roller, a rope and a bucket. Place it over the well. Right-click to draw water
 * (with an empty bucket you get a bucket of water). The keezhshanthi uses it to draw the water for the ayirakudam.
 */
public class WellPulleyBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape X = Shapes.or(Block.box(1, 0, 6.5, 3, 14, 9.5), Block.box(13, 0, 6.5, 15, 14, 9.5),
            Block.box(0, 13, 6.5, 16, 15, 9.5));
    private static final VoxelShape Z = Shapes.or(Block.box(6.5, 0, 1, 9.5, 14, 3), Block.box(6.5, 0, 13, 9.5, 14, 15),
            Block.box(6.5, 13, 0, 9.5, 15, 16));

    public WellPulleyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? X : Z;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WellPulleyBlockEntity(pos, state);
    }

    /** Lowers and raises the bucket (everyone nearby sees it). */
    public static void draw(Level level, BlockPos pos) {
        level.blockEvent(pos, level.getBlockState(pos).getBlock(), 1, 0);
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7F, 0.9F);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        draw(level, pos);
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BUCKET)) {
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
                ItemStack water = new ItemStack(Items.WATER_BUCKET);
                if (!player.addItem(water)) {
                    player.drop(water, false);
                }
            }
        }
        return InteractionResult.CONSUME;
    }
}
