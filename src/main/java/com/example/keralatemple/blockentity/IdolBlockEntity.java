package com.example.keralatemple.blockentity;

import com.example.keralatemple.idol.IdolData;
import com.example.keralatemple.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.data != null) {
            tag.put("Idol", this.data.save());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.data = tag.contains("Idol", 10) ? IdolData.load(tag.getCompound("Idol")) : null;
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
