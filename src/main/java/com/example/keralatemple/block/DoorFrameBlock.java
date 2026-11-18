package com.example.keralatemple.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A block for building the frame around a temple door. Its material and carving style can be changed in game
 * with the Chisel (right-click: next material, sneak + right-click: next style). A new frame copies the look of
 * the frame it is placed against, so a whole doorway can be made to match.
 */
public class DoorFrameBlock extends Block {

    public enum Mat implements StringRepresentable {
        WOOD("wood"), GOLD_PLATED("gold_plated"), GOLD("gold"), GRANITE("granite"), LATERITE("laterite");

        private final String id;

        Mat(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public String label() {
            return switch (this) {
                case WOOD -> "wood";
                case GOLD_PLATED -> "gold-plated wood";
                case GOLD -> "gold";
                case GRANITE -> "granite";
                case LATERITE -> "laterite";
            };
        }
    }

    public enum Style implements StringRepresentable {
        PLAIN("plain"), CARVED("carved"), ORNATE("ornate");

        private final String id;

        Style(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    public static final EnumProperty<Mat> MATERIAL = EnumProperty.create("material", Mat.class);
    public static final EnumProperty<Style> STYLE = EnumProperty.create("style", Style.class);

    public DoorFrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MATERIAL, Mat.WOOD).setValue(STYLE, Style.CARVED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MATERIAL, STYLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos against = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState other = level.getBlockState(against);
        if (other.getBlock() instanceof DoorFrameBlock) {
            return this.defaultBlockState().setValue(MATERIAL, other.getValue(MATERIAL)).setValue(STYLE, other.getValue(STYLE));
        }
        return this.defaultBlockState();
    }

    public static Mat next(Mat m) {
        Mat[] all = Mat.values();
        return all[(m.ordinal() + 1) % all.length];
    }

    public static Style next(Style s) {
        Style[] all = Style.values();
        return all[(s.ordinal() + 1) % all.length];
    }
}
