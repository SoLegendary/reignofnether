package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.world.level.block.WallSkullBlock;

public class DrownedWallSkullBlock extends WallSkullBlock {

    public DrownedWallSkullBlock(Properties props) {
        super(SkullTypes.DROWNED, props);
    }

    @Override
    public String getDescriptionId() {
        return BlockRegistrar.DROWNED_HEAD.get().getDescriptionId();
    }
}