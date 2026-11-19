package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.block.IdolBlock;
import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.idol.IdolData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the idol (and its peetham layers) standing on top of a shila stone or idol pedestal. */
@OnlyIn(Dist.CLIENT)
public class IdolRenderer implements BlockEntityRenderer<IdolBlockEntity> {
    private static final ResourceLocation GRANITE =
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/temple_granite.png");

    private static final ResourceLocation ANJANA =
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/anjana_shila.png");
    private static final ResourceLocation KRISHNA =
            new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/krishna_shila.png");

    public IdolRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(IdolBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        IdolData data = be.getData();
        BlockState state = be.getBlockState();
        if (data == null || !(state.getBlock() instanceof IdolBlock block)) {
            return;
        }
        if (be.getLevel() != null) {
            light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().above());
        }
        Direction facing = state.getValue(IdolBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        // The model's front is -z (north); turn it to face the way the block faces.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));

        float y = block.kind().baseTop;
        long now = be.getLevel() == null ? 0L : be.getLevel().getGameTime();
        com.example.keralatemple.ritual.Liquid liquid = be.liquidAt(now);
        float elapsed = liquid == null ? 0.0F : (now - be.pourStart()) + partialTick;
        float flow = Math.min(1.0F, elapsed / 50.0F);          // how far down the idol the liquid has reached
        float alpha = liquid == null ? 0.0F : Math.min(1.0F, Math.max(0.0F, (be.pourUntil() + 30 - (now + partialTick)) / 30.0F));
        boolean running = liquid != null && now + partialTick < be.pourUntil();
        drawPeetham(poseStack, buffers, data.peetham, data.peethamSize, y, light, overlay, data.peethamStone,
                -(180.0F - facing.toYRot()));
        y += data.peethamHeight();
        if (liquid != null && data.peetham == 4) {
            drawPeethamFlow(poseStack, buffers, be, data.peethamSize, y, liquid, alpha, running, flow,
                    -(180.0F - facing.toYRot()), light);
        }
        if (be.isFixed()) {
            // the asthabandham: a ring of the eight-fold binding paste where the idol meets the peetham
            OrnamentRenderer.box(OrnamentRenderer.white(buffers), poseStack, 0.0F, y + 0.01F, 0.0F, 0.20F * data.size * 0.5F + 0.10F,
                    0.012F, 0.20F * data.size * 0.5F + 0.10F, 0xB07A34, light);
        }

        // An angi (metal cover) replaces the stone idol, a touch larger so it hugs it.
        IdolData shown = be.getAngi() != null ? be.getAngi() : data;
        IdolCache.IdolMesh mesh = IdolCache.get(shown);
        float scale = data.size * (be.getAngi() != null ? 1.03F : 1.0F);
        if (running && be.pourSource() == 1) {
            // a shankham held above and in front of the idol, its liquid falling in a thin stream
            VertexConsumer white = OrnamentRenderer.white(buffers);
            float top = y + scale;
            OrnamentRenderer.box(white, poseStack, 0.0F, top + 0.35F, 0.0F, 0.02F, 0.35F, 0.02F, liquid.rgb, light);
            poseStack.pushPose();
            poseStack.translate(0.0D, top + 0.72D, -0.06D);
            poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F));
            OrnamentRenderer.box(white, poseStack, 0.0F, 0.0F, 0.0F, 0.07F, 0.07F, 0.12F, 0xF3EAD7, light);
            OrnamentRenderer.box(white, poseStack, 0.0F, 0.0F, 0.16F, 0.045F, 0.045F, 0.05F, 0xE5B9A8, light);
            OrnamentRenderer.box(white, poseStack, 0.0F, 0.0F, -0.13F, 0.04F, 0.04F, 0.03F, liquid.rgb, light);
            poseStack.popPose();
        } else if (running) {
            // the stream falling onto the idol's head from the vessel above
            OrnamentRenderer.box(OrnamentRenderer.white(buffers), poseStack, 0.0F, y + scale + 0.5F, 0.0F, 0.035F, 0.5F,
                    0.035F, liquid.rgb, light);
        }
        poseStack.translate(0.0D, y, 0.0D);
        poseStack.scale(scale, scale, scale);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.texture()));
        float[] d = mesh.data();
        for (int i = 0; i < mesh.vertexCount(); i++) {
            int o = i * IdolMeshBuilder.STRIDE;
            int shade = (int) (d[o + 8] * 255.0F);
            consumer.vertex(matrix, d[o], d[o + 1], d[o + 2])
                    .color(shade, shade, shade, 255)
                    .uv(d[o + 6], d[o + 7])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normal, d[o + 3], d[o + 4], d[o + 5])
                    .endVertex();
        }
        if (liquid != null) {
            drawLiquidSheet(poseStack, buffers, mesh, liquid, alpha, flow, light);
        }
        OrnamentRenderer.drawGarland(poseStack, buffers, be.getGarland(), data.thickness, light);
        OrnamentRenderer.draw(poseStack, buffers, be.getOrnaments(), (float) data.width / data.height,
                data.thickness, light);
        poseStack.popPose();
    }

    /** Draws an idol standing on the origin (used for the idol a keezhshanthi carries in the seeveli). */
    public static void drawMesh(PoseStack poseStack, MultiBufferSource buffers, IdolData shown, float scale, int light) {
        IdolCache.IdolMesh mesh = IdolCache.get(shown);
        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.texture()));
        float[] d = mesh.data();
        for (int i = 0; i < mesh.vertexCount(); i++) {
            int o = i * IdolMeshBuilder.STRIDE;
            int shade = (int) (d[o + 8] * 255.0F);
            consumer.vertex(matrix, d[o], d[o + 1], d[o + 2])
                    .color(shade, shade, shade, 255)
                    .uv(d[o + 6], d[o + 7])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normal, d[o + 3], d[o + 4], d[o + 5])
                    .endVertex();
        }
        poseStack.popPose();
    }

    private static final int GOLD = 0xEBBE3C;
    private static final int DARK_RED = 0x8A1C1C;
    private static final int PINK = 0xD04A8E;
    private static final int PURPLE = 0x7E3AA8;

    /**
     * The stand under the idol: 0 none, 1 simple granite, 2 padma (gold base with pink and purple lotus petals),
     * 3 tiered gold with red bands. Every width and height is multiplied by the peetham size.
     */
    private static void drawLiquidSheet(PoseStack ps, MultiBufferSource buffers, IdolCache.IdolMesh mesh,
                                        com.example.keralatemple.ritual.Liquid liquid, float alpha, float flow, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(
                new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/block/white.png")));
        float line = 1.1F - 1.1F * flow;     // the liquid covers everything above this height (idol units)
        ps.pushPose();
        ps.scale(1.05F, 1.0F, 1.3F);
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float[] d = mesh.data();
        int r = (liquid.rgb >> 16) & 255;
        int g = (liquid.rgb >> 8) & 255;
        int b = liquid.rgb & 255;
        int a = (int) (170 * alpha);
        for (int q = 0; q + 3 < mesh.vertexCount(); q += 4) {
            float top = -1.0F;
            for (int k = 0; k < 4; k++) {
                top = Math.max(top, d[(q + k) * IdolMeshBuilder.STRIDE + 1]);
            }
            if (top < line) {
                continue;
            }
            for (int k = 0; k < 4; k++) {
                int o = (q + k) * IdolMeshBuilder.STRIDE;
                float shade = 0.75F + 0.25F * d[o + 8];
                vc.vertex(m, d[o], d[o + 1], d[o + 2]).color((int) (r * shade), (int) (g * shade), (int) (b * shade), a)
                        .uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(n, d[o + 3], d[o + 4], d[o + 5]).endVertex();
            }
        }
        ps.popPose();
    }

    /** The liquid gathering on the peetham, running along the spout (north) and falling from its end. */
    private static void drawPeethamFlow(PoseStack ps, MultiBufferSource buffers, IdolBlockEntity be, float s, float y,
                                        com.example.keralatemple.ritual.Liquid liquid, float alpha, boolean running,
                                        float flow, float northFix, int light) {
        VertexConsumer vc = OrnamentRenderer.white(buffers);
        float top = y;                       // y is already the top of the peetham
        float film = 0.006F;
        if (flow > 0.6F || !running) {
            // a film over the top of the peetham
            OrnamentRenderer.box(vc, ps, 0.0F, top + film, 0.0F, 0.34F * s, film, 0.34F * s, liquid.rgb, light);
        }
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(northFix));
        float spoutTop = y - 0.03F * s;
        if (flow > 0.7F || !running) {
            OrnamentRenderer.box(vc, ps, 0.0F, spoutTop + film, -0.485F * s, 0.05F * s, film, 0.145F * s, liquid.rgb, light);
            boolean channel = be.getLevel() != null
                    && be.getLevel().getBlockState(be.getBlockPos().north()).getBlock()
                    instanceof com.example.keralatemple.block.CarvedBlock;
            if (!channel && flow > 0.85F && alpha > 0.2F) {
                // falls from the end of the spout to the ground
                float fall = spoutTop + film;
                OrnamentRenderer.box(vc, ps, 0.0F, fall / 2.0F, -0.65F * s, 0.03F, fall / 2.0F, 0.03F, liquid.rgb, light);
            }
        }
        ps.popPose();
    }

    private static void drawPeetham(PoseStack ps, MultiBufferSource buffers, int style, float s, float y,
                                    int light, int overlay, int stone, float northFix) {
        ResourceLocation stoneTex = stone == 1 ? KRISHNA : ANJANA;
        if (style <= 0) {
            return;
        }
        if (style == 1) {
            VertexConsumer granite = buffers.getBuffer(RenderType.entityCutoutNoCull(GRANITE));
            box(granite, ps, 0.40F * s, y, y + 0.15F * s, light, overlay);
            return;
        }
        if (style == 4) {
            // Peetham cut from Anjana or Krishna shila: stepped square base, a rim, and a spout that always faces north.
            VertexConsumer rock = buffers.getBuffer(RenderType.entityCutoutNoCull(stoneTex));
            box(rock, ps, 0.48F * s, y, y + 0.10F * s, light, overlay);
            box(rock, ps, 0.42F * s, y + 0.10F * s, y + 0.18F * s, light, overlay);
            box(rock, ps, 0.34F * s, y + 0.18F * s, y + 0.30F * s, light, overlay);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(northFix));   // undo the idol's turn: the spout points to world north
            boxXZ(rock, ps, 0.0F, -0.48F * s, 0.07F * s, 0.15F * s, y + 0.18F * s, y + 0.27F * s, light, overlay);
            ps.popPose();
            return;
        }
        VertexConsumer vc = OrnamentRenderer.white(buffers);
        if (style == 2) {
            layer(vc, ps, 0.46F * s, y, y + 0.06F * s, GOLD, false, light);
            layer(vc, ps, 0.38F * s, y + 0.06F * s, y + 0.10F * s, GOLD, false, light);
            layer(vc, ps, 0.30F * s, y + 0.10F * s, y + 0.18F * s, PINK, true, light);
            layer(vc, ps, 0.24F * s, y + 0.18F * s, y + 0.24F * s, PURPLE, true, light);
            layer(vc, ps, 0.26F * s, y + 0.24F * s, y + 0.30F * s, GOLD, false, light);
        } else {
            layer(vc, ps, 0.48F * s, y, y + 0.10F * s, GOLD, false, light);
            layer(vc, ps, 0.44F * s, y + 0.10F * s, y + 0.13F * s, DARK_RED, false, light);
            layer(vc, ps, 0.40F * s, y + 0.13F * s, y + 0.23F * s, GOLD, false, light);
            layer(vc, ps, 0.36F * s, y + 0.23F * s, y + 0.26F * s, DARK_RED, false, light);
            layer(vc, ps, 0.32F * s, y + 0.26F * s, y + 0.36F * s, GOLD, false, light);
            layer(vc, ps, 0.36F * s, y + 0.36F * s, y + 0.40F * s, GOLD, false, light);
        }
    }

    /** One coloured square layer. With "petals" a second square turned 45 degrees makes an eight-point lotus. */
    private static void layer(VertexConsumer vc, PoseStack ps, float hw, float y0, float y1, int rgb,
                              boolean petals, int light) {
        float cy = (y0 + y1) / 2.0F;
        float hy = (y1 - y0) / 2.0F;
        OrnamentRenderer.box(vc, ps, 0.0F, cy, 0.0F, hw, hy, hw, rgb, light);
        if (petals) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(45.0F));
            OrnamentRenderer.box(vc, ps, 0.0F, cy, 0.0F, hw, hy, hw, rgb, light);
            ps.popPose();
        }
    }

    /** A square-based box centred on the block, half-width hw, from y0 to y1. */
    private static void box(VertexConsumer vc, PoseStack ps, float hw, float y0, float y1, int light, int overlay) {
        boxXZ(vc, ps, 0.0F, 0.0F, hw, hw, y0, y1, light, overlay);
    }

    /** A textured box centred at (cx, cz) with half-widths hx and hz, from y0 to y1. */
    private static void boxXZ(VertexConsumer vc, PoseStack ps, float cx, float cz, float hx, float hz,
                              float y0, float y1, int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float x0 = cx - hx;
        float x1 = cx + hx;
        float z0 = cz - hz;
        float z1 = cz + hz;
        face(vc, m, n, light, overlay, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        face(vc, m, n, light, overlay, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(vc, m, n, light, overlay, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        face(vc, m, n, light, overlay, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        face(vc, m, n, light, overlay, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        face(vc, m, n, light, overlay, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void face(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int overlay,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        vertex(vc, m, n, light, overlay, nx, ny, nz, ax, ay, az, 0.0F, 0.0F);
        vertex(vc, m, n, light, overlay, nx, ny, nz, bx, by, bz, 1.0F, 0.0F);
        vertex(vc, m, n, light, overlay, nx, ny, nz, cx, cy, cz, 1.0F, 1.0F);
        vertex(vc, m, n, light, overlay, nx, ny, nz, dx, dy, dz, 0.0F, 1.0F);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, int light, int overlay,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        vc.vertex(m, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(IdolBlockEntity idol) {
        return true;
    }
}
