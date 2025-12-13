package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.building.buildings.placements.SculkCatalystPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

public class BuildingBlock {
    private BlockPos blockPos;
    private BlockState blockState; // ideal blockstate when placed, not actual world state
    private CompoundTag blockNbt = null;
    private final HashMap<BlockState, Boolean> ignoredCache = new HashMap<>();

    private boolean isIgnored(BlockState state) {
        if (ignoredCache.containsKey(state)) {
            return ignoredCache.get(state);
        }
        var isIgnored = false;
        if (state.is(BlockTags.LEAVES)) isIgnored = true;
        else if (state.is(Tags.Blocks.GLASS)) isIgnored = true;
        else if (SculkCatalystPlacement.isSculk(state.getBlock())) isIgnored = true;
        ignoredCache.put(state, isIgnored);
        return false;
    }

    public BuildingBlock(BlockPos blockPos, BlockState blockState) {
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    public BuildingBlock(BlockPos blockPos, BlockState blockState, CompoundTag blockNbt) {
        this.blockPos = blockPos;
        this.blockState = blockState;
        this.blockNbt = blockNbt;
    }

    public BlockPos getBlockPos() { return blockPos; }
    public BlockState getBlockState() { return blockState; }
    @Nullable public CompoundTag getBlockNbt() { return blockNbt; }

    public void setBlockPos(BlockPos bp) { this.blockPos = bp; }
    public void setBlockState(BlockState bs) { this.blockState = bs; }

    // rotation should only ever be done on a relative BlockPos or it will rotate about world (0,0)
    public BuildingBlock rotate(LevelAccessor level, Rotation rotation) {
        return new BuildingBlock(
            this.blockPos.rotate(rotation),
            this.blockState.rotate(level, blockPos, rotation),
            this.blockNbt
        );
    }

    public BuildingBlock move(LevelAccessor level, BlockPos offset) {
        return new BuildingBlock(
            this.blockPos.offset(offset),
            this.blockState,
            this.blockNbt
        );
    }

    public boolean isPlaced(Level level) {
        BlockState bs;
        if (level.isClientSide())
            bs = Minecraft.getInstance().level.getBlockState(this.blockPos);
        else
            bs = level.getBlockState(this.blockPos);

        // wall blockstates don't match unless the block above them is placed
        boolean isMatchingWallBlock = this.blockState.getBlock() instanceof WallBlock && bs.getBlock() == this.blockState.getBlock();

        // account for sculk sensors turning on and off constantly
        if (isIgnored(this.blockState) &&
                isIgnored(bs))
            return true;

        Block block1 = this.blockState.getBlock();
        Block block2 = bs.getBlock();
        if ((block1 instanceof StairBlock && block2 instanceof StairBlock) ||
            (block1 instanceof FenceBlock && block2 instanceof FenceBlock) ||
            (block1 instanceof WallBlock && block2 instanceof WallBlock) ||
            (block1 instanceof IronBarsBlock && block2 instanceof IronBarsBlock) ||
            (block1 instanceof TallGrassBlock && block2 instanceof TallGrassBlock))
            return true;

        return !this.blockState.isAir() && (bs == this.blockState || isMatchingWallBlock);
    }
}