package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.registrars.BlockRegistrar;
import net.minecraft.world.level.block.WallSkullBlock;

public class HuskWallSkullBlock extends WallSkullBlock {

    public HuskWallSkullBlock(Properties props) {
        super(SkullTypes.HUSK, props);
    }

    @Override
    public String getDescriptionId() {
        return BlockRegistrar.HUSK_HEAD.get().getDescriptionId();
    }
}