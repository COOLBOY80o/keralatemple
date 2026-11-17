package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.idol.IdolMaterial;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds and remembers the 3D model and texture of each distinct idol, so a block entity does not rebuild its
 * idol every frame. Least recently used models are released.
 */
@OnlyIn(Dist.CLIENT)
public final class IdolCache {
    private static final int MAX_ENTRIES = 32;

    public record IdolMesh(float[] data, int vertexCount, ResourceLocation texture) {}

    private static int counter = 0;

    private static final LinkedHashMap<Long, IdolMesh> CACHE = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, IdolMesh> eldest) {
            if (size() > MAX_ENTRIES) {
                Minecraft.getInstance().getTextureManager().release(eldest.getValue().texture());
                return true;
            }
            return false;
        }
    };

    private IdolCache() {}

    public static IdolMesh get(IdolData data) {
        long key = data.contentKey();
        IdolMesh mesh = CACHE.get(key);
        if (mesh == null) {
            mesh = build(data);
            CACHE.put(key, mesh);
        }
        return mesh;
    }

    private static IdolMesh build(IdolData data) {
        int w = data.width;
        int h = data.height;
        int[] argb = colourise(data);

        IdolMeshBuilder.MeshData mesh = IdolMeshBuilder.build(w, h, argb, data.thickness);

        NativeImage image = new NativeImage(w, h, false);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int p = argb[j * w + i];
                int a = (p >>> 24) & 255;
                int r = (p >> 16) & 255;
                int g = (p >> 8) & 255;
                int b = p & 255;
                image.setPixelRGBA(i, j, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        ResourceLocation id = new ResourceLocation(KeralaTempleMod.MOD_ID, "idol/model_" + (counter++));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        return new IdolMesh(mesh.verts(), mesh.vertexCount(), id);
    }

    /**
     * The colour of every cell. In "original colours" mode this is the picture itself. Otherwise only the picture's
     * light and dark are kept (stretched to full contrast so the details show) and tinted with the material:
     * black stone for the shilas, bronze or gold for cast metal.
     */
    private static int[] colourise(IdolData data) {
        int n = data.width * data.height;
        int[] out = new int[n];
        byte[] px = data.rgba;

        float min = 1.0F;
        float max = 0.0F;
        for (int k = 0; k < n; k++) {
            if ((px[k * 4 + 3] & 255) < 128) {
                continue;
            }
            float lum = luminance(px, k);
            min = Math.min(min, lum);
            max = Math.max(max, lum);
        }
        float range = max - min;
        IdolMaterial m = data.material;

        for (int k = 0; k < n; k++) {
            if ((px[k * 4 + 3] & 255) < 128) {
                out[k] = 0;
                continue;
            }
            if (data.originalColours) {
                out[k] = 0xFF000000 | ((px[k * 4] & 255) << 16) | ((px[k * 4 + 1] & 255) << 8) | (px[k * 4 + 2] & 255);
                continue;
            }
            float l = range < 0.05F ? 0.6F : (luminance(px, k) - min) / range;
            float shade = 0.55F + 0.7F * l;
            int r = clamp((int) (m.r * shade));
            int g = clamp((int) (m.g * shade));
            int b = clamp((int) (m.b * shade));
            out[k] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return out;
    }

    private static float luminance(byte[] px, int cell) {
        int r = px[cell * 4] & 255;
        int g = px[cell * 4 + 1] & 255;
        int b = px[cell * 4 + 2] & 255;
        return (0.299F * r + 0.587F * g + 0.114F * b) / 255.0F;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
