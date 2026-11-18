package com.example.keralatemple.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** The chests and barrels near a spot: where the cook and the kazhakam find ingredients and flowers. */
public final class Pantry {
    private static final int RADIUS = 8;

    private Pantry() {}

    private static List<Container> containers(Level level, BlockPos centre) {
        List<Container> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-RADIUS, -3, -RADIUS), centre.offset(RADIUS, 3, RADIUS))) {
            if (level.isLoaded(p) && level.getBlockEntity(p) instanceof Container c) {
                found.add(c);
            }
        }
        return found;
    }

    private static int count(List<Container> list, Item item) {
        int n = 0;
        for (Container c : list) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                ItemStack s = c.getItem(i);
                if (s.is(item)) {
                    n += s.getCount();
                }
            }
        }
        return n;
    }

    private static void remove(List<Container> list, Item item, int amount) {
        int left = amount;
        for (Container c : list) {
            for (int i = 0; i < c.getContainerSize() && left > 0; i++) {
                ItemStack s = c.getItem(i);
                if (s.is(item)) {
                    int used = Math.min(left, s.getCount());
                    s.shrink(used);
                    left -= used;
                    c.setChanged();
                }
            }
        }
    }

    /** What is still missing, as text ("2 Ney, 1 Coconut"); empty when everything is there. */
    public static String missing(Level level, BlockPos centre, Map<Item, Integer> needs) {
        List<Container> list = containers(level, centre);
        StringJoiner j = new StringJoiner(", ");
        for (Map.Entry<Item, Integer> e : needs.entrySet()) {
            int lack = e.getValue() - count(list, e.getKey());
            if (lack > 0) {
                j.add(lack + " " + new ItemStack(e.getKey()).getHoverName().getString());
            }
        }
        return j.toString();
    }

    /** Takes everything if it is all there; otherwise takes nothing and returns false. */
    public static boolean takeAll(Level level, BlockPos centre, Map<Item, Integer> needs) {
        List<Container> list = containers(level, centre);
        for (Map.Entry<Item, Integer> e : needs.entrySet()) {
            if (count(list, e.getKey()) < e.getValue()) {
                return false;
            }
        }
        for (Map.Entry<Item, Integer> e : needs.entrySet()) {
            remove(list, e.getKey(), e.getValue());
        }
        return true;
    }

    public static boolean takeOne(Level level, BlockPos centre, Item item) {
        List<Container> list = containers(level, centre);
        if (count(list, item) < 1) {
            return false;
        }
        remove(list, item, 1);
        return true;
    }
}
