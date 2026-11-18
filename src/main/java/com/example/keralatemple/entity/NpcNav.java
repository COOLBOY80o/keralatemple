package com.example.keralatemple.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;

/** Small walking helper shared by the assistant NPCs. */
final class NpcNav {
    private NpcNav() {}

    /** Walks towards the block; true once within reachSq (squared distance). */
    static boolean goTo(PathfinderMob mob, BlockPos pos, double speed, double reachSq) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        if (mob.distanceToSqr(x, y, z) < reachSq) {
            mob.getNavigation().stop();
            return true;
        }
        if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) {
            mob.getNavigation().moveTo(x, y, z, speed);
        }
        return false;
    }
}
