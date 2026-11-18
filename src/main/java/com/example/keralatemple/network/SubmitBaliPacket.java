package com.example.keralatemple.network;

import com.example.keralatemple.block.BaliStoneBlock;
import com.example.keralatemple.blockentity.BaliStoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client -> server: the new size and build of a bali stone. */
public class SubmitBaliPacket {
    private final BlockPos pos;
    private final float width;
    private final float height;
    private final int layers;
    private final float gap;
    private final boolean round;

    public SubmitBaliPacket(BlockPos pos, float width, float height, int layers, float gap, boolean round) {
        this.pos = pos;
        this.width = width;
        this.height = height;
        this.layers = layers;
        this.gap = gap;
        this.round = round;
    }

    public static void encode(SubmitBaliPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeFloat(m.width);
        buf.writeFloat(m.height);
        buf.writeVarInt(m.layers);
        buf.writeFloat(m.gap);
        buf.writeBoolean(m.round);
    }

    public static SubmitBaliPacket decode(FriendlyByteBuf buf) {
        return new SubmitBaliPacket(buf.readBlockPos(), buf.readFloat(), buf.readFloat(), buf.readVarInt(),
                buf.readFloat(), buf.readBoolean());
    }

    public static void handle(SubmitBaliPacket m, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.distanceToSqr(m.pos.getX() + 0.5D, m.pos.getY() + 0.5D, m.pos.getZ() + 0.5D) > 100.0D) {
                return;
            }
            if (player.level().getBlockState(m.pos).getBlock() instanceof BaliStoneBlock
                    && player.level().getBlockEntity(m.pos) instanceof BaliStoneBlockEntity be) {
                be.set(m.width, m.height, m.layers, m.gap, m.round);
            }
        });
        context.setPacketHandled(true);
    }
}
