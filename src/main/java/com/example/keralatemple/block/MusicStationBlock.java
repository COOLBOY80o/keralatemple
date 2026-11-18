package com.example.keralatemple.block;

import com.example.keralatemple.entity.MusicianEntity;
import com.example.keralatemple.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The Music Station: where the temple's musicians stand during poojas. Right-click to add one of the chosen
 * instrument (0 to 4); sneak + right-click to choose the next instrument (chenda, thimila, chengila).
 * The station keeps that many musicians of each kind standing here and takes the extras away.
 */
public class MusicStationBlock extends Block {
    public static final IntegerProperty SEL = IntegerProperty.create("sel", 0, 2);
    public static final IntegerProperty CHENDA = IntegerProperty.create("chenda", 0, 4);
    public static final IntegerProperty THIMILA = IntegerProperty.create("thimila", 0, 4);
    public static final IntegerProperty CHENGILA = IntegerProperty.create("chengila", 0, 4);
    private static final String[] NAMES = {"Chenda", "Thimila", "Chengila"};
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public MusicStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SEL, 0).setValue(CHENDA, 1)
                .setValue(THIMILA, 0).setValue(CHENGILA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEL, CHENDA, THIMILA, CHENGILA);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static String tagFor(BlockPos pos) {
        return "ktstation:" + pos.asLong();
    }

    private static IntegerProperty propertyFor(int sel) {
        return sel == 0 ? CHENDA : sel == 1 ? THIMILA : CHENGILA;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                state = state.setValue(SEL, (state.getValue(SEL) + 1) % 3);
            } else {
                IntegerProperty p = propertyFor(state.getValue(SEL));
                state = state.setValue(p, (state.getValue(p) + 1) % 5);
            }
            level.setBlock(pos, state, 3);
            player.displayClientMessage(Component.literal("Chenda " + state.getValue(CHENDA)
                    + "  Thimila " + state.getValue(THIMILA) + "  Chengila " + state.getValue(CHENGILA)
                    + "   |  editing: " + NAMES[state.getValue(SEL)] + " (click = add, sneak-click = next instrument)")
                    .withStyle(ChatFormatting.GOLD), true);
            level.scheduleTick(pos, this, 20);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel sl) {
            for (MusicianEntity m : musicians(sl, pos)) {
                m.discard();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    private static List<MusicianEntity> musicians(ServerLevel level, BlockPos pos) {
        String tag = tagFor(pos);
        return level.getEntitiesOfClass(MusicianEntity.class, new AABB(pos).inflate(64.0D), e -> e.getTags().contains(tag));
    }

    /** Every few seconds: add the musicians that are missing and send away the extra ones. */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        List<MusicianEntity> have = musicians(level, pos);
        spawnOrTrim(level, pos, have, MusicianEntity.Instrument.CHENDA, state.getValue(CHENDA), ModEntities.CHENDA.get());
        spawnOrTrim(level, pos, have, MusicianEntity.Instrument.THIMILA, state.getValue(THIMILA), ModEntities.THIMILA.get());
        spawnOrTrim(level, pos, have, MusicianEntity.Instrument.CHENGILA, state.getValue(CHENGILA), ModEntities.CHENGILA.get());
        level.scheduleTick(pos, this, 100);
    }

    private void spawnOrTrim(ServerLevel level, BlockPos pos, List<MusicianEntity> have,
                             MusicianEntity.Instrument ins, int want, EntityType<? extends MusicianEntity> type) {
        List<MusicianEntity> mine = have.stream().filter(m -> m.instrument() == ins).toList();
        for (int i = want; i < mine.size(); i++) {
            mine.get(i).discard();
        }
        for (int i = mine.size(); i < want; i++) {
            MusicianEntity m = type.create(level);
            if (m == null) {
                return;
            }
            double x = pos.getX() + 0.5D + (level.random.nextInt(5) - 2);
            double z = pos.getZ() + 0.5D + level.random.nextInt(3);
            m.moveTo(x, pos.getY(), z, level.random.nextFloat() * 360.0F, 0.0F);
            m.addTag(tagFor(pos));
            level.addFreshEntity(m);
        }
    }
}
