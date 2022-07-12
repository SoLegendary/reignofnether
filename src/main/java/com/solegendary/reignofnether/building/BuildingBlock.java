package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class BuildingBlock {
    public BlockPos blockPos;
    public Integer paletteIndex;
    public Boolean isPlaced = false;

    public BuildingBlock(BlockPos blockPos, Integer paletteIndex) {
        this.blockPos = blockPos;
        this.paletteIndex = paletteIndex;
    }

    public BlockState getBlockState(ArrayList<BlockState> palette) {
        return palette.get(paletteIndex);
    }

    public void place(ServerLevel level) {

    }

    public void destroy(ServerLevel level) {

    }
}