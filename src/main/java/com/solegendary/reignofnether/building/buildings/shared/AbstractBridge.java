package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public abstract class AbstractBridge extends Building {
    //Is this still necessary?
    public static final DataType<Boolean> DIAGONAL = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "diagonal"), (tag, server) -> tag.getBoolean("diagonal"), diagonal -> {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("diagonal", diagonal);
        return tag;
    }, () -> false);
    public final float MELEE_DAMAGE_MULTIPLIER = 0.05f;

    public AbstractBridge(ResourceCost cost) {
        super("", cost, false);
    }

    @Override
    public float getMeleeDamageMult() { return MELEE_DAMAGE_MULTIPLIER; }

    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level, boolean diagonal) {
        return BuildingBlockData.getBuildingBlocksFromNbt(diagonal ? getDiagonalStructureName() : getOrthogonalStructureName(), level);
    }

    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName, boolean diagonal) {
        BuildingPlacement placement = new BuildingPlacement(this, level, pos, rotation, ownerName, BuildingUtils.getAbsoluteBlockData(this.getRelativeBlockData(level, diagonal), level, pos, rotation), this.isCapitol);
        //Is this still necessary?
        placement.getDataStorage().setData(DIAGONAL, diagonal);
        return placement;
    }

    public abstract String getDiagonalStructureName();
    public abstract String getOrthogonalStructureName();

    public void onBlockBreak(ServerLevel level, BlockPos pos, boolean breakBlocks, BuildingPlacement placement) {
        BlockState bs = level.getBlockState(pos);
//        super.onBlockBreak(level, pos, breakBlocks);
        for (BuildingBlock bb : placement.getBlocks()) {
            if (bb.getBlockPos().equals(pos)) {
                replaceWithLiquidBelow(placement, pos, bb.getBlockState());
                return;
            }
        }
    }

    public void destroy(ServerLevel serverLevel, BuildingPlacement placement) {
        super.destroy(serverLevel, placement);
        for (BuildingBlock bb : placement.getBlocks()) // need to check first here since we already destroyed the level blocks
            if (!(bb.getBlockState().getBlock() instanceof FenceBlock) &&
                    !(bb.getBlockState().getBlock() instanceof AirBlock))
                replaceWithLiquidBelow(placement, bb.getBlockPos(), bb.getBlockState());
    }

    private void replaceWithLiquidBelow(BuildingPlacement placement, BlockPos bp, BlockState bs) {
        if (!(bs.getBlock() instanceof FenceBlock)) {
            for (BlockPos bpAdj : List.of(bp.below(), bp.north(), bp.south(), bp.east(), bp.west())) {
                BlockState bsAdj = placement.level.getBlockState(bpAdj);
                if (!bsAdj.getFluidState().isEmpty()) {
                    placement.level.setBlockAndUpdate(bp, bsAdj);
                    break;
                }
            }
        }
    }
}
