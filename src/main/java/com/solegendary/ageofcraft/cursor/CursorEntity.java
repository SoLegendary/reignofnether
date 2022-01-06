package com.solegendary.ageofcraft.cursor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// entity used to track where the game-space location of the cursor is

// it needs to be:
// easily seen from a distance
// not mistaken for a mob or any other entity
// uninteractable apart from being a marker
// good for performance

// think of like the right click flag in an RTS game

public class CursorEntity extends BannerBlockEntity {

    public CursorEntity(BlockPos p_155035_, BlockState p_155036_) {
        super(p_155035_, p_155036_);
    }
}
