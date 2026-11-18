package com.example.keralatemple.blockentity;

import com.example.keralatemple.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** The size and build of a bali stone: width, height, number of tiers, space between tiers and round or square. */
public class BaliStoneBlockEntity extends BlockEntity {
    public static final float MAX_SIZE = 3.0F;

    private float width = 0.40F;      // blocks
    private float height = 0.35F;     // blocks
    private int layers = 4;
    private float gap = 0.0F;         // blocks between tiers
    private boolean round = true;

    public BaliStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BALI_STONE.get(), pos, state);
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public int layers() {
        return layers;
    }

    public float gap() {
        return gap;
    }

    public boolean round() {
        return round;
    }

    public void set(float width, float height, int layers, float gap, boolean round) {
        this.width = Math.max(0.2F, Math.min(MAX_SIZE, width));
        this.height = Math.max(0.2F, Math.min(MAX_SIZE, height));
        this.layers = Math.max(1, Math.min(8, layers));
        this.gap = Math.max(0.0F, Math.min(0.3F, gap));
        this.round = round;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("Width", width);
        tag.putFloat("Height", height);
        tag.putInt("Layers", layers);
        tag.putFloat("Gap", gap);
        tag.putBoolean("Round", round);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Width")) {
            width = tag.getFloat("Width");
            height = tag.getFloat("Height");
            layers = Math.max(1, Math.min(8, tag.getInt("Layers")));
            gap = tag.getFloat("Gap");
            round = tag.getBoolean("Round");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        double r = width / 2.0D + 0.1D;
        return new AABB(worldPosition.getX() + 0.5D - r, worldPosition.getY(), worldPosition.getZ() + 0.5D - r,
                worldPosition.getX() + 0.5D + r, worldPosition.getY() + height + 0.1D, worldPosition.getZ() + 0.5D + r);
    }
}
