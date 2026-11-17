package com.example.keralatemple.event;

import com.example.keralatemple.KeralaTempleMod;
import com.example.keralatemple.config.Config;
import com.example.keralatemple.ritual.KeralaClock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keeps the Overworld clock in step with real time in Kerala. */
@Mod.EventBusSubscriber(modid = KeralaTempleMod.MOD_ID)
public class TimeSyncHandler {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (Config.SYNC_TIME.get()) {
            // The mod sets the time itself, so vanilla's own day/night progression must be off.
            event.getServer().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, event.getServer());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != Level.OVERWORLD || !Config.SYNC_TIME.get()) {
            return;
        }
        if (level.getGameTime() % Config.SYNC_INTERVAL_TICKS.get() != 0) {
            return;
        }

        long tickOfDay = KeralaClock.minecraftTickOfDay(KeralaClock.now());
        long current = level.getDayTime();
        long day = Math.floorDiv(current, 24000L);
        long target = day * 24000L + tickOfDay;

        // Keep the day counter (and so the moon phase) moving forward smoothly across midnight.
        if (target - current > 12000L) {
            target -= 24000L;
        } else if (current - target > 12000L) {
            target += 24000L;
        }

        if (target != current) {
            level.setDayTime(target);
        }
    }
}
