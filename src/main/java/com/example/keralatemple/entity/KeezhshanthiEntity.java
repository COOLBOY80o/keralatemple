package com.example.keralatemple.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The keezhshanthi: the melshanti's assistant. He waits beside the sanctum during poojas, follows the
 * seeveli procession a little behind the melshanti, and makes the bali offerings at the bali stones.
 */
public class KeezhshanthiEntity extends PriestEntity {

    public KeezhshanthiEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean isAssistant() {
        return true;
    }
}
