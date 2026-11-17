package com.example.keralatemple.blockentity;

import com.example.keralatemple.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Remembers which photo this idol shows and how tall it is. The photo itself is read on each player's own
 * computer (keralatemple_idols folder), so only the name and height are saved and synced.
 */
public class IdolBlockEntity extends BlockEntity {
    private String image = "";
    private float height = 2.0F;

    public IdolBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IDOL.get(), pos, state);
    }

    public String getImage() {
        return this.image;
    }

    public float getHeight() {
        return this.height;
    }

    public void setIdol(String image, float height) {
        this.image = image;
        this.height = height;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Image", this.image);
        tag.putFloat("Height", this.height);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.image = tag.getString("Image");
        this.height = tag.contains("Height") ? tag.getFloat("Height") : 2.0F;
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
