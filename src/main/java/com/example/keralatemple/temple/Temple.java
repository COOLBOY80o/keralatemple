package com.example.keralatemple.temple;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/** One temple: where it is, its own ritual schedule, and its bali stones. */
public class Temple {
    public final String name;
    /** The block the temple was marked on with the Temple Wand (or /keralatemple temple create). */
    public BlockPos center;
    public int radius = 24;
    /** Entries as "ID=HH:mm/minutes". Null means "use the default schedule from the config". */
    public List<String> schedule = null;
    public final List<BlockPos> baliStones = new ArrayList<>();
    public final List<Valiya> valiya = new ArrayList<>();

    /** A custom valiya balikkal: the player-built region that holds it. */
    public record Valiya(String name, BlockPos min, BlockPos max) {
        public BlockPos topCenter() {
            return new BlockPos((min.getX() + max.getX()) / 2, max.getY(), (min.getZ() + max.getZ()) / 2);
        }
    }

    public Temple(String name, BlockPos center) {
        this.name = name;
        this.center = center;
    }

    public double horizontalDistSq(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putLong("Center", center.asLong());
        tag.putInt("Radius", radius);
        if (schedule != null) {
            ListTag list = new ListTag();
            for (String entry : schedule) {
                list.add(StringTag.valueOf(entry));
            }
            tag.put("Schedule", list);
        }
        tag.putLongArray("Bali", baliStones.stream().mapToLong(BlockPos::asLong).toArray());
        ListTag valiyaList = new ListTag();
        for (Valiya v : valiya) {
            CompoundTag vt = new CompoundTag();
            vt.putString("Name", v.name());
            vt.putLong("Min", v.min().asLong());
            vt.putLong("Max", v.max().asLong());
            valiyaList.add(vt);
        }
        tag.put("Valiya", valiyaList);
        return tag;
    }

    public static Temple load(CompoundTag tag) {
        Temple t = new Temple(tag.getString("Name"), BlockPos.of(tag.getLong("Center")));
        t.radius = tag.contains("Radius") ? tag.getInt("Radius") : 24;
        if (tag.contains("Schedule", Tag.TAG_LIST)) {
            List<String> list = new ArrayList<>();
            ListTag raw = tag.getList("Schedule", Tag.TAG_STRING);
            for (int i = 0; i < raw.size(); i++) {
                list.add(raw.getString(i));
            }
            t.schedule = list;
        }
        for (long l : tag.getLongArray("Bali")) {
            t.baliStones.add(BlockPos.of(l));
        }
        ListTag valiyaList = tag.getList("Valiya", Tag.TAG_COMPOUND);
        for (int i = 0; i < valiyaList.size(); i++) {
            CompoundTag vt = valiyaList.getCompound(i);
            t.valiya.add(new Valiya(vt.getString("Name"), BlockPos.of(vt.getLong("Min")), BlockPos.of(vt.getLong("Max"))));
        }
        return t;
    }
}
