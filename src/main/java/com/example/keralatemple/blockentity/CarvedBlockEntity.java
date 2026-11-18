package com.example.keralatemple.blockentity;

import com.example.keralatemple.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/** A block cut into 8 x 8 x 8 small cubes. Each cube is either there or carved away. */
public class CarvedBlockEntity extends BlockEntity {
    public static final int N = 8;
    public static final int MATERIALS = 4;   // granite, laterite, wood, gold

    private final long[] layers = new long[N];   // one 8x8 layer per y; bit z*8+x
    private int material;
    private VoxelShape shape;

    public CarvedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARVED.get(), pos, state);
        fill();
    }

    public boolean has(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= N || y >= N || z >= N) {
            return false;
        }
        return (layers[y] >> (z * N + x) & 1L) != 0L;
    }

    public void set(int x, int y, int z, boolean on) {
        long bit = 1L << (z * N + x);
        layers[y] = on ? layers[y] | bit : layers[y] & ~bit;
        changed();
    }

    public void fill() {
        java.util.Arrays.fill(layers, -1L);
        changed();
    }

    public boolean isEmpty() {
        for (long l : layers) {
            if (l != 0L) {
                return false;
            }
        }
        return true;
    }

    public int material() {
        return material;
    }

    public void setMaterial(int m) {
        this.material = Math.floorMod(m, MATERIALS);
        changed();
    }

    /** Kinds of ready-made shapes: 0 pranala (trough spout), 1 open channel, 2 round pipe, 3 curved spout, 4 tube with head. */
    public void applyPreset(int kind, Direction facing) {
        java.util.Arrays.fill(layers, 0L);
        for (int lz = 0; lz < N; lz++) {
            for (int ly = 0; ly < N; ly++) {
                for (int lx = 0; lx < N; lx++) {
                    if (!presetSolid(kind, lx, ly, lz)) {
                        continue;
                    }
                    int x;
                    int z;
                    switch (facing) {
                        case NORTH -> { x = N - 1 - lx; z = N - 1 - lz; }
                        case EAST -> { x = lz; z = N - 1 - lx; }
                        case WEST -> { x = N - 1 - lz; z = lx; }
                        default -> { x = lx; z = lz; }
                    }
                    layers[ly] |= 1L << (z * N + x);
                }
            }
        }
        changed();
    }

    /** In local terms: z runs from the wall (0) to the open end (7). */
    private static boolean presetSolid(int kind, int x, int y, int z) {
        boolean groove = (x == 3 || x == 4);
        switch (kind) {
            case 0 -> {
                boolean body = z <= 5 ? y <= 4 : (x >= 2 && x <= 5 && y >= 2 && y <= 4);
                return body && !(groove && y >= 3);
            }
            case 1 -> {
                return y <= 3 && !(groove && y >= 2);
            }
            case 3 -> {
                // Curved spout: out from the wall along the top, then turning down with a ring at the mouth.
                boolean arm = z <= 5 && x >= 2 && x <= 5 && y >= 5;
                boolean leg = z >= 5 && x >= 2 && x <= 5 && y >= 1;
                boolean ring = z >= 4 && x >= 1 && x <= 6 && y <= 1;
                boolean bore = x >= 3 && x <= 4 && z >= 5 && z <= 6;
                boolean channel = groove && y >= 6 && z <= 5;
                return (arm || leg || ring) && !bore && !channel;
            }
            case 4 -> {
                // Round tube with a heavier decorated head at the open end.
                double dx = x + 0.5D - 4.0D;
                double dy = y + 0.5D - 4.0D;
                double d = Math.sqrt(dx * dx + dy * dy);
                return z >= 6 ? d >= 1.2D && d <= 4.0D : d >= 1.6D && d <= 3.2D;
            }
            default -> {
                double dx = x + 0.5D - 4.0D;
                double dy = y + 0.5D - 4.0D;
                double d = Math.sqrt(dx * dx + dy * dy);
                return d >= 1.6D && d <= 3.8D;
            }
        }
    }

    /** The outline and collision: runs of cubes along x merged into boxes. */
    public VoxelShape shape() {
        if (shape == null) {
            VoxelShape s = Shapes.empty();
            for (int y = 0; y < N; y++) {
                for (int z = 0; z < N; z++) {
                    int x = 0;
                    while (x < N) {
                        if (!has(x, y, z)) {
                            x++;
                            continue;
                        }
                        int start = x;
                        while (x < N && has(x, y, z)) {
                            x++;
                        }
                        s = Shapes.or(s, Block.box(start * 2, y * 2, z * 2, x * 2, y * 2 + 2, z * 2 + 2));
                    }
                }
            }
            shape = s;
        }
        return shape;
    }

    private void changed() {
        shape = null;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLongArray("Layers", layers);
        tag.putInt("Material", material);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        long[] read = tag.getLongArray("Layers");
        if (read.length == N) {
            System.arraycopy(read, 0, layers, 0, N);
        }
        material = tag.getInt("Material");
        shape = null;
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
}
