package com.example.keralatemple.event;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.entity.PriestEntity;
import com.example.keralatemple.registry.ModEntities;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KeralaTempleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCommonEvents {

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.PRIEST.get(), PriestEntity.createAttributes().build());
        event.put(ModEntities.ACHARI.get(), PriestEntity.createAttributes().build());
        event.put(ModEntities.KEEZHSHANTHI.get(), PriestEntity.createAttributes().build());
    }
}
