package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.blockentity.CarvedBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the small cubes of a carved block, leaving out the faces that touch another cube. */
@OnlyIn(Dist.CLIENT)
public class CarvedRenderer implements BlockEntityRenderer<CarvedBlockEntity> {
    private static final ResourceLocation[] TEXTURES = {
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/temple_granite.png"),
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/laterite_bricks.png"),
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/carved_wood_panel.png"),
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/bali_stone_gold.png")};

    public CarvedRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CarvedBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers,
                       int light, int overlay) {
        if (be.getLevel() != null) {
            light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURES[be.material()]));
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        final float s = 1.0F / CarvedBlockEntity.N;
        for (int y = 0; y < CarvedBlockEntity.N; y++) {
            for (int z = 0; z < CarvedBlockEntity.N; z++) {
                for (int x = 0; x < CarvedBlockEntity.N; x++) {
                    if (!be.has(x, y, z)) {
                        continue;
                    }
                    float x0 = x * s;
                    float x1 = x0 + s;
                    float y0 = y * s;
                    float y1 = y0 + s;
                    float z0 = z * s;
                    float z1 = z0 + s;
                    if (!be.has(x, y - 1, z)) {
                        quad(vc, m, n, light, 0, -1, 0, 0.6F, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
                    }
                    if (!be.has(x, y + 1, z)) {
                        quad(vc, m, n, light, 0, 1, 0, 1.0F, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
                    }
                    if (!be.has(x, y, z - 1)) {
                        quad(vc, m, n, light, 0, 0, -1, 0.8F, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
                    }
                    if (!be.has(x, y, z + 1)) {
                        quad(vc, m, n, light, 0, 0, 1, 0.8F, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
                    }
                    if (!be.has(x - 1, y, z)) {
                        quad(vc, m, n, light, -1, 0, 0, 0.7F, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
                    }
                    if (!be.has(x + 1, y, z)) {
                        quad(vc, m, n, light, 1, 0, 0, 0.7F, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
                    }
                }
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float shade, float... p) {
        for (int i = 0; i < 4; i++) {
            float x = p[i * 3];
            float y = p[i * 3 + 1];
            float z = p[i * 3 + 2];
            float u;
            float v;
            if (ny != 0) {
                u = x;
                v = z;
            } else if (nz != 0) {
                u = x;
                v = 1.0F - y;
            } else {
                u = z;
                v = 1.0F - y;
            }
            int c = (int) (shade * 255.0F);
            vc.vertex(m, x, y, z).color(c, c, c, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light).normal(n, nx, ny, nz).endVertex();
        }
    }
}
