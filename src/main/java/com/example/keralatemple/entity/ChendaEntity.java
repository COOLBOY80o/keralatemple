package com.example.keralatemple.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ChendaEntity extends MusicianEntity {
    public ChendaEntity(EntityType<? extends PriestEntity> type, Level level) {
        super(type, level, Instrument.CHENDA);
    }

    @Override
    public Instrument instrument() {
        return Instrument.CHENDA;
    }
}
