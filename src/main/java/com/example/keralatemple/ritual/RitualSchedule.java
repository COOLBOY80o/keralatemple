package com.example.keralatemple.ritual;

import com.example.keralatemple.config.Config;
import com.example.keralatemple.temple.Temple;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The daily ritual (nityapooja) schedule of a Kerala temple.
 *
 * Times are TYPICAL defaults only. Real temples differ, so the default schedule can be edited in the config
 * file, and each temple can have its own schedule edited in game with /keralatemple schedule.
 * Entries look like "ID=HH:mm/minutes".
 */
public final class RitualSchedule {

    public enum Kind {
        /** Priest opens the sanctum (nada thurakkal). */
        OPEN,
        /** Priest performs a pooja at the sanctum. */
        POOJA,
        /** Priest circles the temple with the procession (seeveli / sribali). */
        SEEVELI,
        /** Priest closes the sanctum (nada adakkal). */
        CLOSE
    }

    public record Definition(String displayName, Kind kind) {}

    public record Ritual(String id, String displayName, Kind kind, LocalTime start, int minutes) {
        public boolean isActive(LocalTime now) {
            int s = start.toSecondOfDay();
            int e = s + minutes * 60;
            int n = now.toSecondOfDay();
            if (e <= 86400) {
                return n >= s && n < e;
            }
            return n >= s || n < e - 86400;
        }
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        define("NADA_THURAKKAL", "Nada Thurakkal (temple opens)", Kind.OPEN);
        define("NIRMALYA_DARSHANAM", "Nirmalya Darshanam", Kind.POOJA);
        define("ABHISHEKAM", "Abhishekam", Kind.POOJA);
        define("USHAH_POOJA", "Ushah Pooja", Kind.POOJA);
        define("ETHIRTHA_POOJA", "Ethirtha Pooja", Kind.POOJA);
        define("SEEVELI_MORNING", "Seeveli (morning)", Kind.SEEVELI);
        define("PANTHEERADI_POOJA", "Pantheeradi Pooja", Kind.POOJA);
        define("UCHA_POOJA", "Ucha Pooja", Kind.POOJA);
        define("NADA_ADAKKAL", "Nada Adakkal (temple closes)", Kind.CLOSE);
        define("EVENING_NADA_THURAKKAL", "Nada Thurakkal (evening opening)", Kind.OPEN);
        define("DEEPARADHANA", "Deeparadhana", Kind.POOJA);
        define("ATHAZHA_POOJA", "Athazha Pooja", Kind.POOJA);
        define("SEEVELI_NIGHT", "Athazha Seeveli (night)", Kind.SEEVELI);
        define("NADA_ADAKKAL_NIGHT", "Nada Adakkal (night closing)", Kind.CLOSE);
        // Spare slots so a temple can add rituals of its own.
        define("EXTRA_POOJA_1", "Extra Pooja 1", Kind.POOJA);
        define("EXTRA_POOJA_2", "Extra Pooja 2", Kind.POOJA);
        define("EXTRA_POOJA_3", "Extra Pooja 3", Kind.POOJA);
        define("EXTRA_SEEVELI", "Extra Seeveli", Kind.SEEVELI);
    }

    public static final List<String> DEFAULT_SCHEDULE = List.of(
            "NADA_THURAKKAL=04:00/15",
            "NIRMALYA_DARSHANAM=04:30/30",
            "ABHISHEKAM=05:00/45",
            "USHAH_POOJA=06:30/30",
            "ETHIRTHA_POOJA=08:00/30",
            "SEEVELI_MORNING=08:30/30",
            "PANTHEERADI_POOJA=09:30/30",
            "UCHA_POOJA=11:30/45",
            "NADA_ADAKKAL=12:15/10",
            "EVENING_NADA_THURAKKAL=17:00/15",
            "DEEPARADHANA=18:30/20",
            "ATHAZHA_POOJA=19:30/30",
            "SEEVELI_NIGHT=20:00/30",
            "NADA_ADAKKAL_NIGHT=20:30/10"
    );

    private RitualSchedule() {}

    private static void define(String id, String displayName, Kind kind) {
        DEFINITIONS.put(id, new Definition(displayName, kind));
    }

    public static Set<String> ids() {
        return DEFINITIONS.keySet();
    }

    public static boolean isKnown(String id) {
        return DEFINITIONS.containsKey(id.toUpperCase(Locale.ROOT));
    }

    public static String format(String id, int hour, int minute, int minutes) {
        return String.format(Locale.ROOT, "%s=%02d:%02d/%d", id.toUpperCase(Locale.ROOT), hour, minute, minutes);
    }

    /** The default schedule from the config file, as editable strings. */
    public static List<String> configStrings() {
        return new ArrayList<String>(Config.RITUAL_SCHEDULE.get());
    }

    /** The strings in effect for a temple (its own schedule, or the config default). Always a fresh, editable list. */
    public static List<String> stringsFor(Temple temple) {
        if (temple != null && temple.schedule != null) {
            return new ArrayList<>(temple.schedule);
        }
        return configStrings();
    }

    public static List<Ritual> forTemple(Temple temple) {
        return parseAll(stringsFor(temple));
    }

    /** Parses the entries, ignoring any that are malformed, sorted by start time. */
    public static List<Ritual> parseAll(List<? extends String> raw) {
        List<Ritual> out = new ArrayList<>();
        for (String entry : raw) {
            Ritual ritual = parse(entry);
            if (ritual != null) {
                out.add(ritual);
            }
        }
        out.sort(Comparator.comparing(Ritual::start));
        return out;
    }

    private static Ritual parse(String entry) {
        try {
            String[] idAndRest = entry.split("=", 2);
            String id = idAndRest[0].trim().toUpperCase(Locale.ROOT);
            Definition def = DEFINITIONS.get(id);
            if (def == null) {
                return null;
            }
            String[] timeAndMinutes = idAndRest[1].split("/", 2);
            LocalTime start = LocalTime.parse(timeAndMinutes[0].trim());
            int minutes = Integer.parseInt(timeAndMinutes[1].trim());
            return new Ritual(id, def.displayName(), def.kind(), start, Math.max(1, minutes));
        } catch (Exception e) {
            return null;
        }
    }

    /** The ritual in progress at this time, or null. If rituals overlap, the one that started last wins. */
    public static Ritual current(List<Ritual> all, LocalTime now) {
        Ritual found = null;
        for (Ritual ritual : all) {
            if (ritual.isActive(now)) {
                found = ritual;
            }
        }
        return found;
    }

    /** The next ritual that starts after this time (wrapping to tomorrow), or null if the schedule is empty. */
    public static Ritual next(List<Ritual> all, LocalTime now) {
        for (Ritual ritual : all) {
            if (ritual.start().isAfter(now)) {
                return ritual;
            }
        }
        return all.isEmpty() ? null : all.get(0);
    }
}
