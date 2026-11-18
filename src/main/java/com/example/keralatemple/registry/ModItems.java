package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.item.AngiItem;
import com.example.keralatemple.item.CastIdolItem;
import com.example.keralatemple.item.OrnamentItem;
import com.example.keralatemple.item.ChiselItem;
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

    public static final RegistryObject<Item> CHISEL = ITEMS.register("chisel",
            () -> new ChiselItem(new Item.Properties().stacksTo(1).durability(250)));

    public static final RegistryObject<Item> CAST_IDOL = ITEMS.register("cast_idol",
            () -> new CastIdolItem(new Item.Properties().stacksTo(1)));

    // Bronze and soot
    public static final RegistryObject<Item> ACHARI_SPAWN_EGG = ITEMS.register("achari_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ACHARI, 0xB07A3A, 0x3A2A1E, new Item.Properties()));

    public static final RegistryObject<Item> ORNAMENT = ITEMS.register("ornament",
            () -> new OrnamentItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ANGI = ITEMS.register("angi",
            () -> new AngiItem(new Item.Properties().stacksTo(1)));
}
