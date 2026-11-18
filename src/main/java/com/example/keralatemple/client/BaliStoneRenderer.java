package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.block.BaliStoneBlock;
import com.example.keralatemple.blockentity.BaliStoneBlockEntity;
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

/**
 * Draws a bali stone of any size: a square base slab, then tiers that narrow like a bell, and a flat disc on top.
 * Round tiers are drawn as 16-sided prisms.
 */
@OnlyIn(Dist.CLIENT)
public class BaliStoneRenderer implements BlockEntityRenderer<BaliStoneBlockEntity> {
    private static ResourceLocation tex(String name) {
        return new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/" + name + ".png");
    }

    private static final ResourceLocation[] BODY = {tex("bali_stone"), tex("bali_stone_gold"), tex("bali_stone_silver")};
    private static final ResourceLocation[] TOP = {tex("bali_stone_top"), tex("bali_stone_gold_top"), tex("bali_stone_silver_top")};

    public BaliStoneRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BaliStoneBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers,
                       int light, int overlay) {
        if (be.getLevel() != null) {
            light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        }
        int plating = be.getBlockState().getValue(BaliStoneBlock.PLATING).ordinal();
        VertexConsumer body = buffers.getBuffer(RenderType.entityCutoutNoCull(BODY[plating]));
        VertexConsumer top = buffers.getBuffer(RenderType.entityCutoutNoCull(TOP[plating]));

        float half = be.width() / 2.0F;
        float height = be.height();
        int tiers = be.layers();
        float gap = be.gap();
        int sides = be.round() ? 16 : 4;

        ps.pushPose();
        ps.translate(0.5D, 0.0D, 0.5D);
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();

        // The base slab is always square, a fifth of the height.
        float slab = height * 0.2F;
        prism(body, m, n, light, 4, half, 0.0F, slab);
        float usable = Math.max(0.05F, height - slab - gap * tiers);
        float unit = usable / (tiers + 0.4F);
        float y = slab + gap;
        for (int i = 0; i < tiers; i++) {
            boolean last = i == tiers - 1;
            float t = tiers == 1 ? 0.0F : (float) i / (tiers - 1);
            float r = last ? half * 0.72F : half * (0.92F - 0.30F * t);
            float h = last ? unit * 1.4F : unit;
            prism(last ? top : body, m, n, light, sides, r, y, y + h);
            y += h + gap;
        }
        ps.popPose();
    }

    /** A prism centred on the origin: radius r (half-width when square), from y0 to y1. */
    private static void prism(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int sides, float r, float y0, float y1) {
        double rr = sides == 4 ? r * Math.sqrt(2.0D) : r;
        double off = sides == 4 ? Math.PI / 4.0D : 0.0D;
        float[] px = new float[sides + 1];
        float[] pz = new float[sides + 1];
        for (int k = 0; k <= sides; k++) {
            double a = off + 2.0D * Math.PI * k / sides;
            px[k] = (float) (Math.cos(a) * rr);
            pz[k] = (float) (Math.sin(a) * rr);
        }
        for (int k = 0; k < sides; k++) {
            double mid = off + 2.0D * Math.PI * (k + 0.5D) / sides;
            float nx = (float) Math.cos(mid);
            float nz = (float) Math.sin(mid);
            float u0 = (float) k / sides;
            float u1 = (float) (k + 1) / sides;
            vert(vc, m, n, light, nx, 0, nz, px[k], y0, pz[k], u0, 1.0F, 0.8F);
            vert(vc, m, n, light, nx, 0, nz, px[k + 1], y0, pz[k + 1], u1, 1.0F, 0.8F);
            vert(vc, m, n, light, nx, 0, nz, px[k + 1], y1, pz[k + 1], u1, 0.0F, 0.8F);
            vert(vc, m, n, light, nx, 0, nz, px[k], y1, pz[k], u0, 0.0F, 0.8F);
            // top and bottom fans (a triangle drawn as a quad with a repeated corner)
            float s = 0.5F / Math.max(r, 0.001F) * 0.5F;
            vert(vc, m, n, light, 0, 1, 0, 0.0F, y1, 0.0F, 0.5F, 0.5F, 1.0F);
            vert(vc, m, n, light, 0, 1, 0, px[k + 1], y1, pz[k + 1], 0.5F + px[k + 1] * s, 0.5F + pz[k + 1] * s, 1.0F);
            vert(vc, m, n, light, 0, 1, 0, px[k], y1, pz[k], 0.5F + px[k] * s, 0.5F + pz[k] * s, 1.0F);
            vert(vc, m, n, light, 0, 1, 0, px[k], y1, pz[k], 0.5F + px[k] * s, 0.5F + pz[k] * s, 1.0F);
            vert(vc, m, n, light, 0, -1, 0, 0.0F, y0, 0.0F, 0.5F, 0.5F, 0.6F);
            vert(vc, m, n, light, 0, -1, 0, px[k], y0, pz[k], 0.5F + px[k] * s, 0.5F + pz[k] * s, 0.6F);
            vert(vc, m, n, light, 0, -1, 0, px[k + 1], y0, pz[k + 1], 0.5F + px[k + 1] * s, 0.5F + pz[k + 1] * s, 0.6F);
            vert(vc, m, n, light, 0, -1, 0, px[k + 1], y0, pz[k + 1], 0.5F + px[k + 1] * s, 0.5F + pz[k + 1] * s, 0.6F);
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float nx, float ny, float nz,
                             float x, float y, float z, float u, float v, float shade) {
        int c = (int) (shade * 255.0F);
        vc.vertex(m, x, y, z).color(c, c, c, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(BaliStoneBlockEntity be) {
        return true;
    }
}
