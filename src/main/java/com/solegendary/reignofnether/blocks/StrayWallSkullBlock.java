package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.world.level.block.WallSkullBlock;

public class StrayWallSkullBlock extends WallSkullBlock {

    public StrayWallSkullBlock(Properties props) {
        super(SkullTypes.STRAY, props);
    }

    @Override
    public String getDescriptionId() {
        return BlockRegistrar.STRAY_SKULL.get().getDescriptionId();
    }
}