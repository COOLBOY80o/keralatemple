package com.example.keralatemple.client;

import com.example.keralatemple.blockentity.WellPulleyBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The rope and bucket of a well pulley: the bucket goes down into the well and comes back up full. */
@OnlyIn(Dist.CLIENT)
public class WellPulleyRenderer implements BlockEntityRenderer<WellPulleyBlockEntity> {
    private static final int ROPE = 0xC8B070;
    private static final int WOOD = 0x6B4A2B;
    private static final int WATER = 0x3A6FD0;

    public WellPulleyRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WellPulleyBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers,
                       int light, int overlay) {
        if (be.getLevel() != null) {
            light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos());
        }
        float t = be.progress(partialTick);
        float depth = t < 0.0F ? 0.0F : (float) Math.sin(Math.PI * t) * 2.2F;
        float rollerY = 0.78F;
        float bucketTop = 0.55F - depth;
        VertexConsumer vc = OrnamentRenderer.white(buffers);
        // rope from the roller to the bucket
        float ropeMid = (rollerY + bucketTop) / 2.0F;
        OrnamentRenderer.box(vc, ps, 0.5F, ropeMid, 0.5F, 0.012F, (rollerY - bucketTop) / 2.0F, 0.012F, ROPE, light);
        // the bucket (a wooden pail), with water in it on the way up
        OrnamentRenderer.box(vc, ps, 0.5F, bucketTop - 0.09F, 0.5F, 0.1F, 0.09F, 0.1F, WOOD, light);
        if (t > 0.5F) {
            OrnamentRenderer.box(vc, ps, 0.5F, bucketTop - 0.005F, 0.5F, 0.085F, 0.012F, 0.085F, WATER, light);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(WellPulleyBlockEntity be) {
        return true;
    }
}
