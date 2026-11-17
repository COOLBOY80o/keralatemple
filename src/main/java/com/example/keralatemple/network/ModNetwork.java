package com.example.keralatemple.network;

import com.example.keralatemple.KeralaTempleMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(KeralaTempleMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private ModNetwork() {}

    public static void register() {
        CHANNEL.registerMessage(0, SubmitIdolPacket.class,
                SubmitIdolPacket::encode, SubmitIdolPacket::decode, SubmitIdolPacket::handle);
    }
}
