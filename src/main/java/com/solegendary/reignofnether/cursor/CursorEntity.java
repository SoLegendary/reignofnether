package com.solegendary.reignofnether.cursor;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;

// entity used to track where the game-space location of the cursor is like the right click flag in an RTS game
// TODO: make this a banner with set patterns, always rotated towards camera, able to be made hidden when needed

public class CursorEntity extends BannerBlock {

    public CursorEntity(DyeColor p_49012_, Properties p_49013_) {
        super(p_49012_, p_49013_);
    }
}
