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

    @Nullable
    private IdolData angi;
    private final List<OrnamentData> ornaments = new ArrayList<>();

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
        ListTag list = new ListTag();
        for (OrnamentData o : this.ornaments) {
            list.add(o.save());
        }
        tag.put("Ornaments", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.data = tag.contains("Idol", 10) ? IdolData.load(tag.getCompound("Idol")) : null;
        this.angi = tag.contains("Angi", 10) ? IdolData.load(tag.getCompound("Angi")) : null;
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
