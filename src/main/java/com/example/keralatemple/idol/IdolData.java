package com.example.keralatemple.idol;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Everything that describes one idol: a front-view picture (RGBA, at most 64x64, transparent = empty) that is
 * inflated into a 3D relief, plus its size, thickness, peetham style and material.
 * Immutable. The same object travels in packets, is saved in block entities and items, and is drawn by the client.
 */
public final class IdolData {
    public static final int MAX_CELLS = 64;
    public static final int MIN_CELLS = 4;
    public static final float MIN_SIZE = 0.5F;
    public static final float MAX_SIZE = 6.0F;
    public static final float MIN_THICKNESS = 0.4F;
    public static final float MAX_THICKNESS = 2.0F;
    public static final int PEETHAM_STYLES = 5;
    public static final float MIN_PEETHAM_SIZE = 0.5F;
    public static final float MAX_PEETHAM_SIZE = 3.0F;
    private static final float[] PEETHAM_HEIGHTS = {0.0F, 0.15F, 0.30F, 0.40F, 0.30F};

    public final int width;
    public final int height;
    /** width * height * 4 bytes: r, g, b, a for each cell, row by row from the top. */
    public final byte[] rgba;
    /** Height of the idol in blocks. */
    public final float size;
    public final float thickness;
    public final int peetham;
    /** Scales the peetham's width and height together. */
    public final float peethamSize;
    /** Which stone the peetham is cut from: 0 Anjana Shila, 1 Krishna Shila. */
    public final int peethamStone;
    /** true = show the picture's own colours; false = show the material (carved stone or cast metal). */
    public final boolean originalColours;
    public final IdolMaterial material;

    public IdolData(int width, int height, byte[] rgba, float size, float thickness, int peetham,
                    boolean originalColours, IdolMaterial material) {
        this(width, height, rgba, size, thickness, peetham, 1.0F, originalColours, material);
    }

    public IdolData(int width, int height, byte[] rgba, float size, float thickness, int peetham,
                    float peethamSize, boolean originalColours, IdolMaterial material) {
        this(width, height, rgba, size, thickness, peetham, peethamSize, 0, originalColours, material);
    }

    public IdolData(int width, int height, byte[] rgba, float size, float thickness, int peetham,
                    float peethamSize, int peethamStone, boolean originalColours, IdolMaterial material) {
        this.peethamStone = peethamStone == 1 ? 1 : 0;
        this.peethamSize = peethamSize;
        this.width = width;
        this.height = height;
        this.rgba = rgba;
        this.size = size;
        this.thickness = thickness;
        this.peetham = peetham;
        this.originalColours = originalColours;
        this.material = material;
    }

    public IdolData withMaterial(IdolMaterial newMaterial) {
        return new IdolData(width, height, rgba, size, thickness, peetham, peethamSize, peethamStone, originalColours, newMaterial);
    }

    public float peethamHeight() {
        return PEETHAM_HEIGHTS[Math.max(0, Math.min(PEETHAM_STYLES - 1, peetham))] * peethamSize;
    }

    /** Checks everything, because this also arrives from clients over the network. */
    public boolean isValid() {
        if (width < MIN_CELLS || width > MAX_CELLS || height < MIN_CELLS || height > MAX_CELLS) {
            return false;
        }
        if (rgba == null || rgba.length != width * height * 4) {
            return false;
        }
        if (!(size >= MIN_SIZE && size <= MAX_SIZE) || !(thickness >= MIN_THICKNESS && thickness <= MAX_THICKNESS)) {
            return false;
        }
        if (!(peethamSize >= MIN_PEETHAM_SIZE && peethamSize <= MAX_PEETHAM_SIZE)) {
            return false;
        }
        if (peetham < 0 || peetham >= PEETHAM_STYLES || material == null) {
            return false;
        }
        for (int i = 3; i < rgba.length; i += 4) {
            if ((rgba[i] & 255) >= 128) {
                return true;   // at least one filled cell
            }
        }
        return false;
    }

    /** A cheap key for the client's model cache. */
    public long contentKey() {
        long k = Arrays.hashCode(rgba);
        k = k * 31 + width;
        k = k * 31 + height;
        k = k * 31 + Float.floatToIntBits(thickness);
        k = k * 31 + material.ordinal();
        k = k * 31 + (originalColours ? 1 : 0);
        return k;
    }

    // ------------------------------------------------------------------ saving

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("W", width);
        tag.putInt("H", height);
        tag.putByteArray("Rgba", rgba);
        tag.putFloat("Size", size);
        tag.putFloat("Thickness", thickness);
        tag.putInt("Peetham", peetham);
        tag.putFloat("PeethamSize", peethamSize);
        tag.putInt("PeethamStone", peethamStone);
        tag.putBoolean("Original", originalColours);
        tag.putInt("Material", material.ordinal());
        return tag;
    }

    /** Returns null if the saved data is damaged. */
    @Nullable
    public static IdolData load(CompoundTag tag) {
        IdolData data = new IdolData(tag.getInt("W"), tag.getInt("H"), tag.getByteArray("Rgba"),
                tag.getFloat("Size"), tag.getFloat("Thickness"), tag.getInt("Peetham"),
                tag.contains("PeethamSize") ? tag.getFloat("PeethamSize") : 1.0F, tag.getInt("PeethamStone"),
                tag.getBoolean("Original"), IdolMaterial.byId(tag.getInt("Material")));
        return data.isValid() ? data : null;
    }

    // ------------------------------------------------------------------ network

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(width);
        buf.writeVarInt(height);
        buf.writeByteArray(rgba);
        buf.writeFloat(size);
        buf.writeFloat(thickness);
        buf.writeVarInt(peetham);
        buf.writeFloat(peethamSize);
        buf.writeVarInt(peethamStone);
        buf.writeBoolean(originalColours);
        buf.writeVarInt(material.ordinal());
    }

    public static IdolData read(FriendlyByteBuf buf) {
        int w = buf.readVarInt();
        int h = buf.readVarInt();
        byte[] rgba = buf.readByteArray(MAX_CELLS * MAX_CELLS * 4);
        float size = buf.readFloat();
        float thickness = buf.readFloat();
        int peetham = buf.readVarInt();
        float peethamSize = buf.readFloat();
        int peethamStone = buf.readVarInt();
        boolean original = buf.readBoolean();
        IdolMaterial material = IdolMaterial.byId(buf.readVarInt());
        return new IdolData(w, h, rgba, size, thickness, peetham, peethamSize, peethamStone, original, material);
    }
}
