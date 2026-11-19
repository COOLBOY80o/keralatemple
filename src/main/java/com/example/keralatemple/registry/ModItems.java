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
    public static final RegistryObject<Item> CHENDA_SPAWN_EGG = ITEMS.register("chenda_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CHENDA, 0xC8A060, 0x703818, new Item.Properties()));
    public static final RegistryObject<Item> CHENGILA_SPAWN_EGG = ITEMS.register("chengila_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CHENGILA, 0xE6B93C, 0x703818, new Item.Properties()));
    public static final RegistryObject<Item> THIMILA_SPAWN_EGG = ITEMS.register("thimila_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.THIMILA, 0xB07840, 0x203060, new Item.Properties()));
    public static final RegistryObject<Item> KEEZHSHANTHI_SPAWN_EGG = ITEMS.register("keezhshanthi_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KEEZHSHANTHI, 0xF2E6C8, 0x3C8C50, new Item.Properties()));

    public static final RegistryObject<Item> TEMPLE_WAND = ITEMS.register("temple_wand",
            () -> new TempleWandItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ASTHABANDHAM = ITEMS.register("asthabandham",
            () -> new com.example.keralatemple.item.AsthabandhamItem(new Item.Properties()));
    public static final RegistryObject<Item> CARVING_CHISEL = ITEMS.register("carving_chisel",
            () -> new com.example.keralatemple.item.CarvingChiselItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> TEMPLE_DOOR_SET = ITEMS.register("temple_door_set",
            () -> new com.example.keralatemple.item.TempleDoorSetItem(() -> ModBlocks.TEMPLE_LEAF.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> TEMPLE_DOOR_SET_GOLD_PLATED = ITEMS.register("temple_door_set_gold_plated",
            () -> new com.example.keralatemple.item.TempleDoorSetItem(() -> ModBlocks.TEMPLE_LEAF_GOLD_PLATED.get(),
                    new Item.Properties()));
    public static final RegistryObject<Item> TEMPLE_DOOR_SET_GOLD = ITEMS.register("temple_door_set_gold",
            () -> new com.example.keralatemple.item.TempleDoorSetItem(() -> ModBlocks.TEMPLE_LEAF_GOLD.get(),
                    new Item.Properties()));
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

    // --- Flowers and leaves for pooja ---
    public static final RegistryObject<Item> TULASI = simple("tulasi");
    public static final RegistryObject<Item> THECHI = simple("thechi");
    public static final RegistryObject<Item> LOTUS = simple("lotus");
    public static final RegistryObject<Item> VILWA = simple("vilwa");
    public static final RegistryObject<Item> CHEMBARATHI = simple("chembarathi");
    public static final RegistryObject<Item> MULLA = simple("mulla");

    // --- Ingredients ---
    public static final RegistryObject<Item> RAW_RICE = simple("raw_rice");
    public static final RegistryObject<Item> NEY = simple("ney");
    public static final RegistryObject<Item> JAGGERY = simple("jaggery");
    public static final RegistryObject<Item> COCONUT = simple("coconut");
    public static final RegistryObject<Item> BANANA = simple("banana");

    public static final RegistryObject<Item> URULI_SPOON = ITEMS.register("uruli_spoon",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> KAZHAKAM_SPAWN_EGG = ITEMS.register("kazhakam_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.KAZHAKAM, 0xF2E6C8, 0xD9A0C8, new Item.Properties()));

    public static final RegistryObject<Item> COOK_SPAWN_EGG = ITEMS.register("cook_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.COOK, 0xF2E6C8, 0xE08A2E, new Item.Properties()));

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
}
