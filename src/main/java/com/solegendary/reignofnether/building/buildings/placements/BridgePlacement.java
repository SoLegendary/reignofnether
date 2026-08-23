package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class BridgePlacement extends BuildingPlacement {
    public BridgePlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName,
                           ArrayList<BuildingBlock> blocks, boolean isCapitol, boolean isDiagonal) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        this.isDiagonalBridge = isDiagonal;
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        super.onBlockBreak(level, pos, breakBlocks);
        for (BuildingBlock bb : getBlocks()) {
            if (bb.getBlockPos().equals(pos)) {
                replaceWithLiquidBelow(pos, bb.getBlockState());
                return;
            }
        }
    }

    // todo: fix
    /*
    @Override
    protected void setBlocks(ArrayList<BuildingBlock> blocks) {
        this.blocks = getCulledBlocks(blocks);
    }

    private ArrayList<BuildingBlock> getCulledBlocks(ArrayList<BuildingBlock> blocks) {
        blocks.removeIf((b) -> Building.shouldCullBlock(originPos, b, level));
        return blocks;
    }
     */

    public void replaceWithLiquidBelow( BlockPos bp, BlockState bs) {
        if (!(bs.getBlock() instanceof FenceBlock) && !level.isClientSide()) {
            for (BlockPos bpAdj : List.of(bp.below(), bp.north(), bp.south(), bp.east(), bp.west())) {
                BlockState bsAdj = level.getBlockState(bpAdj);
                if (!bsAdj.getFluidState().isEmpty()) {
                    BlockServerEvents.blocksToPlace.put(bp, bsAdj.getBlock().defaultBlockState());
                    break;
                }
            }
        }
    }
}
