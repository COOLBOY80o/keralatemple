package com.example.keralatemple.ritual;

import java.util.List;
import java.util.Locale;

/**
 * The angavastram: the cloth worn like a rope across the chest, from the right hip up to the left shoulder and
 * round the back. A temple picks its colours and design per role so you can tell the brahmins' positions apart.
 * Packed into one int for syncing: 0 = none, else 1 + base + 8 * (accent + 8 * design).
 */
public final class Vastram {
    public static final List<String> COLOURS = List.of("white", "cream", "saffron", "red", "maroon", "green", "blue", "gold");
    public static final int[] RGB = {0xE8E4DC, 0xD8C9A0, 0xE08A1E, 0xB01818, 0x6A1420, 0x2E7D32, 0x2A4FA0, 0xE6B93C};
    public static final List<String> DESIGNS = List.of("plain", "border", "stripes", "checks");
    public static final List<String> ROLES = List.of("melshanthi", "keezhshanthi", "kazhakam", "cook", "musician");

    private Vastram() {}

    public static int pack(int base, int accent, int design) {
        return 1 + base + 8 * (accent + 8 * design);
    }

    public static int base(int packed) {
        return (packed - 1) % 8;
    }

    public static int accent(int packed) {
        return ((packed - 1) / 8) % 8;
    }

    public static int design(int packed) {
        return Math.min(DESIGNS.size() - 1, (packed - 1) / 64);
    }

    /** Parses "base,accent,design" (as stored in a temple); returns 0 when it is not valid. */
    public static int parse(String spec) {
        if (spec == null) {
            return 0;
        }
        String[] p = spec.toLowerCase(Locale.ROOT).split(",");
        if (p.length != 3) {
            return 0;
        }
        int b = COLOURS.indexOf(p[0]);
        int a = COLOURS.indexOf(p[1]);
        int d = DESIGNS.indexOf(p[2]);
        return b < 0 || a < 0 || d < 0 ? 0 : pack(b, a, d);
    }

    public static String spec(String base, String accent, String design) {
        return base + "," + accent + "," + design;
    }

    /** What each role wears when the temple has not chosen. */
    public static int defaultFor(String role) {
        return switch (role) {
            case "melshanthi" -> pack(0, 7, 1);
            case "keezhshanthi" -> pack(0, 3, 1);
            case "kazhakam" -> pack(1, 5, 2);
            case "musician" -> pack(2, 7, 2);
            default -> pack(0, 6, 3);
        };
    }
}
