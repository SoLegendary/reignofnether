package com.solegendary.ageofcraft.cursorentity;

import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;

// entity used to track where the game-space location of the cursor is

// it needs to be:
// easily seen from a distance
// not mistaken for a mob or any other entity
// uninteractable apart from being a marker
// good for performance

// think of like the right click flag in an RTS game

public class CursorEntity extends ArmorStand {

    public CursorEntity(Level p_i45855_1_, double p_i45855_2_, double p_i45855_4_, double p_i45855_6_) {
        super(p_i45855_1_, p_i45855_2_, p_i45855_4_, p_i45855_6_);
    }
}
