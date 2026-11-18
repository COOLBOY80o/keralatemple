package com.example.keralatemple.blockentity;

import com.example.keralatemple.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds when the bucket last went down (the client animates it). */
public class WellPulleyBlockEntity extends BlockEntity {
    public static final int DRAW_TICKS = 60;
    private long animStart = -1000L;

    public WellPulleyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WELL_PULLEY.get(), pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1 && level != null) {
            animStart = level.getGameTime();
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** 0..1 while the bucket is going down and up again, -1 when it is resting. */
    public float progress(float partialTick) {
        if (level == null) {
            return -1.0F;
        }
        float t = (level.getGameTime() + partialTick - animStart) / DRAW_TICKS;
        return t >= 0.0F && t <= 1.0F ? t : -1.0F;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0.0D, -4.0D, 0.0D);
    }
}
