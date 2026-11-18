package com.example.keralatemple.idol;

import com.example.keralatemple.config.Config;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

/** What the achari asks for his work. Creative players, and servers that switch it off in the config, pay nothing. */
public final class AchariCosts {
    private AchariCosts() {}

    /** Vanilla has no silver, so iron stands in; bronze and panchaloha use copper. */
    public static Item ingot(IdolMaterial metal) {
        return switch (metal) {
            case GOLD -> Items.GOLD_INGOT;
            case SILVER -> Items.IRON_INGOT;
            default -> Items.COPPER_INGOT;
        };
    }

    /** Vanilla has no ruby either, so redstone stands in for it. */
    public static Item jewel(int jewel) {
        return switch (jewel) {
            case 1 -> Items.EMERALD;
            case 2 -> Items.REDSTONE;
            default -> Items.DIAMOND;
        };
    }

    public static Map<Item, Integer> forOrnament(OrnamentData d) {
        Map<Item, Integer> cost = new LinkedHashMap<>();
        cost.put(ingot(d.metal), d.type.ingots);
        for (int j = 1; j < OrnamentData.JEWEL_NAMES.length; j++) {
            int n = d.jewelCount(j);
            if (n > 0) {
                cost.merge(jewel(j), n, Integer::sum);
            }
        }
        return cost;
    }

    public static Map<Item, Integer> forAngi(IdolMaterial metal, float idolSize) {
        Map<Item, Integer> cost = new LinkedHashMap<>();
        cost.put(ingot(metal), Math.max(3, Math.min(18, (int) Math.ceil(idolSize * 3.0F))));
        return cost;
    }

    public static boolean isFree(Player player) {
        return player.getAbilities().instabuild || !Config.ACHARI_NEEDS_MATERIALS.get();
    }

    public static boolean has(Player player, Map<Item, Integer> cost) {
        for (Map.Entry<Item, Integer> e : cost.entrySet()) {
            if (count(player, e.getKey()) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static void take(Player player, Map<Item, Integer> cost) {
        for (Map.Entry<Item, Integer> e : cost.entrySet()) {
            int left = e.getValue();
            for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (s.is(e.getKey())) {
                    int used = Math.min(left, s.getCount());
                    s.shrink(used);
                    left -= used;
                }
            }
        }
    }

    private static int count(Player player, Item item) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    public static String describe(Map<Item, Integer> cost) {
        StringJoiner j = new StringJoiner(", ");
        for (Map.Entry<Item, Integer> e : cost.entrySet()) {
            j.add(e.getValue() + " " + new ItemStack(e.getKey()).getHoverName().getString());
        }
        return j.toString();
    }
}
