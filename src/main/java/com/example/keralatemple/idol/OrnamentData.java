package com.example.keralatemple.idol;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * One piece of jewellery for an idol: its kind, its metal (gold or silver) and what jewel sits in each of its
 * five settings. Immutable.
 */
public final class OrnamentData {
    public static final int SLOTS = 5;
    /** 0 = empty setting. */
    public static final String[] JEWEL_NAMES = {"No jewel", "Emerald", "Ruby", "Diamond"};
    public static final int[] JEWEL_COLOURS = {0x000000, 0x17DD62, 0xE0115F, 0xBDF4FF};

    public enum Type {
        CROWN("Kireedam", "crown", 4),
        NECKLACE("Mala", "necklace", 3),
        ARMLETS("Vala", "armlets", 4),
        BELT("Odyanam", "waist belt", 3);

        public final String label;
        public final String plain;
        /** Ingots of metal it takes. */
        public final int ingots;

        Type(String label, String plain, int ingots) {
            this.label = label;
            this.plain = plain;
            this.ingots = ingots;
        }

        public static Type byId(int id) {
            Type[] all = values();
            return id >= 0 && id < all.length ? all[id] : CROWN;
        }
    }

    public final Type type;
    public final IdolMaterial metal;
    public final byte[] jewels;

    public OrnamentData(Type type, IdolMaterial metal, byte[] jewels) {
        this.type = type;
        this.metal = metal;
        this.jewels = jewels;
    }

    public boolean isValid() {
        if (type == null || jewels == null || jewels.length != SLOTS) {
            return false;
        }
        if (metal != IdolMaterial.GOLD && metal != IdolMaterial.SILVER) {
            return false;
        }
        for (byte j : jewels) {
            if (j < 0 || j >= JEWEL_NAMES.length) {
                return false;
            }
        }
        return true;
    }

    public int jewelCount(int jewel) {
        int n = 0;
        for (byte j : jewels) {
            if (j == jewel) {
                n++;
            }
        }
        return n;
    }

    public String displayName() {
        return metal.label + " " + type.label;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Type", type.ordinal());
        tag.putInt("Metal", metal.ordinal());
        tag.putByteArray("Jewels", jewels);
        return tag;
    }

    @Nullable
    public static OrnamentData load(CompoundTag tag) {
        OrnamentData d = new OrnamentData(Type.byId(tag.getInt("Type")), IdolMaterial.byId(tag.getInt("Metal")),
                tag.getByteArray("Jewels"));
        return d.isValid() ? d : null;
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        buf.writeVarInt(metal.ordinal());
        buf.writeByteArray(jewels);
    }

    public static OrnamentData read(FriendlyByteBuf buf) {
        Type t = Type.byId(buf.readVarInt());
        IdolMaterial m = IdolMaterial.byId(buf.readVarInt());
        return new OrnamentData(t, m, buf.readByteArray(SLOTS));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OrnamentData d && d.type == type && d.metal == metal && Arrays.equals(d.jewels, jewels);
    }

    @Override
    public int hashCode() {
        return type.ordinal() * 31 + metal.ordinal() + Arrays.hashCode(jewels);
    }
}
