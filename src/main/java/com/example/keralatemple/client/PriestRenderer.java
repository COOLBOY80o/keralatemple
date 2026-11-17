package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.entity.PriestEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Player-shaped model with a custom skin (mundu and angavastram), used for both melshanti and keezhshanthi. */
@OnlyIn(Dist.CLIENT)
public class PriestRenderer extends HumanoidMobRenderer<PriestEntity, PlayerModel<PriestEntity>> {
    private final ResourceLocation texture;

    public PriestRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.texture = new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(PriestEntity entity) {
        return this.texture;
    }
}
