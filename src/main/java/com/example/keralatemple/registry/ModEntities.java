package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.entity.AchariEntity;
import com.example.keralatemple.entity.CookEntity;
import com.example.keralatemple.entity.KazhakamEntity;
import com.example.keralatemple.entity.KeezhshanthiEntity;
import com.example.keralatemple.entity.PriestEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KeralaTempleMod.MOD_ID);

    public static final RegistryObject<EntityType<PriestEntity>> PRIEST = ENTITY_TYPES.register("priest",
            () -> EntityType.Builder.of(PriestEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(KeralaTempleMod.MOD_ID, "priest").toString()));

    public static final RegistryObject<EntityType<KeezhshanthiEntity>> KEEZHSHANTHI = ENTITY_TYPES.register("keezhshanthi",
            () -> EntityType.Builder.of(KeezhshanthiEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(KeralaTempleMod.MOD_ID, "keezhshanthi").toString()));

    public static final RegistryObject<EntityType<AchariEntity>> ACHARI = ENTITY_TYPES.register("achari",
            () -> EntityType.Builder.of(AchariEntity::new, MobCategory.MISC)
                    .sized(0.5F, 1.6F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(KeralaTempleMod.MOD_ID, "achari").toString()));

    public static final RegistryObject<EntityType<KazhakamEntity>> KAZHAKAM = ENTITY_TYPES.register("kazhakam",
            () -> EntityType.Builder.of(KazhakamEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(KeralaTempleMod.MOD_ID, "kazhakam").toString()));

    public static final RegistryObject<EntityType<CookEntity>> COOK = ENTITY_TYPES.register("cook",
            () -> EntityType.Builder.of(CookEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(KeralaTempleMod.MOD_ID, "cook").toString()));
}
