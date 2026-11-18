package com.example.keralatemple.block;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * A temple door (wood, gold-plated wood or gold). Players open it by hand like any door, and the melshanthi opens
 * and closes all of a temple's doors at nada thurakkal and nada adakkal.
 */
public class TempleDoorBlock extends DoorBlock {
    public TempleDoorBlock(Properties properties) {
        super(properties, BlockSetType.OAK);
    }
}
