package com.example.keralatemple.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** What the achari makes: an idol, jewellery, or an angi cast from an idol you have measured. */
@OnlyIn(Dist.CLIENT)
public class AchariMenuScreen extends Screen {
    private final int entityId;

    public AchariMenuScreen(int entityId) {
        super(Component.literal("Achari"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 50;
        addRenderableWidget(Button.builder(Component.literal("Cast a metal idol"),
                b -> this.minecraft.setScreen(new IdolDesignerScreen(null, entityId, null))).bounds(x, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Make jewellery (crown, mala, vala, belt)"),
                b -> this.minecraft.setScreen(new OrnamentScreen(entityId))).bounds(x, y + 24, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cast an angi from a measured idol"),
                b -> this.minecraft.setScreen(new AngiScreen(entityId))).bounds(x, y + 48, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Leave"), b -> onClose())
                .bounds(x, y + 80, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, "What shall the achari make?", this.width / 2, this.height / 2 - 70, 0xFFE0B0);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
