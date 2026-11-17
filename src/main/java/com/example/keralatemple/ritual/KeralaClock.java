package com.example.keralatemple.ritual;

import com.example.keralatemple.config.Config;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Real-world time in Kerala (configurable time zone) and its mapping onto Minecraft's day. */
public final class KeralaClock {
    private static final ZoneId FALLBACK = ZoneId.of("Asia/Kolkata");

    private KeralaClock() {}

    public static ZoneId zone() {
        try {
            return ZoneId.of(Config.TIME_ZONE.get());
        } catch (Exception e) {
            return FALLBACK;
        }
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.now(zone());
    }

    /**
     * Maps wall-clock time onto Minecraft's 24000-tick day.
     * Minecraft tick 0 is sunrise, so 06:00 -> 0, 12:00 -> 6000 (noon), 18:00 -> 12000 (sunset), 00:00 -> 18000 (midnight).
     */
    public static long minecraftTickOfDay(ZonedDateTime time) {
        double seconds = time.toLocalTime().toSecondOfDay() + time.getNano() / 1_000_000_000.0;
        double ticks = seconds / 86400.0 * 24000.0 - 6000.0;
        ticks = ((ticks % 24000.0) + 24000.0) % 24000.0;
        return (long) ticks;
    }
}
