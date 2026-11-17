package com.example.keralatemple.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A small bali stone (balikkal). Register it for a temple with the Temple Wand so the seeveli stops here. */
public class BaliStoneBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);

    public BaliStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
