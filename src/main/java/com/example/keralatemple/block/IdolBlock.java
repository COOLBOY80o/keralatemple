package com.example.keralatemple.block;

import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.idol.IdolMaterial;
import com.example.keralatemple.item.CastIdolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

import javax.annotation.Nullable;

/**
 * One block class for the three places an idol can stand:
 *  - Anjana Shila and Krishna Shila: stone you carve with the chisel (the idol is carved "in" that stone);
 *  - Idol Pedestal (peetham): where a cast metal idol from the achari is placed.
 * The block entity holds the idol and the client draws it standing on top of the block.
 */
public class IdolBlock extends BaseEntityBlock {

    public enum Kind {
        ANJANA(IdolMaterial.ANJANA_SHILA, 1.0F),
        KRISHNA(IdolMaterial.KRISHNA_SHILA, 1.0F),
        PEETHAM(null, 0.25F);

        /** The stone's material, or null for the pedestal. */
        public final IdolMaterial material;
        /** How high the top of the block is, in blocks: the idol's stand starts here. */
        public final float baseTop;

        Kind(IdolMaterial material, float baseTop) {
            this.material = material;
            this.baseTop = baseTop;
        }
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape PEETHAM_SHAPE = Block.box(2, 0, 2, 14, 4, 14);

    private final Kind kind;

    public IdolBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The idol faces the player who placed the block. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.kind == Kind.PEETHAM ? PEETHAM_SHAPE : Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IdolBlockEntity(pos, state);
    }

    /** Pedestal only: right-click with a cast idol to place it; sneak + empty hand takes it back. */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (this.kind != Kind.PEETHAM) {
            return InteractionResult.PASS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IdolBlockEntity idol)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        if (held.getItem() instanceof CastIdolItem) {
            IdolData data = CastIdolItem.getData(held);
            if (data == null) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            if (idol.getData() != null) {
                player.displayClientMessage(Component.literal("This pedestal already holds an idol. "
                        + "Sneak and right-click with an empty hand to take it."), true);
                return InteractionResult.CONSUME;
            }
            idol.setData(data);
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
            return InteractionResult.CONSUME;
        }

        if (held.isEmpty() && player.isShiftKeyDown() && idol.getData() != null) {
            if (!level.isClientSide) {
                ItemStack out = CastIdolItem.create(idol.getData());
                idol.setData(null);
                if (!player.getInventory().add(out)) {
                    player.drop(out, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** A pedestal that is broken gives its idol back as an item. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && this.kind == Kind.PEETHAM) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IdolBlockEntity idol && idol.getData() != null) {
                Block.popResource(level, pos, CastIdolItem.create(idol.getData()));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
