package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.blockentity.IdolBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, KeralaTempleMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<IdolBlockEntity>> IDOL = BLOCK_ENTITIES.register("idol",
            () -> BlockEntityType.Builder.of(IdolBlockEntity::new, ModBlocks.IDOL_PEDESTAL.get()).build(null));
}
