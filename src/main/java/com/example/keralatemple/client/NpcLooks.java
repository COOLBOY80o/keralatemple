package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.entity.PriestEntity;
import com.example.keralatemple.ritual.Vastram;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Gives every priest his own face: hair colour, moustache/beard and forehead mark are painted onto the base skin
 * the first time a combination is needed, then remembered.
 */
@OnlyIn(Dist.CLIENT)
public final class NpcLooks {
    private static final int[] HAIR = {0, 0x4A3020, 0x6B4423, 0x8C8C8C, 0xDCDCDC};
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private NpcLooks() {}

    public static ResourceLocation get(String skin, int look, int vastram, ResourceLocation base) {
        String key = skin + "/" + look + "/" + vastram;
        ResourceLocation found = CACHE.get(key);
        if (found == null) {
            found = build(skin, look, vastram, base, key);
            CACHE.put(key, found);
        }
        return found;
    }

    private static ResourceLocation build(String skin, int look, int vastram, ResourceLocation base, String key) {
        int hair = look % PriestEntity.HAIRS;
        int facial = (look / PriestEntity.HAIRS) % PriestEntity.FACIALS;
        int tilak = (look / (PriestEntity.HAIRS * PriestEntity.FACIALS)) % PriestEntity.TILAKS;
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(base).orElseThrow().open()) {
            NativeImage img = NativeImage.read(in);
            paint(img, hair, facial, tilak);
            if (vastram > 0) {
                sash(img, vastram);
            }
            ResourceLocation id = new ResourceLocation(KeralaTempleMod.MOD_ID,
                    "look/" + skin + "_" + look + "_" + vastram);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
            return id;
        } catch (Exception e) {
            return base;
        }
    }

    /** The angavastram: a rope of cloth from the right hip, across the chest, over the left shoulder and round the back. */
    private static void sash(NativeImage img, int packed) {
        int base = Vastram.RGB[Vastram.base(packed)];
        int accent = Vastram.RGB[Vastram.accent(packed)];
        int design = Vastram.design(packed);
        for (int y = 20; y <= 31; y++) {
            int front = 20 + Math.round((31 - y) * 7.0F / 11.0F);
            int back = 32 + Math.round((y - 20) * 7.0F / 11.0F);
            for (int k = 0; k <= 1; k++) {
                sashPixel(img, front + k, y, k, base, accent, design, 20, 27);
                sashPixel(img, back + k, y, k, base, accent, design, 32, 39);
            }
        }
        // Where it turns the corners: over the left shoulder and under the right hip.
        for (int y = 20; y <= 22; y++) {
            for (int x = 28; x <= 31; x++) {
                sashPixel(img, x, y, y == 21 ? 0 : 1, base, accent, design, 28, 31);
            }
        }
        for (int y = 29; y <= 31; y++) {
            for (int x = 16; x <= 19; x++) {
                sashPixel(img, x, y, y == 30 ? 0 : 1, base, accent, design, 16, 19);
            }
        }
    }

    private static void sashPixel(NativeImage img, int x, int y, int k, int base, int accent, int design, int lo, int hi) {
        if (x < lo || x > hi) {
            return;
        }
        int c = base;
        switch (design) {
            case 1 -> c = k == 0 ? base : accent;
            case 2 -> c = (y % 4 < 2) ? base : accent;
            case 3 -> c = ((x + y) % 2 == 0 && k == 0) ? accent : base;
            default -> { }
        }
        set(img, x, y, scale(c, k == 0 ? 1.0F : 0.92F));
    }

    private static void sashPixel(NativeImage img, int x, int y, int k, int base, int accent, int design) {
        sashPixel(img, x, y, k, base, accent, design, 0, 63);
    }

    // NativeImage stores ABGR; these helpers work in plain 0xRRGGBB.
    private static int get(NativeImage img, int x, int y) {
        int p = img.getPixelRGBA(x, y);
        return ((p & 255) << 16) | (p & 0xFF00) | ((p >> 16) & 255);
    }

    private static void set(NativeImage img, int x, int y, int rgb) {
        img.setPixelRGBA(x, y, 0xFF000000 | ((rgb & 255) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 255));
    }

    private static int scale(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    /** Bands round the upper arms (only on bare skin). A dot colour of -1 means no dot. */
    private static void armBands(NativeImage img, int[] rightRows, int[] leftRows, int colour, int dot) {
        int skin = get(img, 9, 9);
        for (int r = 0; r < rightRows.length; r++) {
            for (int x = 40; x <= 55; x++) {
                bandPixel(img, x, rightRows[r], skin, colour);
            }
            for (int x = 32; x <= 47; x++) {
                bandPixel(img, x, leftRows[r], skin, colour);
            }
        }
        if (dot >= 0) {
            set(img, 45, rightRows[1], dot);
            set(img, 46, rightRows[1], dot);
            set(img, 37, leftRows[1], dot);
            set(img, 38, leftRows[1], dot);
        }
    }

    private static void bandPixel(NativeImage img, int x, int y, int skin, int colour) {
        int c = get(img, x, y);
        int dr = ((c >> 16) & 255) - ((skin >> 16) & 255);
        int dg = ((c >> 8) & 255) - ((skin >> 8) & 255);
        int db = (c & 255) - (skin & 255);
        if (dr * dr + dg * dg + db * db < 70 * 70) {
            set(img, x, y, colour);
        }
    }

    private static void paint(NativeImage img, int hair, int facial, int tilak) {
        int hairRgb = HAIR[hair];
        // 1. Recolour the dark hair on the head (everything dark except the eyes/brows on the front of the face).
        if (hair != 0) {
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 32; x++) {
                    if (x >= 8 && x < 16 && y >= 9) {
                        continue;
                    }
                    int c = get(img, x, y);
                    int max = Math.max((c >> 16) & 255, Math.max((c >> 8) & 255, c & 255));
                    if (max < 0x34 && (img.getPixelRGBA(x, y) >>> 24) > 0) {
                        set(img, x, y, scale(hairRgb, 0.55F + max / 28.0F * 0.45F));
                    }
                }
            }
        }
        // 2. Forehead: wipe the painted mark, then draw the chosen one on the two rows above the eyes.
        int skinTone = get(img, 9, 9);
        for (int y = 9; y <= 10; y++) {
            for (int x = 9; x <= 14; x++) {
                set(img, x, y, skinTone);
            }
        }
        int ash = 0xE8E4DC;
        int red = 0xB01818;
        int sandal = 0xEEC46E;
        int dot = tilak == 2 ? red : sandal;
        if (tilak == 0) {
            // One line of sandal paste across the forehead, and one band round each arm.
            for (int x = 9; x <= 14; x++) {
                set(img, x, 10, sandal);
            }
            armBands(img, new int[]{23}, new int[]{55}, sandal, -1);
        } else {
            // Three lines of bhasma with a sandal (or kumkum) dot between the eyebrows, and the same on the arms.
            for (int x = 10; x <= 13; x++) {
                set(img, x, 8, ash);
                set(img, x, 10, ash);
            }
            set(img, 11, 9, dot);
            set(img, 12, 9, dot);
            armBands(img, new int[]{21, 23, 25}, new int[]{53, 55, 57}, ash, dot);
        }
        // 3. Moustache, beard, full beard.
        if (facial > 0) {
            int beard = hair == 0 ? 0x1B1614 : hairRgb;
            for (int x = 10; x <= 13; x++) {
                set(img, x, 13, scale(beard, (x + 13) % 2 == 0 ? 1.0F : 0.9F));
            }
            if (facial == 2) {
                // a short beard: the chin and the corners of the mouth
                for (int x = 10; x <= 13; x++) {
                    set(img, x, 15, scale(beard, (x + 15) % 2 == 0 ? 1.0F : 0.9F));
                }
                set(img, 10, 14, beard);
                set(img, 13, 14, beard);
            }
            if (facial >= 3) {
                for (int y = 14; y <= 15; y++) {
                    for (int x = 9; x <= 14; x++) {
                        if (y == 14 && (x == 11 || x == 12)) {
                            continue;           // the mouth stays visible
                        }
                        set(img, x, y, scale(beard, (x + y) % 2 == 0 ? 1.0F : 0.9F));
                    }
                }
                for (int y = 11; y <= 13; y++) {
                    set(img, 8, y, beard);
                    set(img, 15, y, beard);
                }
                set(img, 9, 13, beard);
                set(img, 14, 13, beard);
            }
        }
    }
}
