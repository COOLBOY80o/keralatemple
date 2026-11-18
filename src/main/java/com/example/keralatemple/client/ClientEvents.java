package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.registry.ModBlockEntities;
import com.example.keralatemple.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KeralaTempleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRIEST.get(), ctx -> new NpcRenderer<>(ctx, "priest"));
        event.registerEntityRenderer(ModEntities.KEEZHSHANTHI.get(), ctx -> new NpcRenderer<>(ctx, "keezhshanthi"));
        event.registerEntityRenderer(ModEntities.KAZHAKAM.get(), ctx -> new NpcRenderer<>(ctx, "kazhakam"));
        event.registerEntityRenderer(ModEntities.COOK.get(), ctx -> new NpcRenderer<>(ctx, "cook"));
        event.registerEntityRenderer(ModEntities.ACHARI.get(), ctx -> new NpcRenderer<>(ctx, "achari"));
        event.registerBlockEntityRenderer(ModBlockEntities.IDOL.get(), IdolRenderer::new);
    }
}
