package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.world.level.block.WallSkullBlock;

public class BoggedWallSkullBlock extends WallSkullBlock {

    public BoggedWallSkullBlock(Properties props) {
        super(SkullTypes.BOGGED, props);
    }

    @Override
    public String getDescriptionId() {
        return BlockRegistrar.BOGGED_SKULL.get().getDescriptionId();
    }
}