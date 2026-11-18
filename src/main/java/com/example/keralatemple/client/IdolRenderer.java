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
        drawPeetham(poseStack, buffers, data.peetham, y, light, overlay);
        y += data.peethamHeight();

        // An angi (metal cover) replaces the stone idol, a touch larger so it hugs it.
        IdolData shown = be.getAngi() != null ? be.getAngi() : data;
        IdolCache.IdolMesh mesh = IdolCache.get(shown);
        float scale = data.size * (be.getAngi() != null ? 1.03F : 1.0F);
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
        OrnamentRenderer.draw(poseStack, buffers, be.getOrnaments(), (float) data.width / data.height,
                data.thickness, light);
        poseStack.popPose();
    }

    /** The stand under the idol: 0 none, 1 simple, 2 padma (lotus), 3 tiered. Total heights match IdolData. */
    private static void drawPeetham(PoseStack ps, MultiBufferSource buffers, int style, float y, int light, int overlay) {
        if (style <= 0) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(GRANITE));
        switch (style) {
            case 1 -> box(vc, ps, 0.40F, y, y + 0.15F, light, overlay);
            case 2 -> {
                box(vc, ps, 0.45F, y, y + 0.10F, light, overlay);
                // Two squares, one turned 45 degrees, make an eight-petalled lotus.
                box(vc, ps, 0.30F, y + 0.10F, y + 0.22F, light, overlay);
                ps.pushPose();
                ps.mulPose(Axis.YP.rotationDegrees(45.0F));
                box(vc, ps, 0.30F, y + 0.10F, y + 0.22F, light, overlay);
                ps.popPose();
                box(vc, ps, 0.22F, y + 0.22F, y + 0.30F, light, overlay);
            }
            default -> {
                box(vc, ps, 0.48F, y, y + 0.12F, light, overlay);
                box(vc, ps, 0.40F, y + 0.12F, y + 0.24F, light, overlay);
                box(vc, ps, 0.32F, y + 0.24F, y + 0.36F, light, overlay);
                box(vc, ps, 0.36F, y + 0.36F, y + 0.40F, light, overlay);
            }
        }
    }

    /** A square-based box centred on the block, half-width hw, from y0 to y1. */
    private static void box(VertexConsumer vc, PoseStack ps, float hw, float y0, float y1, int light, int overlay) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float x0 = -hw;
        float x1 = hw;
        float z0 = -hw;
        float z1 = hw;
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
