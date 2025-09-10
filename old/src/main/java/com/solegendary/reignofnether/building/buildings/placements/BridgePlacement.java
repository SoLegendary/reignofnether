package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BridgePlacement extends BuildingPlacement {
    boolean diagonal;
    public BridgePlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol, boolean diagonal) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        this.diagonal = diagonal;
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        BlockState bs = level.getBlockState(pos);
        super.onBlockBreak(level, pos, breakBlocks);
        replaceWithLiquidBelow(pos, bs);
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        for (BuildingBlock bb : blocks) // need to check first here since we already destroyed the level blocks
            if (!(bb.getBlockState().getBlock() instanceof FenceBlock) &&
                    !(bb.getBlockState().getBlock() instanceof AirBlock))
                replaceWithLiquidBelow(bb.getBlockPos(), bb.getBlockState());
    }



    private void replaceWithLiquidBelow(BlockPos bp, BlockState bs) {
        if (!(bs.getBlock() instanceof FenceBlock)) {
            for (BlockPos bpAdj : List.of(bp.below(), bp.north(), bp.south(), bp.east(), bp.west())) {
                BlockState bsAdj = level.getBlockState(bpAdj);
                if (!bsAdj.getFluidState().isEmpty())
                    level.setBlockAndUpdate(bp, bsAdj);
            }
        }
    }

    public boolean isDiagonal() {
        return diagonal;
    }
}
