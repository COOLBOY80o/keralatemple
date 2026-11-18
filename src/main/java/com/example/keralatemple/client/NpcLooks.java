package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.entity.PriestEntity;
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

    public static ResourceLocation get(String skin, int look, ResourceLocation base) {
        String key = skin + "/" + look;
        ResourceLocation found = CACHE.get(key);
        if (found == null) {
            found = build(skin, look, base, key);
            CACHE.put(key, found);
        }
        return found;
    }

    private static ResourceLocation build(String skin, int look, ResourceLocation base, String key) {
        int hair = look % PriestEntity.HAIRS;
        int facial = (look / PriestEntity.HAIRS) % PriestEntity.FACIALS;
        int tilak = look / (PriestEntity.HAIRS * PriestEntity.FACIALS);
        try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(base).orElseThrow().open()) {
            NativeImage img = NativeImage.read(in);
            paint(img, hair, facial, tilak);
            ResourceLocation id = new ResourceLocation(KeralaTempleMod.MOD_ID, "look/" + skin + "_" + look);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(img));
            return id;
        } catch (Exception e) {
            return base;
        }
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
        switch (tilak) {
            case 0 -> {                         // sandal-paste mark
                set(img, 11, 9, sandal); set(img, 12, 9, sandal); set(img, 11, 10, sandal); set(img, 12, 10, sandal);
            }
            case 1 -> {                         // red kumkum dot
                set(img, 11, 10, red); set(img, 12, 10, red);
            }
            case 2 -> {                         // three lines of ash (two rows plus a faint third at the hairline)
                for (int x = 9; x <= 14; x++) {
                    set(img, x, 9, ash);
                    set(img, x, 10, ash);
                }
            }
            case 3 -> {                         // Vaishnava "U" mark: two white strokes with a red line between
                set(img, 10, 9, ash); set(img, 10, 10, ash); set(img, 13, 9, ash); set(img, 13, 10, ash);
                set(img, 11, 10, red); set(img, 12, 10, red);
            }
            default -> { }                      // no mark
        }
        // 3. Moustache, beard, full beard.
        if (facial > 0) {
            int beard = hair == 0 ? 0x1B1614 : hairRgb;
            for (int x = 10; x <= 13; x++) {
                set(img, x, 13, scale(beard, (x + 13) % 2 == 0 ? 1.0F : 0.9F));
            }
            if (facial >= 2) {
                for (int y = 14; y <= 15; y++) {
                    for (int x = 9; x <= 14; x++) {
                        if (y == 14 && (x == 11 || x == 12)) {
                            continue;           // the mouth stays visible
                        }
                        set(img, x, y, scale(beard, (x + y) % 2 == 0 ? 1.0F : 0.9F));
                    }
                }
            }
            if (facial >= 3) {
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
