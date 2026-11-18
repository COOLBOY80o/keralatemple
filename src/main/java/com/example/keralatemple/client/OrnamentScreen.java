package com.example.keralatemple.client;

import com.example.keralatemple.idol.AchariCosts;
import com.example.keralatemple.idol.IdolMaterial;
import com.example.keralatemple.idol.OrnamentData;
import com.example.keralatemple.network.ModNetwork;
import com.example.keralatemple.network.SubmitOrderPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Design a gold or silver ornament and choose the jewel for each of its five settings. */
@OnlyIn(Dist.CLIENT)
public class OrnamentScreen extends Screen {
    private final int entityId;
    private OrnamentData.Type type = OrnamentData.Type.CROWN;
    private boolean gold = true;
    private final byte[] jewels = new byte[OrnamentData.SLOTS];
    private Button typeBtn;
    private Button metalBtn;
    private final Button[] jewelBtns = new Button[OrnamentData.SLOTS];

    public OrnamentScreen(int entityId) {
        super(Component.literal("Ornament"));
        this.entityId = entityId;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 80;
        typeBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            type = OrnamentData.Type.byId((type.ordinal() + 1) % OrnamentData.Type.values().length);
            refresh();
        }).bounds(cx - 130, y, 126, 20).build());
        metalBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            gold = !gold;
            refresh();
        }).bounds(cx + 4, y, 126, 20).build());
        for (int i = 0; i < OrnamentData.SLOTS; i++) {
            final int slot = i;
            jewelBtns[i] = addRenderableWidget(Button.builder(Component.empty(), b -> {
                jewels[slot] = (byte) ((jewels[slot] + 1) % OrnamentData.JEWEL_NAMES.length);
                refresh();
            }).bounds(cx - 130 + i * 53, y + 84, 51, 20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Order"), b -> {
            ModNetwork.CHANNEL.sendToServer(SubmitOrderPacket.ornament(entityId, current()));
            onClose();
        }).bounds(cx - 130, y + 150, 126, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"),
                b -> this.minecraft.setScreen(new AchariMenuScreen(entityId))).bounds(cx + 4, y + 150, 126, 20).build());
        refresh();
    }

    private OrnamentData current() {
        return new OrnamentData(type, gold ? IdolMaterial.GOLD : IdolMaterial.SILVER, jewels.clone());
    }

    private void refresh() {
        typeBtn.setMessage(Component.literal(type.label + " (" + type.plain + ")"));
        metalBtn.setMessage(Component.literal("Metal: " + (gold ? "Gold" : "Silver")));
        for (int i = 0; i < OrnamentData.SLOTS; i++) {
            jewelBtns[i].setMessage(Component.literal(OrnamentData.JEWEL_NAMES[jewels[i]].replace("No jewel", "None")));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        int cx = this.width / 2;
        int y = this.height / 2 - 80;
        g.drawCenteredString(this.font, "Jewellery for an idol", cx, y - 14, 0xFFE0B0);

        // A flat sketch: a metal bar with the five settings on it.
        int metal = 0xFF000000 | OrnamentRenderer.metalColour(gold ? IdolMaterial.GOLD : IdolMaterial.SILVER);
        g.fill(cx - 128, y + 36, cx + 128, y + 76, 0xFF222222);
        g.fill(cx - 126, y + 52, cx + 126, y + 66, metal);
        if (type == OrnamentData.Type.CROWN) {
            for (int i = 0; i < OrnamentData.SLOTS; i++) {
                g.fill(cx - 114 + i * 53, y + 40, cx - 92 + i * 53, y + 52, metal);
            }
        }
        for (int i = 0; i < OrnamentData.SLOTS; i++) {
            int px = cx - 130 + i * 53 + 18;
            g.fill(px - 1, y + 49, px + 17, y + 69, 0xFF000000);
            g.fill(px, y + 50, px + 16, y + 68, jewels[i] == 0 ? 0xFF555555 : (0xFF000000 | OrnamentData.JEWEL_COLOURS[jewels[i]]));
        }

        OrnamentData d = current();
        g.drawCenteredString(this.font, "Needs: " + AchariCosts.describe(AchariCosts.forOrnament(d)), cx, y + 112, 0xFFFFFF);
        g.drawCenteredString(this.font, "Silver costs iron ingots, bronze/panchaloha copper, rubies redstone.", cx, y + 126, 0x999999);
        g.drawCenteredString(this.font, "Right-click an idol with the finished piece to dress it.", cx, y + 138, 0x999999);
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
