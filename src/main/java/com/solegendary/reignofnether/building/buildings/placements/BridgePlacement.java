package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.blocks.BlockServerEvents;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
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

    private int getFullSolidBlockCount() {
        return ((AbstractBridge) getBuilding()).getRelativeBlockData(level, this.isDiagonalBridge)
                .stream().filter(b -> b.getBlockState().isSolid())
                .toList()
                .size();
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

    private List<BuildingBlock> getSolidBlocks() {
        return blocks.stream().filter(b -> b.getBlockState().isSolid()).toList();
    }

    @Override
    public double getHealthPerBlock() {
        int solidBlocks = getSolidBlocks().size();
        return (double) (getMaxHealth() / solidBlocks) * 2;
    }

    @Override
    public int getHealth() {
        if (getBlocksPlaced() >= getSolidBlocks().size() && partialBlocksDestroyed <= 0)
            return getMaxHealth();
        return super.getHealth();
    }

    @Override
    public int getMaxHealth() {
        return (int) (super.getMaxHealth() * ((double) this.getSolidBlocks().size() / getFullSolidBlockCount()));
    }

    @Override
    protected void setBlocks(ArrayList<BuildingBlock> blocks) {
        super.setBlocks(blocks);
        this.blocks = new ArrayList<>(
            getCulledBlocks(blocks).stream().filter(b -> b.getBlockState().isSolid()).toList()
        );
        this.totalBlocks = this.blocks.size();
    }

    private ArrayList<BuildingBlock> getCulledBlocks(ArrayList<BuildingBlock> blocks) {
        blocks.removeIf((b) -> AbstractBridge.shouldCullBlock(originPos, b, level));
        return blocks;
    }

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
