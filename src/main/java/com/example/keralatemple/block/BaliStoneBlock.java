package com.example.keralatemple.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.util.StringRepresentable;

/**
 * A bali stone (balikkal): black stone base, tier and drum. It can be covered with a removable gold or silver plating:
 * right-click with a gold ingot (gold) or iron ingot (silver); sneak + empty hand takes the plating off and gives the
 * ingot back. Register it for a temple with the Temple Wand so the seeveli stops here.
 */
public class BaliStoneBlock extends BaseEntityBlock {
    public enum Plating implements StringRepresentable {
        NONE("none"), GOLD("gold"), SILVER("silver");

        private final String id;

        Plating(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public static final EnumProperty<Plating> PLATING = EnumProperty.create("plating", Plating.class);

    public BaliStoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PLATING, Plating.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLATING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new com.example.keralatemple.blockentity.BaliStoneBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof com.example.keralatemple.blockentity.BaliStoneBlockEntity be) {
            double half = Math.min(8.0D, be.width() * 8.0D);
            return Block.box(8.0D - half, 0.0D, 8.0D - half, 8.0D + half, Math.min(16.0D, be.height() * 16.0D), 8.0D + half);
        }
        return Block.box(4.8, 0, 4.8, 11.2, 5.4, 11.2);
    }

    private static ItemStack ingotFor(Plating p) {
        return new ItemStack(p == Plating.GOLD ? Items.GOLD_INGOT : Items.IRON_INGOT);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        Plating current = state.getValue(PLATING);
        if (player.isShiftKeyDown() && held.isEmpty() && current != Plating.NONE) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(PLATING, Plating.NONE), 3);
                Block.popResource(level, pos.above(), ingotFor(current));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        Plating wanted = held.is(Items.GOLD_INGOT) ? Plating.GOLD : held.is(Items.IRON_INGOT) ? Plating.SILVER : null;
        if (wanted != null && current == Plating.NONE) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(PLATING, wanted), 3);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && !player.isShiftKeyDown()) {
            // Right-click with an empty hand: change the size and shape.
            if (level.isClientSide) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> com.example.keralatemple.client.ClientHooks.openBaliScreen(pos));
            } else {
                level.getBlockEntity(pos);   // makes sure older stones get their settings
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Plating p = state.getValue(PLATING);
        if (!level.isClientSide && p != Plating.NONE && !player.isCreative()) {
            Block.popResource(level, pos, ingotFor(p));
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
