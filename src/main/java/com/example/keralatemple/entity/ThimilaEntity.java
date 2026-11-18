package com.example.keralatemple.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ThimilaEntity extends MusicianEntity {
    public ThimilaEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level, Instrument.THIMILA);
    }

    @Override
    public Instrument instrument() {
        return Instrument.THIMILA;
    }
}
