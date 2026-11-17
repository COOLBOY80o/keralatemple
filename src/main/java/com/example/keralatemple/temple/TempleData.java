package com.example.keralatemple.temple;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All temples of one dimension, saved with the world. */
public class TempleData extends SavedData {
    private static final String NAME = "keralatemple_temples";

    private final Map<String, Temple> temples = new LinkedHashMap<>();

    public static TempleData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TempleData::load, TempleData::new, NAME);
    }

    public static TempleData load(CompoundTag tag) {
        TempleData data = new TempleData();
        ListTag list = tag.getList("Temples", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Temple t = Temple.load(list.getCompound(i));
            data.temples.put(t.name, t);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Temple t : temples.values()) {
            list.add(t.save());
        }
        tag.put("Temples", list);
        return tag;
    }

    public Temple get(String name) {
        return temples.get(name);
    }

    public Collection<Temple> all() {
        return temples.values();
    }

    public List<String> names() {
        return new ArrayList<>(temples.keySet());
    }

    public Temple create(String name, BlockPos center) {
        Temple t = new Temple(name, center);
        temples.put(name, t);
        setDirty();
        return t;
    }

    public boolean remove(String name) {
        boolean removed = temples.remove(name) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public String nextName() {
        int i = temples.size() + 1;
        while (temples.containsKey("temple_" + i)) {
            i++;
        }
        return "temple_" + i;
    }

    /** The nearest temple whose radius contains this position (horizontally), or null. */
    public Temple findFor(BlockPos pos) {
        Temple best = null;
        double bestDist = Double.MAX_VALUE;
        for (Temple t : temples.values()) {
            double d = t.horizontalDistSq(pos);
            if (d <= (double) t.radius * t.radius && d < bestDist) {
                best = t;
                bestDist = d;
            }
        }
        return best;
    }

    /** Call after changing a Temple's fields. */
    public void touch() {
        setDirty();
    }
}
