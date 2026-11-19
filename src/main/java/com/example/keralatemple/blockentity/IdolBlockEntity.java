package com.example.keralatemple.blockentity;

import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.idol.OrnamentData;
import com.example.keralatemple.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds the idol (picture, size, material...) of a shila stone or an idol pedestal. */
public class IdolBlockEntity extends BlockEntity {
    @Nullable
    private IdolData data;

    public IdolBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IDOL.get(), pos, state);
    }

    @Nullable
    public IdolData getData() {
        return this.data;
    }

    public void setData(@Nullable IdolData data) {
        this.data = data;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /** Flower codes (see Flower.code) of the garland the idol wears; empty = none. */
    private byte[] garland = new byte[0];
    @Nullable
    private IdolData angi;
    private boolean fixed;
    private int pourLiquid = -1;
    private int pourSource;   // 0 a kalasam above the idol, 1 a shankham (conch)
    private long pourStart;
    private long pourUntil;

    /** Starts pouring a liquid over the idol, or keeps the same liquid going longer. */
    public void startPour(com.example.keralatemple.ritual.Liquid liquid, int ticks) {
        startPour(liquid, ticks, 0);
    }

    public int pourSource() {
        return this.pourSource;
    }

    public void startPour(com.example.keralatemple.ritual.Liquid liquid, int ticks, int source) {
        this.pourSource = source;
        if (this.level == null) {
            return;
        }
        long now = this.level.getGameTime();
        if (this.pourLiquid == liquid.ordinal() && now < this.pourUntil + 25) {
            this.pourUntil = Math.max(this.pourUntil, now + ticks);
        } else {
            this.pourLiquid = liquid.ordinal();
            this.pourStart = now;
            this.pourUntil = now + ticks;
        }
        sync();
    }

    /** The liquid on the idol right now (still running down for a moment after the pour), or null. */
    @Nullable
    public com.example.keralatemple.ritual.Liquid liquidAt(long now) {
        if (this.pourLiquid < 0 || now > this.pourUntil + 30) {
            return null;
        }
        return com.example.keralatemple.ritual.Liquid.byOrdinal(this.pourLiquid);
    }

    public boolean isPouring(long now) {
        return this.pourLiquid >= 0 && now < this.pourUntil;
    }

    public long pourStart() {
        return this.pourStart;
    }

    public long pourUntil() {
        return this.pourUntil;
    }

    public boolean isFixed() {
        return this.fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
        sync();
    }
    private final List<OrnamentData> ornaments = new ArrayList<>();

    public byte[] getGarland() {
        return this.garland;
    }

    public void setGarland(byte[] garland) {
        this.garland = garland;
        sync();
    }

    /** A visitor adds one flower to the garland (at most 12). Returns false if it is full. */
    public boolean addFlower(byte code) {
        if (this.garland.length >= 12) {
            return false;
        }
        byte[] bigger = java.util.Arrays.copyOf(this.garland, this.garland.length + 1);
        bigger[this.garland.length] = code;
        setGarland(bigger);
        return true;
    }

    @Nullable
    public IdolData getAngi() {
        return this.angi;
    }

    public void setAngi(@Nullable IdolData angi) {
        this.angi = angi;
        sync();
    }

    public List<OrnamentData> getOrnaments() {
        return Collections.unmodifiableList(this.ornaments);
    }

    /** Puts an ornament on; one of each kind at a time. Returns the one it replaced, if any. */
    @Nullable
    public OrnamentData addOrnament(OrnamentData ornament) {
        OrnamentData replaced = null;
        for (int i = 0; i < this.ornaments.size(); i++) {
            if (this.ornaments.get(i).type == ornament.type) {
                replaced = this.ornaments.remove(i);
                break;
            }
        }
        this.ornaments.add(ornament);
        sync();
        return replaced;
    }

    @Nullable
    public OrnamentData removeLastOrnament() {
        if (this.ornaments.isEmpty()) {
            return null;
        }
        OrnamentData o = this.ornaments.remove(this.ornaments.size() - 1);
        sync();
        return o;
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.data != null) {
            tag.put("Idol", this.data.save());
        }
        if (this.angi != null) {
            tag.put("Angi", this.angi.save());
        }
        if (this.garland.length > 0) {
            tag.putByteArray("Garland", this.garland);
        }
        ListTag list = new ListTag();
        for (OrnamentData o : this.ornaments) {
            list.add(o.save());
        }
        tag.put("Ornaments", list);
        tag.putBoolean("Fixed", this.fixed);
        tag.putInt("PourLiquid", this.pourLiquid);
        tag.putInt("PourSource", this.pourSource);
        tag.putLong("PourStart", this.pourStart);
        tag.putLong("PourUntil", this.pourUntil);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.data = tag.contains("Idol", 10) ? IdolData.load(tag.getCompound("Idol")) : null;
        this.angi = tag.contains("Angi", 10) ? IdolData.load(tag.getCompound("Angi")) : null;
        this.garland = tag.getByteArray("Garland");
        this.fixed = tag.getBoolean("Fixed");
        this.pourLiquid = tag.contains("PourLiquid") ? tag.getInt("PourLiquid") : -1;
        this.pourSource = tag.getInt("PourSource");
        this.pourStart = tag.getLong("PourStart");
        this.pourUntil = tag.getLong("PourUntil");
        this.ornaments.clear();
        ListTag list = tag.getList("Ornaments", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            OrnamentData o = OrnamentData.load(list.getCompound(i));
            if (o != null) {
                this.ornaments.add(o);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The idol is drawn well above and beside its block, so widen the box that decides when it is rendered. */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.worldPosition.offset(-5, 0, -5), this.worldPosition.offset(6, 10, 6));
    }
}
