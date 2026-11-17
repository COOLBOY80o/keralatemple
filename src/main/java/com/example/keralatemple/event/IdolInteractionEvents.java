package com.example.keralatemple.event;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.block.IdolBlock;
import com.example.keralatemple.client.ClientHooks;
import com.example.keralatemple.entity.AchariEntity;
import com.example.keralatemple.item.ChiselItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

/**
 * Left-click hooks:
 *  - chisel on an Anjana / Krishna Shila opens the idol designer (and does not break the stone);
 *  - left-click on the achari opens the designer to order a metal idol (and does not hurt him).
 */
@Mod.EventBusSubscriber(modid = KeralaTempleMod.MOD_ID)
public class IdolInteractionEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof ChiselItem)) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof IdolBlock block) || block.kind() == IdolBlock.Kind.PEETHAM) {
            return;
        }
        event.setCanceled(true);   // never break the stone with the chisel
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        if (event.getLevel().isClientSide) {
            BlockPos pos = event.getPos();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCarveScreen(pos));
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof AchariEntity)) {
            return;
        }
        event.setCanceled(true);   // the achari cannot be hit by players
        if (event.getEntity().level().isClientSide) {
            int id = event.getTarget().getId();
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openAchariScreen(id));
        }
    }
}
