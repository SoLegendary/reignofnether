package com.solegendary.reignofnether.building;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class BuildingBlock {
    private BlockPos blockPos;
    private BlockState blockState;
    public Boolean isPlaced = false;

    public BuildingBlock(BlockPos blockPos, BlockState blockState) {
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    public BlockPos getBlockPos() { return blockPos; }
    public BlockState getBlockState() { return blockState; }

    public void setBlockPos(BlockPos bp) { this.blockPos = bp; }
    public void setBlockState(BlockState bs) { this.blockState = bs; }

    public void rotate(LevelAccessor level, Rotation rotation) {
        this.blockPos = this.blockPos.rotate(rotation);
        this.blockState = this.blockState.rotate(level, blockPos, rotation);
    }

    public void place(ServerLevel level) {

    }

    public void destroy(ServerLevel level) {

    }
}