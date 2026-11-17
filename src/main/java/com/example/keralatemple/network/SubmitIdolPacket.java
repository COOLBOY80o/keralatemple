package com.example.keralatemple.network;

import com.example.keralatemple.block.IdolBlock;
import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.entity.AchariEntity;
import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.item.ChiselItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> server: "carve this idol into that stone" or "ask that achari to cast this idol".
 * Everything is re-checked on the server (distance, tool, block type, data limits).
 */
public class SubmitIdolPacket {
    private final boolean toEntity;
    private final BlockPos pos;
    private final int entityId;
    private final IdolData data;

    private SubmitIdolPacket(boolean toEntity, BlockPos pos, int entityId, IdolData data) {
        this.toEntity = toEntity;
        this.pos = pos;
        this.entityId = entityId;
        this.data = data;
    }

    public static SubmitIdolPacket forBlock(BlockPos pos, IdolData data) {
        return new SubmitIdolPacket(false, pos, -1, data);
    }

    public static SubmitIdolPacket forEntity(int entityId, IdolData data) {
        return new SubmitIdolPacket(true, BlockPos.ZERO, entityId, data);
    }

    public static void encode(SubmitIdolPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.toEntity);
        if (msg.toEntity) {
            buf.writeVarInt(msg.entityId);
        } else {
            buf.writeBlockPos(msg.pos);
        }
        msg.data.write(buf);
    }

    public static SubmitIdolPacket decode(FriendlyByteBuf buf) {
        boolean toEntity = buf.readBoolean();
        BlockPos pos = BlockPos.ZERO;
        int entityId = -1;
        if (toEntity) {
            entityId = buf.readVarInt();
        } else {
            pos = buf.readBlockPos();
        }
        return new SubmitIdolPacket(toEntity, pos, entityId, IdolData.read(buf));
    }

    public static void handle(SubmitIdolPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !msg.data.isValid()) {
                return;
            }
            if (msg.toEntity) {
                handleEntity(player, msg);
            } else {
                handleBlock(player, msg);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleBlock(ServerPlayer player, SubmitIdolPacket msg) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(msg.pos) || !msg.pos.closerToCenterThan(player.position(), 8.0D)) {
            return;
        }
        BlockState state = level.getBlockState(msg.pos);
        if (!(state.getBlock() instanceof IdolBlock block) || block.kind() == IdolBlock.Kind.PEETHAM) {
            return;
        }
        boolean hasChisel = player.getMainHandItem().getItem() instanceof ChiselItem
                || player.getOffhandItem().getItem() instanceof ChiselItem;
        if (!hasChisel) {
            return;
        }
        BlockEntity be = level.getBlockEntity(msg.pos);
        if (be instanceof IdolBlockEntity idol) {
            // The stone decides the material, whatever the client says.
            idol.setData(msg.data.withMaterial(block.kind().material));
        }
    }

    private static void handleEntity(ServerPlayer player, SubmitIdolPacket msg) {
        Entity entity = player.serverLevel().getEntity(msg.entityId);
        if (!(entity instanceof AchariEntity achari) || achari.distanceToSqr(player) > 64.0D) {
            return;
        }
        if (!msg.data.material.metal) {
            return;
        }
        achari.takeOrder(player, msg.data);
    }
}
