package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.item.TempleWandItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Block items are registered here too (see ModBlocks); this class adds the non-block items. */
public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KeralaTempleMod.MOD_ID);

    // Cream (mundu) and sandal-gold colours
    public static final RegistryObject<Item> PRIEST_SPAWN_EGG = ITEMS.register("priest_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.PRIEST, 0xF2E6C8, 0xC8962E, new Item.Properties()));

    // Cream and green (the green kara of his mundu)
    public static final RegistryObject<Item> KEEZHSHANTHI_SPAWN_EGG = ITEMS.register("keezhshanthi_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KEEZHSHANTHI, 0xF2E6C8, 0x3C8C50, new Item.Properties()));

    public static final RegistryObject<Item> TEMPLE_WAND = ITEMS.register("temple_wand",
            () -> new TempleWandItem(new Item.Properties().stacksTo(1)));
}
