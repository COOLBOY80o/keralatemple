package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Player-shaped model with a custom skin, used for the melshanti, keezhshanthi and achari. */
@OnlyIn(Dist.CLIENT)
public class NpcRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;
    private final String skin;

    public NpcRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.skin = textureName;
        this.texture = new ResourceLocation(KeralaTempleMod.MOD_ID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        if (entity instanceof com.example.keralatemple.entity.PriestEntity priest) {
            return NpcLooks.get(this.skin, priest.getLook(), priest.getVastram(), this.texture);
        }
        return this.texture;
    }
}
