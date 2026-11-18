package com.example.keralatemple.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The aduppu: a brick hearth with a fire. Put an Uruli on top. Light it with flint and steel; the cook lights it himself. */
public class AduppuBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public AduppuBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.FLINT_AND_STEEL) && !state.getValue(LIT)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, Boolean.TRUE), 3);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (held.isEmpty() && player.isShiftKeyDown() && state.getValue(LIT)) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(LIT, Boolean.FALSE), 3);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(3) != 0) {
            return;
        }
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        double x = pos.getX() + 0.5D + side.getStepX() * 0.52D + (random.nextDouble() - 0.5D) * 0.3D * Math.abs(side.getStepZ());
        double z = pos.getZ() + 0.5D + side.getStepZ() * 0.52D + (random.nextDouble() - 0.5D) * 0.3D * Math.abs(side.getStepX());
        level.addParticle(ParticleTypes.FLAME, x, pos.getY() + 0.3D + random.nextDouble() * 0.3D, z, 0.0D, 0.01D, 0.0D);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0D, 0.03D, 0.0D);
        }
    }
}
