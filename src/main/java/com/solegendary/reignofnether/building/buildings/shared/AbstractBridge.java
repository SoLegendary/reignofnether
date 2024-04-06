package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public abstract class AbstractBridge extends Building {

    public AbstractBridge(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getCulledBlocks(getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), level), false);
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static boolean shouldCullBlock(BlockPos originPos, BuildingBlock b, Level level) {
        BlockState bs = b.getBlockState();
        boolean isFenceWallOrAir = b.getBlockState().getBlock() instanceof AirBlock ||
                b.getBlockState().getBlock() instanceof FenceBlock ||
                b.getBlockState().getBlock() instanceof WallBlock;
        BlockPos bp = b.getBlockPos().offset(originPos);
        Material bm = level.getBlockState(bp).getMaterial();
        Material bmBelow = level.getBlockState(bp.below()).getMaterial();

        for (BlockPos bpAdj : List.of(bp.north(), bp.south(), bp.east(), bp.west()))
            if (isFenceWallOrAir && BuildingUtils.isPosPartOfAnyBuilding(level.isClientSide, bpAdj, false))
                return true;

        return bm.isSolidBlocking() || (isFenceWallOrAir && bmBelow.isSolidBlocking());
    }

    private static ArrayList<BuildingBlock> getCulledBlocks(ArrayList<BuildingBlock> blocks, Level level) {
        blocks.removeIf(b -> shouldCullBlock(new BlockPos(0,0,0), b, level));
        return blocks;
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        super.onBlockBreak(level, pos, breakBlocks);
        BlockState bsBelow = level.getBlockState(pos.below());
        if (bsBelow.getMaterial().isLiquid())
            level.setBlockAndUpdate(pos, bsBelow);
    }
}
