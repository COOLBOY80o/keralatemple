package com.example.keralatemple.network;

import com.example.keralatemple.blockentity.IdolBlockEntity;
import com.example.keralatemple.entity.AchariEntity;
import com.example.keralatemple.idol.AchariCosts;
import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.idol.IdolMaterial;
import com.example.keralatemple.idol.OrnamentData;
import com.example.keralatemple.item.AngiItem;
import com.example.keralatemple.item.ChiselItem;
import com.example.keralatemple.item.OrnamentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client -> server: order an ornament (crown, necklace, armlets, belt) or an angi from an achari.
 * The server checks the achari, the materials and (for an angi) the chisel's measurements.
 */
public class SubmitOrderPacket {
    private final int entityId;
    @Nullable
    private final OrnamentData ornament;
    private final IdolMaterial angiMetal;

    private SubmitOrderPacket(int entityId, @Nullable OrnamentData ornament, IdolMaterial angiMetal) {
        this.entityId = entityId;
        this.ornament = ornament;
        this.angiMetal = angiMetal;
    }

    public static SubmitOrderPacket ornament(int entityId, OrnamentData data) {
        return new SubmitOrderPacket(entityId, data, IdolMaterial.GOLD);
    }

    public static SubmitOrderPacket angi(int entityId, IdolMaterial metal) {
        return new SubmitOrderPacket(entityId, null, metal);
    }

    public static void encode(SubmitOrderPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeBoolean(msg.ornament != null);
        if (msg.ornament != null) {
            msg.ornament.write(buf);
        } else {
            buf.writeVarInt(msg.angiMetal.ordinal());
        }
    }

    public static SubmitOrderPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        if (buf.readBoolean()) {
            return new SubmitOrderPacket(id, OrnamentData.read(buf), IdolMaterial.GOLD);
        }
        return new SubmitOrderPacket(id, null, IdolMaterial.byId(buf.readVarInt()));
    }

    public static void handle(SubmitOrderPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            Entity entity = player.serverLevel().getEntity(msg.entityId);
            if (!(entity instanceof AchariEntity achari) || achari.distanceToSqr(player) > 64.0D) {
                return;
            }
            if (achari.isBusy()) {
                player.sendSystemMessage(Component.literal("The achari is busy with another order."));
                return;
            }
            if (msg.ornament != null) {
                orderOrnament(player, achari, msg.ornament);
            } else {
                orderAngi(player, achari, msg.angiMetal);
            }
        });
        context.setPacketHandled(true);
    }

    private static void orderOrnament(ServerPlayer player, AchariEntity achari, OrnamentData data) {
        if (!data.isValid() || !pay(player, AchariCosts.forOrnament(data))) {
            return;
        }
        achari.takeOrder(player, OrnamentItem.create(data), AchariEntity.ticksFor(0.5F), data.displayName());
    }

    private static void orderAngi(ServerPlayer player, AchariEntity achari, IdolMaterial metal) {
        if (!metal.metal) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos pos = ChiselItem.getMould(player.getMainHandItem(), level);
        if (pos == null) {
            pos = ChiselItem.getMould(player.getOffhandItem(), level);
        }
        if (pos == null || !level.isLoaded(pos)) {
            player.sendSystemMessage(Component.literal("Hold a chisel that has measured an idol in this world "
                    + "(right-click the idol with the chisel)."));
            return;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IdolBlockEntity idol) || idol.getData() == null) {
            player.sendSystemMessage(Component.literal("The achari cannot find that idol any more."));
            return;
        }
        IdolData src = idol.getData();
        if (!pay(player, AchariCosts.forAngi(metal, src.size))) {
            return;
        }
        IdolData angi = new IdolData(src.width, src.height, src.rgba, src.size, src.thickness, 0, false, metal);
        achari.takeOrder(player, AngiItem.create(angi), AchariEntity.ticksFor(src.size / 2.0F * 1.5F),
                metal.label + " angi");
    }

    private static boolean pay(ServerPlayer player, Map<Item, Integer> cost) {
        if (AchariCosts.isFree(player)) {
            return true;
        }
        if (!AchariCosts.has(player, cost)) {
            player.sendSystemMessage(Component.literal("The achari needs: " + AchariCosts.describe(cost) + "."));
            return false;
        }
        AchariCosts.take(player, cost);
        return true;
    }
}
