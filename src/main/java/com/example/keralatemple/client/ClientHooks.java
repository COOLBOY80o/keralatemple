package com.example.keralatemple.client;

import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.entity.AchariEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The only door from common code into client-only screens. */
@OnlyIn(Dist.CLIENT)
public final class ClientHooks {
    private ClientHooks() {}

    public static void openCarveScreen(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        BlockEntity be = mc.level == null ? null : mc.level.getBlockEntity(pos);
        com.example.keralatemple.idol.IdolData existing = be instanceof IdolBlockEntity idol ? idol.getData() : null;
        mc.setScreen(new IdolDesignerScreen(pos, -1, existing));
    }

    public static void openAchariScreen(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getEntity(entityId) instanceof AchariEntity) {
            mc.setScreen(new AchariMenuScreen(entityId));
        }
    }
}
