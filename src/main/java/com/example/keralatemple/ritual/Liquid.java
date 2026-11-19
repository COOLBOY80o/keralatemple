package com.example.keralatemple.ritual;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The nine things poured over the idol at abhishekam. */
public enum Liquid {
    WATER("Water", 0x5E9BE6),
    MILK("Milk", 0xF8F8F2),
    YOGURT("Yogurt", 0xF2EBD6),
    GHEE("Ghee", 0xF0CD58),
    HONEY("Honey", 0xD7962A),
    KALABHAM("Kalabham (paste)", 0xD9B27A),
    PANCHAMRITAM("Panchamritam", 0x8B5A32),
    PANCHAGAVYAM("Panchagavyam", 0xD6CC96),
    TENDER_COCONUT("Tender coconut", 0xE6EFDC);

    public final String label;
    public final int rgb;

    Liquid(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Liquid byId(String id) {
        for (Liquid l : values()) {
            if (l.id().equals(id.toLowerCase(Locale.ROOT))) {
                return l;
            }
        }
        return null;
    }

    public static Liquid byOrdinal(int i) {
        Liquid[] all = values();
        return i >= 0 && i < all.length ? all[i] : WATER;
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Liquid l : values()) {
            out.add(l.id());
        }
        return out;
    }
}
