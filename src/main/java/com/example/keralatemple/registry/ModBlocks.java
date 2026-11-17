package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.block.BaliStoneBlock;
import com.example.keralatemple.block.IdolBlock;
import com.example.keralatemple.block.MarkerBlock;
import com.example.keralatemple.block.NilavilakkuBlock;
import net.minecraft.world.item.BlockItem;
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
            () -> new IdolBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(2.0F, 8.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.STONE)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registered = BLOCKS.register(name, block);
        ModItems.ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
        return registered;
    }
}
