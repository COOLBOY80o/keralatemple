package com.example.keralatemple.block;

import com.example.keralatemple.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The uruli: the wide bronze cooking vessel. Set it on an Aduppu (hearth). The cook fills it with water,
 * boils the rice, adds the ingredients and stirs with the spoon. You can pour water in with a bucket,
 * stir with an Uruli Spoon, or sneak with an empty hand to empty it.
 */
public class UruliBlock extends Block {

    public enum Content implements StringRepresentable {
        EMPTY("empty"), WATER("water"), COOKING("cooking"), DONE("done");

        private final String name;

        Content(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Content> CONTENT = EnumProperty.create("content", Content.class);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 10, 15);

    public UruliBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CONTENT, Content.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CONTENT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.WATER_BUCKET) && state.getValue(CONTENT) == Content.EMPTY) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(CONTENT, Content.WATER), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.is(ModItems.URULI_SPOON.get())) {
            if (!level.isClientSide) {
                level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.8F, 0.9F);
            }
            player.swing(hand);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && player.isShiftKeyDown() && state.getValue(CONTENT) != Content.EMPTY) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(CONTENT, Content.EMPTY), 3);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
}
