package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBridge extends Building {

    public final boolean isDiagonal;

    public AbstractBridge(Level level, BlockPos originPos, Rotation rotation, String ownerName, boolean isDiagonal, ArrayList<BuildingBlock> culledBlocks) {
        super(level, originPos, rotation, ownerName, culledBlocks,false);
        this.isDiagonal = isDiagonal;
    }

    public static boolean shouldCullBlock(BlockPos originPos, BuildingBlock b, Level level) {
        BlockState bs = b.getBlockState();
        boolean isFenceWallOrAir = b.getBlockState().getBlock() instanceof AirBlock ||
                b.getBlockState().getBlock() instanceof FenceBlock ||
                b.getBlockState().getBlock() instanceof WallBlock;
        BlockPos bp = b.getBlockPos().offset(originPos);
        Material bm = level.getBlockState(bp).getMaterial();
        Material bmBelow = level.getBlockState(bp.below()).getMaterial();

        for (BlockPos bpAdj : List.of(bp.north(), bp.south(), bp.east(), bp.west())) {
            BlockState bsAdj = level.getBlockState(bpAdj);
            if (isFenceWallOrAir && !bsAdj.isAir() && BuildingUtils.isPosInsideAnyBuilding(level.isClientSide, bpAdj))
                return true;
        }


        return bm.isSolidBlocking() || (isFenceWallOrAir && bmBelow.isSolidBlocking());
    }

    protected static ArrayList<BuildingBlock> getCulledBlocks(ArrayList<BuildingBlock> blocks, Level level) {
        blocks.removeIf(b -> shouldCullBlock(new BlockPos(0,0,0), b, level));
        return blocks;
    }

    private void replaceWithLiquidBelow(BlockPos bp) {
        BlockState bs = level.getBlockState(bp);
        boolean isFenceOrWall = bs.getBlock() instanceof FenceBlock || bs.getBlock() instanceof WallBlock;

        BlockState bsBelow = level.getBlockState(bp.below());
        if (bsBelow.getMaterial().isLiquid() && !isFenceOrWall)
            level.setBlockAndUpdate(bp, bsBelow);
    }

    @Override
    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks) {
        super.onBlockBreak(level, pos, breakBlocks);
        replaceWithLiquidBelow(pos);
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        super.destroy(serverLevel);
        for (BuildingBlock bb : blocks) // need to check first here since we already destroyed the level blocks
            if (!(bb.getBlockState().getBlock() instanceof FenceBlock) &&
                !(bb.getBlockState().getBlock() instanceof WallBlock) &&
                !(bb.getBlockState().getBlock() instanceof AirBlock))
                replaceWithLiquidBelow(bb.getBlockPos());
    }
}
