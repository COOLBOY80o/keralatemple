package com.example.keralatemple.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ChengilaEntity extends MusicianEntity {
    public ChengilaEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level, Instrument.CHENGILA);
    }

    @Override
    public Instrument instrument() {
        return Instrument.CHENGILA;
    }
}
