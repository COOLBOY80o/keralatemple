package com.example.keralatemple.client;

import com.example.keralatemple.entity.PriestEntity;
import com.example.keralatemple.idol.IdolData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The idol the keezhshanthi carries in the seeveli: on his head (a blue cushion with gold studs under a white
 * arch) or held in his arms.
 */
@OnlyIn(Dist.CLIENT)
public class CarryLayer<T extends Mob> extends RenderLayer<T, PlayerModel<T>> {
    private static final int BLUE = 0x1E2A78;
    private static final int GOLD = 0xEBBE3C;
    private static final int WHITE = 0xF2F0EA;

    public CarryLayer(RenderLayerParent<T, PlayerModel<T>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, T entity, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof PriestEntity priest) || priest.carryMode() == 0) {
            return;
        }
        IdolData idol = priest.carriedIdol();
        if (idol == null) {
            return;
        }
        ps.pushPose();
        // Model space has y pointing down; (-1,-1,1) turns it into an ordinary frame with the front at -z.
        if (priest.carryMode() == 2) {
            getParentModel().head.translateAndRotate(ps);
            ps.translate(0.0D, -0.5D, 0.0D);
            ps.scale(-1.0F, -1.0F, 1.0F);
            VertexConsumer vc = OrnamentRenderer.white(buffers);
            // The cushion: a wide blue pad with gold squares round it.
            OrnamentRenderer.box(vc, ps, 0.0F, 0.07F, 0.0F, 0.34F, 0.07F, 0.34F, BLUE, light);
            for (int i = -1; i <= 1; i++) {
                float o = i * 0.18F;
                OrnamentRenderer.box(vc, ps, o, 0.07F, -0.345F, 0.045F, 0.045F, 0.01F, GOLD, light);
                OrnamentRenderer.box(vc, ps, o, 0.07F, 0.345F, 0.045F, 0.045F, 0.01F, GOLD, light);
                OrnamentRenderer.box(vc, ps, -0.345F, 0.07F, o, 0.01F, 0.045F, 0.045F, GOLD, light);
                OrnamentRenderer.box(vc, ps, 0.345F, 0.07F, o, 0.01F, 0.045F, 0.045F, GOLD, light);
            }
            ps.translate(0.0D, 0.14D, 0.0D);
            IdolRenderer.drawMesh(ps, buffers, idol, 0.55F, light);
            // The white arch (prabhavali) behind and around the idol, with a golden top.
            float hw = Math.max(0.2F, 0.55F * idol.width / (float) idol.height / 2.0F + 0.06F);
            OrnamentRenderer.box(vc, ps, -hw, 0.33F, 0.0F, 0.025F, 0.33F, 0.03F, WHITE, light);
            OrnamentRenderer.box(vc, ps, hw, 0.33F, 0.0F, 0.025F, 0.33F, 0.03F, WHITE, light);
            OrnamentRenderer.box(vc, ps, 0.0F, 0.68F, 0.0F, hw, 0.03F, 0.03F, WHITE, light);
            OrnamentRenderer.box(vc, ps, -hw + 0.05F, 0.64F, 0.0F, 0.04F, 0.04F, 0.03F, WHITE, light);
            OrnamentRenderer.box(vc, ps, hw - 0.05F, 0.64F, 0.0F, 0.04F, 0.04F, 0.03F, WHITE, light);
            OrnamentRenderer.box(vc, ps, 0.0F, 0.76F, 0.0F, 0.03F, 0.06F, 0.03F, GOLD, light);
        } else {
            getParentModel().body.translateAndRotate(ps);
            ps.translate(0.0D, 0.6D, -0.45D);
            ps.scale(-1.0F, -1.0F, 1.0F);
            IdolRenderer.drawMesh(ps, buffers, idol, 0.5F, light);
        }
        ps.popPose();
    }
}
