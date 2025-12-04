package com.solegendary.reignofnether.blocks;

import com.solegendary.reignofnether.registrars.BlockEntityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GarrisonBlockEntity extends BlockEntity {

    public GarrisonBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistrar.GARRISON_BLOCK_ENTITY.get(), pos, state);
    }
}