package com.example.keralatemple.client;

import com.example.keralatemple.blockentity.BaliStoneBlockEntity;
import com.example.keralatemple.network.ModNetwork;
import com.example.keralatemple.network.SubmitBaliPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Adjust a bali stone: width, height, number of tiers, space between tiers, round or square. */
@OnlyIn(Dist.CLIENT)
public class BaliStoneScreen extends Screen {
    private final BlockPos pos;
    private float width;
    private float height;
    private int layers;
    private float gap;
    private boolean round;
    private Button shapeButton;

    public BaliStoneScreen(BlockPos pos, BaliStoneBlockEntity be) {
        super(Component.literal("Bali stone"));
        this.pos = pos;
        this.width = be.width();
        this.height = be.height();
        this.layers = be.layers();
        this.gap = be.gap();
        this.round = be.round();
    }

    @Override
    protected void init() {
        int x = this.width() / 2 - 100;
        int y = this.height() / 2 - 80;
        addRenderableWidget(new Slide(x, y, "Width", 0.2F, 3.0F, width, 0.05F, " blocks", v -> width = v));
        addRenderableWidget(new Slide(x, y + 24, "Height", 0.2F, 3.0F, height, 0.05F, " blocks", v -> height = v));
        addRenderableWidget(new Slide(x, y + 48, "Layers", 1.0F, 8.0F, layers, 1.0F, "", v -> layers = Math.round(v)));
        addRenderableWidget(new Slide(x, y + 72, "Space between layers", 0.0F, 0.3F, gap, 0.01F, " blocks", v -> gap = v));
        shapeButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            round = !round;
            refresh();
        }).bounds(x, y + 98, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Small balikkal"), b -> preset(0.4F, 0.35F, 4, 0.0F))
                .bounds(x, y + 122, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Valiya balikkal"), b -> preset(1.6F, 1.4F, 6, 0.0F))
                .bounds(x + 102, y + 122, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            ModNetwork.CHANNEL.sendToServer(new SubmitBaliPacket(pos, width, height, layers, gap, round));
            onClose();
        }).bounds(x, y + 148, 200, 20).build());
        refresh();
    }

    private void preset(float w, float h, int l, float g) {
        this.width = w;
        this.height = h;
        this.layers = l;
        this.gap = g;
        rebuildWidgets();
    }

    private void refresh() {
        shapeButton.setMessage(Component.literal("Shape: " + (round ? "round" : "square")));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        g.drawCenteredString(this.font, "Bali stone: right-click the stone with an empty hand to open this", this.width() / 2,
                this.height() / 2 - 104, 0xFFFFFF);
        super.render(g, mx, my, partial);
    }

    private int width() {
        return super.width;
    }

    private int height() {
        return super.height;
    }

    /** A slider in real units, snapped to a step. */
    private static class Slide extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private final float step;
        private final String suffix;
        private final java.util.function.Consumer<Float> onChange;

        Slide(int x, int y, String label, float min, float max, float value, float step, String suffix,
              java.util.function.Consumer<Float> onChange) {
            super(x, y, 200, 20, Component.empty(), (Math.max(min, Math.min(max, value)) - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.suffix = suffix;
            this.onChange = onChange;
            updateMessage();
        }

        private float real() {
            float v = min + (float) this.value * (max - min);
            return Math.round(v / step) * step;
        }

        @Override
        protected void updateMessage() {
            float r = real();
            setMessage(Component.literal(label + ": " + (step >= 1.0F ? String.valueOf(Math.round(r))
                    : String.format("%.2f", r)) + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(real());
        }
    }
}
