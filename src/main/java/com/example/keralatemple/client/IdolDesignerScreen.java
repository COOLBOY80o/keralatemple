package com.example.keralatemple.client;

import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.idol.IdolMaterial;
import com.example.keralatemple.network.ModNetwork;
import com.example.keralatemple.network.SubmitIdolPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;

/**
 * The idol designer. Opened by left-clicking a shila with the chisel (carve) or by left-clicking the achari (cast).
 * Draw the idol's front view, or drop a picture onto the window; the game inflates it into 3D.
 */
@OnlyIn(Dist.CLIENT)
public class IdolDesignerScreen extends Screen {
    private static final int PANEL_W = 360;
    private static final int PANEL_H = 286;
    private static final int CANVAS = 192;
    private static final int[] PALETTE = {
            0x000000, 0x3B3B3B, 0x7F7F7F, 0xBFBFBF, 0xFFFFFF, 0x5A3A1E, 0x8B5A2B, 0xC8962E,
            0xFFD700, 0xF2E6C8, 0xE8B890, 0xB5651D, 0x8B0000, 0xE02020, 0xFF7F00, 0xFFE000,
            0x1E5A2E, 0x3CB44B, 0x1E90FF, 0x00008B, 0x6A0DAD, 0xFF69B4, 0x00CED1, 0xDEB887};

    private enum Tool {PEN, ERASE, FILL, RESTORE}

    @Nullable
    private final BlockPos blockPos;
    private final int entityId;
    private final boolean achari;

    private int gw = 32;
    private int gh = 48;
    private int[] pixels;   // argb, 0 = empty
    private Tool tool = Tool.PEN;
    private int colour = 0xC8962E;
    private boolean mirror = true;
    private boolean importTab = false;
    private int brush = 1;
    @Nullable
    private int[] original;   // the imported picture with nothing cut away
    private float[] crop = {0.0F, 0.0F, 1.0F, 1.0F};   // the part of the dropped picture in use
    private double dragX0 = -1, dragY0 = -1, dragX1 = -1, dragY1 = -1;
    private boolean autoCrop = true;
    private Button autoBtn;
    private int cutMode = 1;   // 0 keep everything, 1 normal cut-out, 2 generous (keeps more)
    private boolean originalColours = false;
    private float size = 2.0F;
    private float thickness = 1.0F;
    private int peetham = 1;
    private float peethamSize = 1.0F;
    private int peethamStone = 0;
    private Button stoneBtn;
    private int metalIndex = 0;
    private String status = "";
    @Nullable
    private Path lastDropped;

    private Button designTabBtn;
    private Button importTabBtn;
    private Button penBtn;
    private Button eraseBtn;
    private Button fillBtn;
    private Button clearBtn;
    private Button mirrorBtn;
    private Button brushBtn;
    private Button bgBtn;
    private Button peethamBtn;
    private Button colourBtn;
    private Button metalBtn;

