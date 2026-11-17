package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KeralaTempleMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> TEMPLE_TAB = CREATIVE_TABS.register("temple_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.keralatemple.temple_tab"))
                    .icon(() -> new ItemStack(ModBlocks.NILAVILAKKU.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.LATERITE_BRICKS.get());
                        output.accept(ModBlocks.LATERITE_BRICK_STAIRS.get());
                        output.accept(ModBlocks.LATERITE_BRICK_SLAB.get());
                        output.accept(ModBlocks.TEMPLE_GRANITE.get());
                        output.accept(ModBlocks.TEMPLE_GRANITE_SLAB.get());
                        output.accept(ModBlocks.COPPER_ROOF_TILES.get());
                        output.accept(ModBlocks.COPPER_ROOF_STAIRS.get());
                        output.accept(ModBlocks.COPPER_ROOF_SLAB.get());
                        output.accept(ModBlocks.CARVED_WOOD_PILLAR.get());
                        output.accept(ModBlocks.CARVED_WOOD_PANEL.get());
                        output.accept(ModBlocks.NILAVILAKKU.get());
                        output.accept(ModBlocks.POOJA_STATION.get());
                        output.accept(ModBlocks.SEEVELI_WAYPOINT.get());
                        output.accept(ModBlocks.BALI_STONE.get());
                        output.accept(ModBlocks.IDOL_PEDESTAL.get());
                        output.accept(ModItems.TEMPLE_WAND.get());
                        output.accept(ModItems.PRIEST_SPAWN_EGG.get());
                        output.accept(ModItems.KEEZHSHANTHI_SPAWN_EGG.get());
                    })
                    .build());
}
