package com.example.keralatemple;

import com.example.keralatemple.config.Config;
import com.example.keralatemple.network.ModNetwork;
import com.example.keralatemple.registry.ModBlockEntities;
import com.example.keralatemple.registry.ModBlocks;
import com.example.keralatemple.registry.ModCreativeTabs;
import com.example.keralatemple.registry.ModEntities;
import com.example.keralatemple.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(KeralaTempleMod.MOD_ID)
public class KeralaTempleMod {
    public static final String MOD_ID = "keralatemple";

    public KeralaTempleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Blocks first: ModBlocks also queues the matching block items into ModItems.ITEMS.
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        ModNetwork.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
