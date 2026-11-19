package com.example.keralatemple.temple;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import com.example.keralatemple.ritual.Flower;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

/** One temple: where it is, its own ritual schedule, and its bali stones. */
public class Temple {
    public final String name;
    /** The block the temple was marked on with the Temple Wand (or /keralatemple temple create). */
    public BlockPos center;
    public int radius = 24;
    /** Entries as "ID=HH:mm/minutes". Null means "use the default schedule from the config". */
    public List<String> schedule = null;
    /** Ayirakudam progress (not saved): pots delivered in the current ritual, and which ritual that was. */
    public int kudams = 0;
    public String kudamKey = "";
    /** Flowers this temple offers (ids from Flower). */
    public final Set<String> flowers = new LinkedHashSet<>();
    /** Ritual id -> nivedyam id: what the cook makes for which ritual. */
    public final Map<String, String> nivedyam = new LinkedHashMap<>();
    /** Progress of today's cooking and garland (not saved). */
    public String cookKey = "";
    public int cookStage = 0;
    public String cookMissing = "";
    public String garlandKey = "";
    public final List<BlockPos> baliStones = new ArrayList<>();
    /** The sreeveli route the player marked, in walking order. Empty = automatic clockwise route. */
    /** Angavastram choice per role (melshanthi, keezhshanthi, kazhakam, cook): "base,accent,design". */
    public final Map<String, String> vastram = new java.util.HashMap<>();
    /** The order of liquids poured at the Abhishekam pooja (Liquid ids). Empty means all nine in their usual order. */
    public final java.util.List<String> abhishekam = new java.util.ArrayList<>();

    /** The liquids to pour, in order. */
    public java.util.List<com.example.keralatemple.ritual.Liquid> abhishekamOrder() {
        java.util.List<com.example.keralatemple.ritual.Liquid> out = new java.util.ArrayList<>();
        for (String id : abhishekam) {
            com.example.keralatemple.ritual.Liquid l = com.example.keralatemple.ritual.Liquid.byId(id);
            if (l != null) {
                out.add(l);
            }
        }
        if (out.isEmpty()) {
            out.addAll(java.util.Arrays.asList(com.example.keralatemple.ritual.Liquid.values()));
        }
        return out;
    }
    /** How the keezhshanthi takes the idol out for the seeveli: arms, head or none. */
    public String carry = "head";
    /** Places the player marked as wells (the ayirakudam water is drawn here). */
    public final List<BlockPos> wells = new ArrayList<>();
    public final List<BlockPos> route = new ArrayList<>();
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
        for (Flower f : Flower.values()) {
            this.flowers.add(f.id);
        }
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
        ListTag flowerList = new ListTag();
        for (String f : flowers) {
            flowerList.add(StringTag.valueOf(f));
        }
        tag.put("Flowers", flowerList);
        CompoundTag nived = new CompoundTag();
        for (Map.Entry<String, String> e : nivedyam.entrySet()) {
            nived.putString(e.getKey(), e.getValue());
        }
        tag.put("Nivedyam", nived);
        CompoundTag vast = new CompoundTag();
        for (Map.Entry<String, String> e : vastram.entrySet()) {
            vast.putString(e.getKey(), e.getValue());
        }
        tag.put("Vastram", vast);
        net.minecraft.nbt.ListTag abh = new net.minecraft.nbt.ListTag();
        for (String a : abhishekam) {
            abh.add(net.minecraft.nbt.StringTag.valueOf(a));
        }
        tag.put("Abhishekam", abh);
        tag.putString("Carry", carry);
        tag.putLongArray("Wells", wells.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("Bali", baliStones.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("Route", route.stream().mapToLong(BlockPos::asLong).toArray());
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
        if (tag.contains("Flowers", Tag.TAG_LIST)) {
            t.flowers.clear();
            ListTag fl = tag.getList("Flowers", Tag.TAG_STRING);
            for (int i = 0; i < fl.size(); i++) {
                t.flowers.add(fl.getString(i));
            }
        }
        CompoundTag nived = tag.getCompound("Nivedyam");
        for (String key : nived.getAllKeys()) {
            t.nivedyam.put(key, nived.getString(key));
        }
        for (long l : tag.getLongArray("Wells")) {
            t.wells.add(BlockPos.of(l));
        }
        if (tag.contains("Carry")) {
            t.carry = tag.getString("Carry");
        }
        net.minecraft.nbt.ListTag abhTag = tag.getList("Abhishekam", 8);
        for (int i = 0; i < abhTag.size(); i++) {
            t.abhishekam.add(abhTag.getString(i));
        }
        CompoundTag vastTag = tag.getCompound("Vastram");
        for (String key : vastTag.getAllKeys()) {
            t.vastram.put(key, vastTag.getString(key));
        }
        for (long l : tag.getLongArray("Bali")) {
            t.baliStones.add(BlockPos.of(l));
        }
        for (long l : tag.getLongArray("Route")) {
            t.route.add(BlockPos.of(l));
        }
        ListTag valiyaList = tag.getList("Valiya", Tag.TAG_COMPOUND);
        for (int i = 0; i < valiyaList.size(); i++) {
            CompoundTag vt = valiyaList.getCompound(i);
            t.valiya.add(new Valiya(vt.getString("Name"), BlockPos.of(vt.getLong("Min")), BlockPos.of(vt.getLong("Max"))));
        }
        return t;
    }
}
