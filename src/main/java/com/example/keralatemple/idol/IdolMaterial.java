package com.example.keralatemple.idol;

import java.util.List;

/** What an idol is made of. The two shilas are carved in place; the metals are cast by the achari. */
public enum IdolMaterial {
    ANJANA_SHILA("Anjana Shila", false, 66, 66, 74),
    KRISHNA_SHILA("Krishna Shila", false, 58, 70, 88),
    PANCHALOHA("Panchaloha", true, 184, 134, 66),
    BRONZE("Bronze", true, 156, 104, 52),
    SILVER("Silver", true, 205, 208, 214),
    GOLD("Gold", true, 236, 190, 60);

    public static final List<IdolMaterial> METALS = List.of(PANCHALOHA, BRONZE, SILVER, GOLD);

    public final String label;
    public final boolean metal;
    /** Base colour used when the idol is shown in its material (not in the photo's own colours). */
    public final int r;
    public final int g;
    public final int b;

    IdolMaterial(String label, boolean metal, int r, int g, int b) {
        this.label = label;
        this.metal = metal;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static IdolMaterial byId(int id) {
        IdolMaterial[] all = values();
        return id >= 0 && id < all.length ? all[id] : ANJANA_SHILA;
    }
}
