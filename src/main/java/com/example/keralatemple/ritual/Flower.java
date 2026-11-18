package com.example.keralatemple.ritual;

import com.example.keralatemple.registry.ModItems;
import net.minecraft.world.item.Item;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** The flowers and leaves offered in pooja. Each temple chooses which of them it uses. */
public enum Flower {
    TULASI("tulasi", "Tulasi", 0x3F7F2A, () -> ModItems.TULASI.get()),
    THECHI("thechi", "Thechi", 0xD9202A, () -> ModItems.THECHI.get()),
    LOTUS("lotus", "Lotus", 0xF5A0C8, () -> ModItems.LOTUS.get()),
    VILWA("vilwa", "Vilwa", 0x2F6B3A, () -> ModItems.VILWA.get()),
    CHEMBARATHI("chembarathi", "Chembarathi", 0xC21E56, () -> ModItems.CHEMBARATHI.get()),
    MULLA("mulla", "Mulla", 0xFFFFFF, () -> ModItems.MULLA.get());

    public final String id;
    public final String label;
    public final int colour;
    private final Supplier<Item> item;

    Flower(String id, String label, int colour, Supplier<Item> item) {
        this.id = id;
        this.label = label;
        this.colour = colour;
        this.item = item;
    }

    public Item item() {
        return this.item.get();
    }

    @Nullable
    public static Flower byId(String id) {
        for (Flower f : values()) {
            if (f.id.equals(id)) {
                return f;
            }
        }
        return null;
    }

    @Nullable
    public static Flower byItem(Item item) {
        for (Flower f : values()) {
            if (f.item() == item) {
                return f;
            }
        }
        return null;
    }

    /** Index + 1, stored in a garland (0 means nothing). */
    public byte code() {
        return (byte) (ordinal() + 1);
    }

    @Nullable
    public static Flower byCode(int code) {
        Flower[] all = values();
        return code >= 1 && code <= all.length ? all[code - 1] : null;
    }
}
