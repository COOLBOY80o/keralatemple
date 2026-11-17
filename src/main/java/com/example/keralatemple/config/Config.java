package com.example.keralatemple.config;

import com.example.keralatemple.ritual.RitualSchedule;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SYNC_TIME = BUILDER
            .comment("Lock the Overworld day/night cycle to the real time in the configured time zone.",
                    "Turning this off does not turn the daylight cycle gamerule back on; use /gamerule doDaylightCycle true.")
            .define("syncTimeToKerala", true);

    public static final ForgeConfigSpec.ConfigValue<String> TIME_ZONE = BUILDER
            .comment("Java time zone ID used for time sync and for the ritual schedule. Kerala uses Asia/Kolkata (IST, UTC+5:30).")
            .define("timeZone", "Asia/Kolkata");

    public static final ForgeConfigSpec.IntValue SYNC_INTERVAL_TICKS = BUILDER
            .comment("How often (in game ticks) the world time is re-synced to real time. 20 = once per second.")
            .defineInRange("syncIntervalTicks", 20, 1, 1200);

    public static final ForgeConfigSpec.BooleanValue ANNOUNCE_RITUALS = BUILDER
            .comment("Show an action-bar message to nearby players when the priest begins a ritual.")
            .define("announceRituals", true);

    public static final ForgeConfigSpec.IntValue MARKER_SEARCH_RADIUS = BUILDER
            .comment("How far (in blocks) around its home the priest looks for Pooja Station and Seeveli Waypoint blocks",
                    "when the priest is not inside a registered temple.")
            .defineInRange("markerSearchRadius", 24, 4, 64);

    public static final ForgeConfigSpec.IntValue COMMAND_PERMISSION_LEVEL = BUILDER
            .comment("Permission level needed to use the Temple Wand and the commands that change temples, schedules, idols",
                    "and balikkal templates. 2 = operators / cheats on (default). Set to 0 to let every player do it,",
                    "which is handy in a single-player world without cheats.")
            .defineInRange("commandPermissionLevel", 2, 0, 4);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RITUAL_SCHEDULE = BUILDER
            .comment("Default daily ritual schedule as ID=HH:mm/minutes (24-hour clock in the configured time zone).",
                    "Temples you register in game can override this with /keralatemple schedule.",
                    "These are typical defaults. Real temples keep their own timings, so adjust to the temple you are modelling.",
                    "Known IDs: NADA_THURAKKAL, NIRMALYA_DARSHANAM, ABHISHEKAM, USHAH_POOJA, ETHIRTHA_POOJA, SEEVELI_MORNING,",
                    "PANTHEERADI_POOJA, UCHA_POOJA, NADA_ADAKKAL, EVENING_NADA_THURAKKAL, DEEPARADHANA, ATHAZHA_POOJA,",
                    "SEEVELI_NIGHT, NADA_ADAKKAL_NIGHT, EXTRA_POOJA_1, EXTRA_POOJA_2, EXTRA_POOJA_3, EXTRA_SEEVELI")
            .defineList("ritualSchedule", RitualSchedule.DEFAULT_SCHEDULE, o -> o instanceof String);

    public static final ForgeConfigSpec.IntValue ACHARI_CAST_SECONDS = BUILDER
            .comment("How many seconds the achari takes to cast a 2-block-tall idol (taller idols take longer).")
            .defineInRange("achariCastSeconds", 30, 1, 3600);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
