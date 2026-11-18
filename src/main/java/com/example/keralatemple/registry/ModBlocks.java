package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.block.AduppuBlock;
import com.example.keralatemple.block.BaliStoneBlock;
import com.example.keralatemple.block.DoorFrameBlock;
import com.example.keralatemple.block.UruliBlock;
import com.example.keralatemple.block.IdolBlock;
import com.example.keralatemple.block.MarkerBlock;
import com.example.keralatemple.block.NilavilakkuBlock;
import com.example.keralatemple.block.TempleDoorBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, KeralaTempleMod.MOD_ID);

    // --- Laterite (the traditional red stone of Kerala) ---
    public static final RegistryObject<Block> LATERITE_BRICKS = registerBlock("laterite_bricks",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(1.8F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> LATERITE_BRICK_STAIRS = registerBlock("laterite_brick_stairs",
            () -> new StairBlock(() -> LATERITE_BRICKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(LATERITE_BRICKS.get())));
    public static final RegistryObject<Block> LATERITE_BRICK_SLAB = registerBlock("laterite_brick_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(LATERITE_BRICKS.get())));

    // --- Granite plinth (adhisthana base) ---
    public static final RegistryObject<Block> TEMPLE_GRANITE = registerBlock("temple_granite",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> TEMPLE_GRANITE_SLAB = registerBlock("temple_granite_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(TEMPLE_GRANITE.get())));

    // --- Copper-sheet roof ---
    public static final RegistryObject<Block> COPPER_ROOF_TILES = registerBlock("copper_roof_tiles",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.COPPER)));
    public static final RegistryObject<Block> COPPER_ROOF_STAIRS = registerBlock("copper_roof_stairs",
            () -> new StairBlock(() -> COPPER_ROOF_TILES.get().defaultBlockState(),
                    BlockBehaviour.Properties.copy(COPPER_ROOF_TILES.get())));
    public static final RegistryObject<Block> COPPER_ROOF_SLAB = registerBlock("copper_roof_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.copy(COPPER_ROOF_TILES.get())));

    // --- Carved wood ---
    public static final RegistryObject<Block> CARVED_WOOD_PILLAR = registerBlock("carved_wood_pillar",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));
    public static final RegistryObject<Block> CARVED_WOOD_PANEL = registerBlock("carved_wood_panel",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    // --- Nilavilakku (brass oil lamp) ---
    public static final RegistryObject<Block> NILAVILAKKU = registerBlock("nilavilakku",
            () -> new NilavilakkuBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(0.5F)
                    .lightLevel(state -> 15)
                    .noOcclusion()
                    .sound(SoundType.LANTERN)));

    // --- Ritual markers used by the priest ---
    public static final RegistryObject<Block> POOJA_STATION = registerBlock("pooja_station",
            () -> new MarkerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(0.3F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)));
    public static final RegistryObject<Block> SEEVELI_WAYPOINT = registerBlock("seeveli_waypoint",
            () -> new MarkerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(0.3F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)));

    // --- Bali stone (small balikkal) and the idol pedestal ---
    public static final RegistryObject<Block> BALI_STONE = registerBlock("bali_stone",
            () -> new BaliStoneBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> IDOL_PEDESTAL = registerBlock("idol_pedestal",
            () -> new IdolBlock(IdolBlock.Kind.PEETHAM, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE)));

    // --- Sacred stones for carving idols (left-click with the Chisel) ---
    public static final RegistryObject<Block> ANJANA_SHILA = registerBlock("anjana_shila",
            () -> new IdolBlock(IdolBlock.Kind.ANJANA, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> KRISHNA_SHILA = registerBlock("krishna_shila",
            () -> new IdolBlock(IdolBlock.Kind.KRISHNA, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.DEEPSLATE)));

    // --- Kudam Station: where the keezhshanthis fill the pots for ayirakudam ---
    public static final RegistryObject<Block> CARVED_BLOCK = BLOCKS.register("carved_block",
            () -> new com.example.keralatemple.block.CarvedBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 8.0F)
                    .noOcclusion()
                    .dynamicShape()
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> TEMPLE_LEAF = BLOCKS.register("temple_leaf",
            () -> new com.example.keralatemple.block.TempleLeafBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(3.0F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD), () -> ModItems.TEMPLE_DOOR_SET.get()));
    public static final RegistryObject<Block> TEMPLE_LEAF_GOLD_PLATED = BLOCKS.register("temple_leaf_gold_plated",
            () -> new com.example.keralatemple.block.TempleLeafBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.WOOD), () -> ModItems.TEMPLE_DOOR_SET_GOLD_PLATED.get()));
    public static final RegistryObject<Block> TEMPLE_LEAF_GOLD = BLOCKS.register("temple_leaf_gold",
            () -> new com.example.keralatemple.block.TempleLeafBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(3.0F, 6.0F)
                    .noOcclusion()
                    .sound(SoundType.METAL), () -> ModItems.TEMPLE_DOOR_SET_GOLD.get()));
    public static final RegistryObject<Block> WELL_PULLEY = registerBlock("well_pulley",
            () -> new com.example.keralatemple.block.WellPulleyBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .noOcclusion()
                    .sound(SoundType.WOOD)));
    public static final RegistryObject<Block> MUSIC_STATION = registerBlock("music_station",
            () -> new com.example.keralatemple.block.MusicStationBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.3F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)));
    public static final RegistryObject<Block> KUDAM_STATION = registerBlock("kudam_station",
            () -> new MarkerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(0.3F)
                    .noOcclusion()
                    .sound(SoundType.WOOL)));

    // --- Door frame: one block whose material and style you change in game with the Chisel ---
    public static final RegistryObject<Block> TEMPLE_DOOR_FRAME = registerBlock("temple_door_frame",
            () -> new DoorFrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F, 6.0F)
                    .sound(SoundType.STONE)));

    // --- Kitchen: hearth and cooking vessel ---
    public static final RegistryObject<Block> ADUPPU = registerBlock("aduppu",
            () -> new AduppuBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED)
                    .strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> state.getValue(AduppuBlock.LIT) ? 13 : 0)
                    .sound(SoundType.STONE)));
    public static final RegistryObject<Block> URULI = registerBlock("uruli",
            () -> new UruliBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .strength(1.5F, 4.0F)
                    .noOcclusion()
                    .sound(SoundType.COPPER)));

    // --- Temple doors ---
    public static final RegistryObject<Block> TEMPLE_DOOR = registerDoor("temple_door", MapColor.WOOD, SoundType.WOOD);
    public static final RegistryObject<Block> TEMPLE_DOOR_GOLD_PLATED =
            registerDoor("temple_door_gold_plated", MapColor.GOLD, SoundType.WOOD);
    public static final RegistryObject<Block> TEMPLE_DOOR_GOLD = registerDoor("temple_door_gold", MapColor.GOLD, SoundType.METAL);

    private static RegistryObject<Block> registerDoor(String name, MapColor colour, SoundType sound) {
        RegistryObject<Block> registered = BLOCKS.register(name, () -> new TempleDoorBlock(BlockBehaviour.Properties.of()
                .mapColor(colour)
                .strength(3.0F, 6.0F)
                .sound(sound)
                .noOcclusion()
                .pushReaction(PushReaction.DESTROY)));
        ModItems.ITEMS.register(name, () -> new DoubleHighBlockItem(registered.get(), new Item.Properties()));
        return registered;
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }
}
