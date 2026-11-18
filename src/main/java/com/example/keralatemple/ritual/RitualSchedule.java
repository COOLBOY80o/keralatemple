package com.example.keralatemple.ritual;

import com.example.keralatemple.config.Config;
import com.example.keralatemple.temple.Temple;

import java.time.LocalDate;
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

    /** What the priest visibly does during a pooja. */
    public enum Effect {
        PLAIN, NIRMALYA, VAKACHARTHU, SHANKHA, JALADHARA, KSHEERADHARA, AYIRAKUDAM, DEEPARADHANA
    }

    public record Definition(String displayName, Kind kind, Effect effect) {}

    public record Ritual(String id, String displayName, Kind kind, Effect effect, LocalTime start, int minutes) {
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
        define("NIRMALYA_DARSHANAM", "Nirmalya Darshanam", Kind.POOJA, Effect.NIRMALYA);
        define("VAKACHARTHU", "Vakacharthu (sandal paste)", Kind.POOJA, Effect.VAKACHARTHU);
        define("SHANKHA_ABHISHEKAM", "Shankha Abhishekam", Kind.POOJA, Effect.SHANKHA);
        define("JALADHARA", "Jaladhara", Kind.POOJA, Effect.JALADHARA);
        define("KSHEERADHARA", "Ksheeradhara", Kind.POOJA, Effect.KSHEERADHARA);
        define("AYIRAKUDAM", "Ayirakudam Abhishekam (1000 pots)", Kind.POOJA, Effect.AYIRAKUDAM);
        define("ABHISHEKAM", "Abhishekam", Kind.POOJA);
        define("USHAH_POOJA", "Ushah Pooja", Kind.POOJA);
        define("ETHIRTHA_POOJA", "Ethirtha Pooja", Kind.POOJA);
        define("SEEVELI_MORNING", "Seeveli (morning)", Kind.SEEVELI);
        define("PANTHEERADI_POOJA", "Pantheeradi Pooja", Kind.POOJA);
        define("UCHA_POOJA", "Ucha Pooja", Kind.POOJA);
        define("NADA_ADAKKAL", "Nada Adakkal (temple closes)", Kind.CLOSE);
        define("EVENING_NADA_THURAKKAL", "Nada Thurakkal (evening opening)", Kind.OPEN);
        define("DEEPARADHANA", "Deeparadhana", Kind.POOJA, Effect.DEEPARADHANA);
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
            "NADA_THURAKKAL=04:00/10",
            "NIRMALYA_DARSHANAM=04:10/20",
            "VAKACHARTHU=04:30/10",
            "SHANKHA_ABHISHEKAM=04:40/10",
            "JALADHARA=04:50/10",
            "KSHEERADHARA=05:00/10",
            "USHAH_POOJA=06:30/30",
            "ETHIRTHA_POOJA=08:00/30",
            "SEEVELI_MORNING=08:30/30",
            "PANTHEERADI_POOJA=09:30/30",
            "UCHA_POOJA=11:30/45",
            "NADA_ADAKKAL=12:15/10",
            "EVENING_NADA_THURAKKAL=17:00/15",
            "DEEPARADHANA=SUNSET/20",
            "ATHAZHA_POOJA=19:30/30",
            "SEEVELI_NIGHT=20:00/30",
            "NADA_ADAKKAL_NIGHT=20:30/10"
    );

    private RitualSchedule() {}

    private static void define(String id, String displayName, Kind kind) {
        DEFINITIONS.put(id, new Definition(displayName, kind, Effect.PLAIN));
    }

    private static void define(String id, String displayName, Kind kind, Effect effect) {
        DEFINITIONS.put(id, new Definition(displayName, kind, effect));
    }

    /**
     * Sunset in Kerala (about 10 degrees north) for a date, approximated: about 18:05 in December, 18:49 in June.
     * It is close to the real sunset, not exact.
     */
    public static LocalTime sunset(LocalDate date) {
        double minutes = 18 * 60 + 27 + 22.0 * Math.sin(2.0 * Math.PI * (date.getDayOfYear() - 80) / 365.0);
        return LocalTime.of(0, 0).plusMinutes(Math.round(minutes));
    }

    /** Reads "HH:mm", "SUNSET", "SUNSET+10" or "SUNSET-15". */
    private static LocalTime parseTime(String spec) {
        String t = spec.trim().toUpperCase(Locale.ROOT);
        if (t.startsWith("SUNSET")) {
            String rest = t.substring(6).trim();
            int offset = rest.isEmpty() ? 0 : Integer.parseInt(rest.startsWith("+") ? rest.substring(1) : rest);
            return sunset(KeralaClock.now().toLocalDate()).plusMinutes(offset);
        }
        return LocalTime.parse(t);
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

    /** An entry that follows sunset, with an offset in minutes (negative = earlier). */
    public static String formatSunset(String id, int offsetMinutes, int minutes) {
        String off = offsetMinutes == 0 ? "" : (offsetMinutes > 0 ? "+" + offsetMinutes : String.valueOf(offsetMinutes));
        return String.format(Locale.ROOT, "%s=SUNSET%s/%d", id.toUpperCase(Locale.ROOT), off, minutes);
    }

    /** The saved time part of an entry, for showing to players ("SUNSET" or "18:30"). */
    public static String timeSpec(String entry) {
        try {
            return entry.split("=", 2)[1].split("/", 2)[0].trim().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "?";
        }
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
            LocalTime start = parseTime(timeAndMinutes[0]);
            int minutes = Integer.parseInt(timeAndMinutes[1].trim());
            return new Ritual(id, def.displayName(), def.kind(), def.effect(), start, Math.max(1, minutes));
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
