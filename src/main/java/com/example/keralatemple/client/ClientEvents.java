package com.example.keralatemple.client;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.entity.PriestEntity;
import com.example.keralatemple.registry.ModBlockEntities;
import com.example.keralatemple.registry.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;
import java.nio.file.Files;

@Mod.EventBusSubscriber(modid = KeralaTempleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.PRIEST.get(), ctx -> new PriestRenderer(ctx, "priest"));
        // Explicit type argument: the renderer is an EntityRenderer<PriestEntity>, which also serves the subclass.
        event.<PriestEntity>registerEntityRenderer(ModEntities.KEEZHSHANTHI.get(), ctx -> new PriestRenderer(ctx, "keezhshanthi"));
        event.registerBlockEntityRenderer(ModBlockEntities.IDOL.get(), IdolRenderer::new);
    }

    /** Creates the folder where the player drops idol photos. */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                Files.createDirectories(IdolCache.folder());
            } catch (IOException ignored) {
                // The folder can also be created by hand.
            }
        });
    }
}
