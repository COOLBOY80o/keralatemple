package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.idol.IdolMaterial;
import com.example.keralatemple.idol.OrnamentData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws jewellery on an idol out of small coloured boxes. Everything is in the idol's own units: height 0 to 1,
 * centred on x = 0, the front facing -z. Positions are tuned for a typical standing deity; a picture with a very
 * different pose will not line up perfectly.
 */
@OnlyIn(Dist.CLIENT)
public final class OrnamentRenderer {
    private static final ResourceLocation WHITE =
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/white.png");

    private OrnamentRenderer() {}

    public static void draw(PoseStack ps, MultiBufferSource buffers, List<OrnamentData> list,
                            float aspect, float thickness, int light) {
        if (list.isEmpty()) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        float zf = -0.14F * thickness;   // front surface of the idol's middle
        for (OrnamentData o : list) {
            int metal = metalColour(o.metal);
            switch (o.type) {
                case CROWN -> {
                    box(vc, ps, 0.0F, 0.935F, 0.0F, 0.080F, 0.020F, 0.070F, metal, light);
                    box(vc, ps, 0.0F, 0.975F, 0.0F, 0.065F, 0.020F, 0.055F, metal, light);
                    box(vc, ps, 0.0F, 1.020F, 0.0F, 0.035F, 0.025F, 0.035F, metal, light);
                    box(vc, ps, 0.0F, 1.055F, 0.0F, 0.012F, 0.012F, 0.012F, metal, light);
                    for (int i = 0; i < OrnamentData.SLOTS; i++) {
                        jewel(vc, ps, -0.060F + 0.030F * i, 0.935F, -0.072F, 0.011F, o.jewels[i], light);
                    }
                }
                case NECKLACE -> {
                    for (int k = 0; k < 9; k++) {
                        float t = (k - 4) / 4.0F;
                        float x = t * 0.09F;
                        float y = 0.735F - 0.05F * (1.0F - t * t);
                        box(vc, ps, x, y, zf - 0.004F, 0.008F, 0.008F, 0.008F, metal, light);
                        if (k % 2 == 0) {
                            jewel(vc, ps, x, y - 0.002F, zf - 0.012F, 0.012F, o.jewels[k / 2], light);
                        }
                    }
                }
                case ARMLETS -> {
                    float x = Math.min(0.20F, Math.max(0.09F, aspect * 0.38F));
                    for (int side = -1; side <= 1; side += 2) {
                        box(vc, ps, side * x, 0.62F, zf * 0.4F, 0.022F, 0.035F, 0.05F, metal, light);
                        int base = side < 0 ? 0 : 2;
                        jewel(vc, ps, side * x, 0.635F, zf * 0.4F - 0.052F, 0.011F, o.jewels[base], light);
                        jewel(vc, ps, side * x, 0.605F, zf * 0.4F - 0.052F, 0.011F, o.jewels[base + 1], light);
                    }
                }
                case BELT -> {
                    box(vc, ps, 0.0F, 0.46F, 0.0F, 0.10F, 0.015F, 0.085F, metal, light);
                    for (int i = 0; i < OrnamentData.SLOTS; i++) {
                        jewel(vc, ps, -0.08F + 0.04F * i, 0.46F, -0.087F, 0.011F, o.jewels[i], light);
                    }
                }
            }
        }
    }

    public static int metalColour(IdolMaterial m) {
        return (m.r << 16) | (m.g << 8) | m.b;
    }

    private static void jewel(VertexConsumer vc, PoseStack ps, float x, float y, float z, float s, int jewel, int light) {
        if (jewel > 0 && jewel < OrnamentData.JEWEL_COLOURS.length) {
            box(vc, ps, x, y, z, s, s, s, OrnamentData.JEWEL_COLOURS[jewel], light);
        }
    }

    private static void box(VertexConsumer vc, PoseStack ps, float cx, float cy, float cz,
                            float hx, float hy, float hz, int rgb, int light) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        float x0 = cx - hx;
        float x1 = cx + hx;
        float y0 = cy - hy;
        float y1 = cy + hy;
        float z0 = cz - hz;
        float z1 = cz + hz;
        quad(vc, m, n, r, g, b, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(vc, m, n, r, g, b, light, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(vc, m, n, r, g, b, light, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(vc, m, n, r, g, b, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(vc, m, n, r, g, b, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(vc, m, n, r, g, b, light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int r, int g, int b, int light,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        v(vc, m, n, r, g, b, light, nx, ny, nz, ax, ay, az, 0, 0);
        v(vc, m, n, r, g, b, light, nx, ny, nz, bx, by, bz, 1, 0);
        v(vc, m, n, r, g, b, light, nx, ny, nz, cx, cy, cz, 1, 1);
        v(vc, m, n, r, g, b, light, nx, ny, nz, dx, dy, dz, 0, 1);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, int r, int g, int b, int light,
                          float nx, float ny, float nz, float x, float y, float z, float u, float w) {
        vc.vertex(m, x, y, z).color(r, g, b, 255).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
    }
}
