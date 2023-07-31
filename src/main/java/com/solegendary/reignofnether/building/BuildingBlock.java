package com.solegendary.reignofnether.building;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public class BuildingBlock {
    private BlockPos blockPos;
    private BlockState blockState; // ideal blockstate when placed, not actual world state

    public BuildingBlock(BlockPos blockPos, BlockState blockState) {
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    public BlockPos getBlockPos() { return blockPos; }
    public BlockState getBlockState() { return blockState; }

    public void setBlockPos(BlockPos bp) { this.blockPos = bp; }
    public void setBlockState(BlockState bs) { this.blockState = bs; }

    // rotation should only ever be done on a relative BlockPos or it will rotate about world (0,0)
    public BuildingBlock rotate(LevelAccessor level, Rotation rotation) {
        return new BuildingBlock(
            this.blockPos.rotate(rotation),
            this.blockState.rotate(level, blockPos, rotation)
        );
    }

    public boolean isPlaced(Level level) {
        if (level.isClientSide())
            return !this.blockState.isAir() && Minecraft.getInstance().level.getBlockState(this.blockPos) == this.blockState;
        else
            return !this.blockState.isAir() && level.getBlockState(this.blockPos) == this.blockState;
    }
}