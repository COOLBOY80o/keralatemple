package com.example.keralatemple.registry;

import com.example.keralatemple.KeralaTempleMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The temple instruments' own recorded-style sounds. */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, KeralaTempleMod.MOD_ID);

    public static final RegistryObject<SoundEvent> CHENDA_HIT = reg("chenda_hit");
    public static final RegistryObject<SoundEvent> CHENDA_SOFT = reg("chenda_soft");
    public static final RegistryObject<SoundEvent> THIMILA_HIT = reg("thimila_hit");
    public static final RegistryObject<SoundEvent> THIMILA_SOFT = reg("thimila_soft");
    public static final RegistryObject<SoundEvent> CHENGILA_HIT = reg("chengila_hit");
    public static final RegistryObject<SoundEvent> CHENGILA_SOFT = reg("chengila_soft");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(KeralaTempleMod.MOD_ID, name)));
    }

    private ModSounds() {}
}
