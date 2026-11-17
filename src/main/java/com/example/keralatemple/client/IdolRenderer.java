package com.example.keralatemple.client;

import com.example.keralatemple.block.IdolBlock;
import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the photo-based 3D idol standing on its pedestal. */
@OnlyIn(Dist.CLIENT)
public class IdolRenderer implements BlockEntityRenderer<IdolBlockEntity> {

    public IdolRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(IdolBlockEntity idol, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        String image = idol.getImage();
        if (image.isEmpty()) {
            return;
        }
        IdolCache.IdolMesh mesh = IdolCache.get(image);
        if (mesh == null) {
            return;
        }

        Direction facing = idol.getBlockState().getValue(IdolBlock.FACING);
        float height = idol.getHeight();

        poseStack.pushPose();
        // Stand on the pedestal (4/16 of a block high), centred on the block.
        poseStack.translate(0.5D, 4.0D / 16.0D, 0.5D);
        // The model's front is -z (north); turn it to face the way the block faces.
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.scale(height, height, height);

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

    @Override
    public boolean shouldRenderOffScreen(IdolBlockEntity idol) {
        return true;
    }
}