    public IdolDesignerScreen(@Nullable BlockPos pos, int entityId, @Nullable IdolData existing) {
        super(Component.literal("Idol designer"));
        this.blockPos = pos;
        this.entityId = entityId;
        this.achari = pos == null;
        this.pixels = new int[gw * gh];
        if (existing != null) {
            gw = existing.width;
            gh = existing.height;
            pixels = new int[gw * gh];
            for (int k = 0; k < pixels.length; k++) {
                int a = existing.rgba[k * 4 + 3] & 255;
                if (a >= 128) {
                    pixels[k] = 0xFF000000 | ((existing.rgba[k * 4] & 255) << 16)
                            | ((existing.rgba[k * 4 + 1] & 255) << 8) | (existing.rgba[k * 4 + 2] & 255);
                }
            }
            size = existing.size;
            thickness = existing.thickness;
            peetham = existing.peetham;
            peethamSize = existing.peethamSize;
            peethamStone = existing.peethamStone;
            originalColours = existing.originalColours;
        }
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int l = left();
        int t = top();
        int rx = l + 208;

        designTabBtn = addRenderableWidget(Button.builder(Component.literal("Design"), b -> setTab(false))
                .bounds(rx, t + 20, 70, 20).build());
        importTabBtn = addRenderableWidget(Button.builder(Component.literal("Import"), b -> setTab(true))
                .bounds(rx + 74, t + 20, 70, 20).build());

        penBtn = addRenderableWidget(Button.builder(Component.literal("Pen"), b -> tool = Tool.PEN)
                .bounds(rx, t + 44, 30, 20).build());
        eraseBtn = addRenderableWidget(Button.builder(Component.literal("Erase"), b -> tool = Tool.ERASE)
                .bounds(rx + 32, t + 44, 36, 20).build());
        fillBtn = addRenderableWidget(Button.builder(Component.literal("Fill"), b -> tool = Tool.FILL)
                .bounds(rx + 70, t + 44, 28, 20).build());
        clearBtn = addRenderableWidget(Button.builder(Component.literal("Restore"), b -> tool = Tool.RESTORE)
                .bounds(rx + 100, t + 44, 44, 20).build());

        mirrorBtn = addRenderableWidget(Button.builder(Component.empty(), b -> mirror = !mirror)
                .bounds(rx, t + 68, 92, 20).build());
        brushBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            brush = brush == 1 ? 3 : brush == 3 ? 5 : 1;
            refreshLabels();
        }).bounds(rx + 94, t + 68, 50, 20).build());
        bgBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            cutMode = (cutMode + 1) % 3;
            if (lastDropped != null) {
                importFile(lastDropped);
            }
        }).bounds(rx, t + 44, 144, 20).build());

        autoBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            autoCrop = !autoCrop;
            crop = autoCrop && lastDropped != null ? ImageImport.autoCrop(lastDropped) : new float[]{0.0F, 0.0F, 1.0F, 1.0F};
            if (lastDropped != null) {
                importFile(lastDropped);
            }
            refreshLabels();
        }).bounds(rx, t + 68, 144, 20).build());

        addRenderableWidget(new SimpleSlider(rx, t + 142, 144, "Height", IdolData.MIN_SIZE, IdolData.MAX_SIZE, size,
                v -> size = v, " blocks"));
        addRenderableWidget(new SimpleSlider(rx, t + 163, 144, "Thickness", IdolData.MIN_THICKNESS,
                IdolData.MAX_THICKNESS, thickness, v -> thickness = v, "x"));

        addRenderableWidget(new SimpleSlider(rx, t + 184, 144, "Peetham size", IdolData.MIN_PEETHAM_SIZE,
                IdolData.MAX_PEETHAM_SIZE, peethamSize, v -> peethamSize = v, "x"));

        peethamBtn = addRenderableWidget(Button.builder(Component.empty(),
                b -> peetham = (peetham + 1) % IdolData.PEETHAM_STYLES).bounds(rx, t + 206, 70, 20).build());
        stoneBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            peethamStone = 1 - peethamStone;
            refreshLabels();
        }).bounds(rx + 74, t + 206, 70, 20).build());
        colourBtn = addRenderableWidget(Button.builder(Component.empty(), b -> originalColours = !originalColours)
                .bounds(rx + 74, t + 206, 70, 20).build());
        metalBtn = addRenderableWidget(Button.builder(Component.empty(),
                        b -> metalIndex = (metalIndex + 1) % IdolMaterial.METALS.size())
                .bounds(rx, t + 228, 144, 20).build());
        metalBtn.visible = achari;

        addRenderableWidget(Button.builder(Component.literal(achari ? "Cast idol" : "Carve idol"), b -> submit())
                .bounds(rx, t + 258, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose())
                .bounds(rx + 74, t + 258, 70, 20).build());

        refreshLabels();
    }

    private void setTab(boolean imp) {
        importTab = imp;
        refreshLabels();
    }

    private void refreshLabels() {
        designTabBtn.active = importTab;
        importTabBtn.active = !importTab;
        boolean d = !importTab;
        penBtn.visible = d;
        eraseBtn.visible = d;
        fillBtn.visible = d;
        clearBtn.visible = d;
        mirrorBtn.visible = d;
        brushBtn.visible = d;
        brushBtn.setMessage(Component.literal("Brush " + brush));
        bgBtn.visible = !d;
        autoBtn.visible = !d;
        autoBtn.setMessage(Component.literal("Auto-crop: " + (autoCrop ? "on" : "off")));
        mirrorBtn.setMessage(Component.literal("Mirror: " + (mirror ? "on" : "off")));
        bgBtn.setMessage(Component.literal("Cut-out: " + (cutMode == 0 ? "none (whole picture)" : cutMode == 1 ? "normal" : "generous (keeps more)")));
        peethamBtn.setMessage(Component.literal("Peetham: " + peethamName()));
        stoneBtn.setMessage(Component.literal(peethamStone == 0 ? "Anjana" : "Krishna"));
        colourBtn.setMessage(Component.literal(originalColours ? "Own colours" : "Material"));
        metalBtn.setMessage(Component.literal("Metal: " + IdolMaterial.METALS.get(metalIndex).label));
    }

    private String peethamName() {
        return switch (peetham) {
            case 0 -> "none";
            case 1 -> "simple";
            case 2 -> "lotus";
            case 4 -> "spout (stone)";
            default -> "tiered";
        };
    }

    // ------------------------------------------------------------------ importing

    @Override
    public void onFilesDrop(List<Path> files) {
        if (!files.isEmpty()) {
            setTab(true);
            importFile(files.get(0));
        }
    }

    private void importFile(Path file) {
        if (file != lastDropped) {
            crop = autoCrop ? ImageImport.autoCrop(file) : new float[]{0.0F, 0.0F, 1.0F, 1.0F};
        }
        lastDropped = file;
        try {
            ImageImport.Result r = ImageImport.load(file, cutMode, crop[0], crop[1], crop[2], crop[3]);
            gw = r.width();
            gh = r.height();
            pixels = r.argb();
            original = r.original();
            status = "Loaded " + file.getFileName() + " (" + gw + "x" + gh + ")";
        } catch (Exception e) {
            status = "Could not read that file: " + e.getMessage();
        }
    }

    // ------------------------------------------------------------------ drawing

    private int cell() {
        return Math.max(1, Math.min(CANVAS / gw, CANVAS / gh));
    }

    private int canvasX() {
        return left() + 8 + (CANVAS - gw * cell()) / 2;
    }

    private int canvasY() {
        return top() + 20 + (CANVAS - gh * cell()) / 2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        if (!importTab) {
            int rx = left() + 208;
            int py = top() + 92;
            if (mx >= rx && mx < rx + 144 && my >= py && my < py + 48) {
                int idx = (int) ((my - py) / 16) * 8 + (int) ((mx - rx) / 18);
                if (idx >= 0 && idx < PALETTE.length) {
                    colour = PALETTE[idx];
                    tool = tool == Tool.ERASE ? Tool.PEN : tool;
                }
                return true;
            }
            return paintAt(mx, my, button == 1);
        }
        if (lastDropped != null && inCanvas(mx, my)) {
            if (button == 1) {
                crop = new float[]{0.0F, 0.0F, 1.0F, 1.0F};   // right-click: use the whole picture again
                importFile(lastDropped);
            } else {
                dragX0 = dragX1 = mx;
                dragY0 = dragY1 = my;
            }
            return true;
        }
        return false;
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= canvasX() && mx < canvasX() + gw * cell() && my >= canvasY() && my < canvasY() + gh * cell();
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (importTab && dragX0 >= 0 && button == 0) {
            double ax = Math.min(dragX0, dragX1), bx = Math.max(dragX0, dragX1);
            double ay = Math.min(dragY0, dragY1), by = Math.max(dragY0, dragY1);
            dragX0 = -1;
            if (bx - ax >= 8 && by - ay >= 8 && lastDropped != null) {
                float vw = gw * cell(), vh = gh * cell();
                float fx0 = (float) Math.max(0, (ax - canvasX()) / vw), fx1 = (float) Math.min(1, (bx - canvasX()) / vw);
                float fy0 = (float) Math.max(0, (ay - canvasY()) / vh), fy1 = (float) Math.min(1, (by - canvasY()) / vh);
                float cw = crop[2] - crop[0], ch = crop[3] - crop[1];
                crop = new float[]{crop[0] + fx0 * cw, crop[1] + fy0 * ch, crop[0] + fx1 * cw, crop[1] + fy1 * ch};
                importFile(lastDropped);
            }
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (super.mouseDragged(mx, my, button, dx, dy)) {
            return true;
        }
        if (importTab && dragX0 >= 0) {
            dragX1 = mx;
            dragY1 = my;
            return true;
        }
        if (!importTab && tool != Tool.FILL) {
            return paintAt(mx, my, button == 1);
        }
        return false;
    }

    private boolean paintAt(double mx, double my, boolean rightButton) {
        int c = cell();
        int x = (int) Math.floor((mx - canvasX()) / c);
        int y = (int) Math.floor((my - canvasY()) / c);
        if (x < 0 || y < 0 || x >= gw || y >= gh) {
            return false;
        }
        boolean restore = tool == Tool.RESTORE && !rightButton;
        if (restore && original == null) {
            status = "Nothing to restore: import a picture first.";
            return true;
        }
        int value = (tool == Tool.ERASE || rightButton) ? 0 : (0xFF000000 | colour);
        if (tool == Tool.FILL && !rightButton) {
            flood(x, y, value);
        } else {
            int r = brush / 2;
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    stamp(x + dx, y + dy, value, restore);
                    if (mirror) {
                        stamp(gw - 1 - (x + dx), y + dy, value, restore);
                    }
                }
            }
        }
        return true;
    }

    private void stamp(int x, int y, int value, boolean restore) {
        if (x < 0 || y < 0 || x >= gw || y >= gh) {
            return;
        }
        pixels[y * gw + x] = restore && original != null && original.length == pixels.length
                ? original[y * gw + x] : value;
    }

    private void flood(int sx, int sy, int value) {
        int target = pixels[sy * gw + sx];
        if (target == value) {
            return;
        }
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(sy * gw + sx);
        while (!queue.isEmpty()) {
            int k = queue.poll();
            if (pixels[k] != target) {
                continue;
            }
            pixels[k] = value;
            int x = k % gw;
            int y = k / gw;
            if (x > 0) {
                queue.add(k - 1);
            }
            if (x < gw - 1) {
                queue.add(k + 1);
            }
            if (y > 0) {
                queue.add(k - gw);
            }
            if (y < gh - 1) {
                queue.add(k + gw);
            }
        }
    }

    // ------------------------------------------------------------------ submit

    private void submit() {
        byte[] rgba = new byte[gw * gh * 4];
        boolean any = false;
        for (int k = 0; k < pixels.length; k++) {
            int p = pixels[k];
            if ((p >>> 24) >= 128) {
                any = true;
                rgba[k * 4] = (byte) (p >> 16);
                rgba[k * 4 + 1] = (byte) (p >> 8);
                rgba[k * 4 + 2] = (byte) p;
                rgba[k * 4 + 3] = (byte) 255;
            }
        }
        if (!any) {
            status = "Draw something or drop a picture first.";
            return;
        }
        IdolMaterial material = achari ? IdolMaterial.METALS.get(metalIndex) : IdolMaterial.ANJANA_SHILA;
        IdolData data = new IdolData(gw, gh, rgba, size, thickness, peetham, peethamSize, peethamStone, originalColours, material);
        if (achari) {
            ModNetwork.CHANNEL.sendToServer(SubmitIdolPacket.forEntity(entityId, data));
        } else {
            ModNetwork.CHANNEL.sendToServer(SubmitIdolPacket.forBlock(blockPos, data));
        }
        onClose();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        this.renderBackground(g);
        int l = left();
        int t = top();
        g.fill(l, t, l + PANEL_W, t + PANEL_H, 0xE0201810);
        g.fill(l + 1, t + 1, l + PANEL_W - 1, t + PANEL_H - 1, 0xFF3A2E22);
        g.drawString(this.font, achari ? "Achari: cast a metal idol" : "Chisel: carve an idol", l + 8, t + 6, 0xFFE0B0, false);

        // Canvas with a checker backdrop so empty cells are visible.
        int c = cell();
        int cx = canvasX();
        int cy = canvasY();
        g.fill(cx - 1, cy - 1, cx + gw * c + 1, cy + gh * c + 1, 0xFF000000);
        for (int j = 0; j < gh; j++) {
            for (int i = 0; i < gw; i++) {
                int p = pixels[j * gw + i];
                int col = (p >>> 24) >= 128 ? (0xFF000000 | p) : (((i + j) & 1) == 0 ? 0xFF505050 : 0xFF444444);
                g.fill(cx + i * c, cy + j * c, cx + (i + 1) * c, cy + (j + 1) * c, col);
            }
        }
        if (mirror && !importTab) {
            int mxl = cx + gw * c / 2;
            g.fill(mxl, cy, mxl + 1, cy + gh * c, 0x66FFFFFF);
        }

        if (!importTab) {
            int rx = l + 208;
            int py = t + 92;
            for (int i = 0; i < PALETTE.length; i++) {
                int x = rx + (i % 8) * 18;
                int y = py + (i / 8) * 16;
                g.fill(x, y, x + 16, y + 14, PALETTE[i] == colour ? 0xFFFFFFFF : 0xFF000000);
                g.fill(x + 1, y + 1, x + 15, y + 13, 0xFF000000 | PALETTE[i]);
            }
            g.drawString(this.font, "Tool: " + tool.name().toLowerCase() + "  (right-click erases)", l + 8, t + 20 + CANVAS + 16, 0xBBBBBB, false);
        } else {
            g.drawString(this.font, "Drop a PNG/JPG onto this window.", l + 208, t + 94, 0xDDDDDD, false);
            g.drawString(this.font, "Drag a box = zoom in on the idol.", l + 208, t + 106, 0xFFDD88, false);
            g.drawString(this.font, "Right-click = whole picture.", l + 208, t + 118, 0xFFDD88, false);
            g.drawString(this.font, "Then Erase / Restore to tidy.", l + 208, t + 130, 0xBBBBBB, false);
        }

        if (importTab && dragX0 >= 0) {
            int ax = (int) Math.min(dragX0, dragX1), bx = (int) Math.max(dragX0, dragX1);
            int ay = (int) Math.min(dragY0, dragY1), by = (int) Math.max(dragY0, dragY1);
            g.renderOutline(ax, ay, bx - ax, by - ay, 0xFFFFFF00);
        }
        if (!status.isEmpty()) {
            g.drawString(this.font, this.font.plainSubstrByWidth(status, CANVAS + 8), l + 8, t + 20 + CANVAS + 4, 0xFFCC88, false);
        }
        super.render(g, mx, my, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A slider that reports its value in real units. */
    private static class SimpleSlider extends AbstractSliderButton {
        private final String label;
        private final float min;
        private final float max;
        private final String suffix;
        private final java.util.function.Consumer<Float> onChange;

        SimpleSlider(int x, int y, int w, String label, float min, float max, float value,
                     java.util.function.Consumer<Float> onChange, String suffix) {
            super(x, y, w, 20, Component.empty(), (value - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.suffix = suffix;
            this.onChange = onChange;
            updateMessage();
        }

        private float real() {
            return Math.round((min + (float) this.value * (max - min)) * 20.0F) / 20.0F;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(label + ": " + real() + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(real());
        }
    }
}
