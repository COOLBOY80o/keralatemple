package com.example.keralatemple.ritual;

import com.example.keralatemple.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Food offered to the deity (nivedyam), and what the cook needs to make it. */
public enum Nivedyam {
    NEY_PAYASAM("ney_payasam", "Ney Payasam",
            new Ing(() -> ModItems.RAW_RICE.get(), 2), new Ing(() -> ModItems.NEY.get(), 2),
            new Ing(() -> ModItems.JAGGERY.get(), 2), new Ing(() -> ModItems.COCONUT.get(), 1)),
    MALAR("malar", "Malar Nivedyam (puffed rice)",
            new Ing(() -> ModItems.RAW_RICE.get(), 3), new Ing(() -> ModItems.JAGGERY.get(), 1)),
    THRIMADHURAM("thrimadhuram", "Thrimadhuram",
            new Ing(() -> ModItems.BANANA.get(), 2), new Ing(() -> ModItems.JAGGERY.get(), 1),
            new Ing(() -> Items.HONEY_BOTTLE, 1)),
    KADUM_PAYASAM("kadum_payasam", "Kadum Payasam",
            new Ing(() -> ModItems.RAW_RICE.get(), 2), new Ing(() -> ModItems.JAGGERY.get(), 3),
            new Ing(() -> ModItems.COCONUT.get(), 1), new Ing(() -> ModItems.NEY.get(), 1));

    public record Ing(Supplier<Item> item, int count) {}

    public final String id;
    public final String label;
    private final Ing[] ingredients;

    Nivedyam(String id, String label, Ing... ingredients) {
        this.id = id;
        this.label = label;
        this.ingredients = ingredients;
    }

    public Map<Item, Integer> ingredients() {
        Map<Item, Integer> map = new LinkedHashMap<>();
        for (Ing i : ingredients) {
            map.merge(i.item().get(), i.count(), Integer::sum);
        }
        return map;
    }

    /** An item to throw as particles when it is offered. */
    public Item showItem() {
        return ingredients[0].item().get();
    }

    @Nullable
    public static Nivedyam byId(String id) {
        for (Nivedyam n : values()) {
            if (n.id.equals(id)) {
                return n;
            }
        }
        return null;
    }
}
