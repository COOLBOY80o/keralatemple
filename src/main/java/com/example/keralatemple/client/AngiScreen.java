package com.example.keralatemple.client;

import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.idol.AchariCosts;
import com.example.keralatemple.idol.IdolMaterial;
import com.example.keralatemple.item.ChiselItem;
import com.example.keralatemple.network.ModNetwork;
import com.example.keralatemple.network.SubmitOrderPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/** Order a metal angi (thiruvabharanam) cast from the idol your chisel has measured. */
@OnlyIn(Dist.CLIENT)
public class AngiScreen extends Screen {
    private final int entityId;
    private int metalIndex = 3;   // gold
    private Button metalBtn;

    public AngiScreen(int entityId) {
        super(Component.literal("Angi"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 50;
        metalBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            metalIndex = (metalIndex + 1) % IdolMaterial.METALS.size();
            refresh();
        }).bounds(cx - 100, y, 200, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Order"), b -> {
            ModNetwork.CHANNEL.sendToServer(SubmitOrderPacket.angi(entityId, IdolMaterial.METALS.get(metalIndex)));
            onClose();
        }).bounds(cx - 100, y + 70, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),
                b -> this.minecraft.setScreen(new AchariMenuScreen(entityId))).bounds(cx + 2, y + 70, 98, 20).build());
        refresh();
    }

    private void refresh() {
        metalBtn.setMessage(Component.literal("Metal: " + IdolMaterial.METALS.get(metalIndex).label));
    }

    @Nullable
    private IdolBlockEntity measuredIdol() {
        Player p = this.minecraft.player;
        if (p == null || this.minecraft.level == null) {
            return null;
        }
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            BlockPos pos = ChiselItem.getMould(s, this.minecraft.level);
            if (pos != null && this.minecraft.level.getBlockEntity(pos) instanceof IdolBlockEntity idol && idol.getData() != null) {
                return idol;
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int y = this.height / 2 - 50;
        g.drawCenteredString(this.font, "Angi: a metal cover cast to fit an idol", cx, y - 24, 0xFFE0B0);
        IdolBlockEntity idol = measuredIdol();
        if (idol == null) {
            g.drawCenteredString(this.font, "Hold your chisel, after right-clicking an idol with it,", cx, y + 28, 0xFF8888);
            g.drawCenteredString(this.font, "then talk to the achari again.", cx, y + 40, 0xFF8888);
        } else {
            g.drawCenteredString(this.font, "Needs: " + AchariCosts.describe(
                    AchariCosts.forAngi(IdolMaterial.METALS.get(metalIndex), idol.getData().size)), cx, y + 30, 0xFFFFFF);
            g.drawCenteredString(this.font, "Right-click the idol with the finished angi to fit it.", cx, y + 44, 0x999999);
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
