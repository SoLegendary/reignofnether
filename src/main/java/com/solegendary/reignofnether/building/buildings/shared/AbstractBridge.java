package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BridgePlacement;
import com.solegendary.reignofnether.building.data.DataType;
import com.solegendary.reignofnether.resources.ResourceCost;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractBridge extends Building {
    //Is this still necessary?
    public static final DataType<Boolean> DIAGONAL = DataType.createRegistered(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "diagonal"), (tag, server) -> tag.getBoolean("diagonal"), diagonal -> {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("diagonal", diagonal);
        return tag;
    }, () -> false);

    public AbstractBridge(ResourceCost cost) {
        super("", cost, false);
        this.maxHealth = 350d;
    }

    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level, boolean diagonal) {
        return BuildingBlockData.getBuildingBlocksFromNbt(diagonal ? getDiagonalStructureName() : getOrthogonalStructureName(), level);
    }

    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName, boolean diagonal) {
        BuildingPlacement placement = new BridgePlacement(this, level, pos, rotation, ownerName,
                BuildingUtils.getAbsoluteBlockData(this.getRelativeBlockData(level, diagonal), level, pos, rotation), this.isCapitol, diagonal);
        return placement;
    }

    public abstract String getDiagonalStructureName();
    public abstract String getOrthogonalStructureName();

    public void destroy(ServerLevel serverLevel, BuildingPlacement placement) {
        super.destroy(serverLevel, placement);
        if (placement instanceof BridgePlacement bpl) {
            for (BuildingBlock bb : placement.getBlocks()) // need to check first here since we already destroyed the level blocks
                if (!(bb.getBlockState().getBlock() instanceof FenceBlock) &&
                        !(bb.getBlockState().getBlock() instanceof AirBlock))
                    bpl.replaceWithLiquidBelow(bb.getBlockPos(), bb.getBlockState());
        }
    }

    public static boolean shouldCullBlock(BlockPos originPos, BuildingBlock b, Level level) {
        BlockState bs = b.getBlockState();

        boolean isFenceOrAir = b.getBlockState().getBlock() instanceof AirBlock ||
                b.getBlockState().getBlock() instanceof FenceBlock;
        BlockPos bp = b.getBlockPos();

        if (level.isClientSide)
            bp = bp.offset(originPos);

        // if the block in the world matches this exactly, don't cull it, instead just consider it to be our block too
        BlockState bsWorld = level.getBlockState(bp);

        if (bsWorld.getBlock() == Blocks.OBSIDIAN)
            return false;
        if (bsWorld.equals(bs))
            return false;
        if ((bsWorld.isAir() || !bsWorld.getFluidState().isEmpty()) && !isFenceOrAir)
            return false;

        // cull if fence is adjacent to another solid block (or a bridge block, even if air)
        for (BlockPos bpAdj : List.of(bp.north(), bp.south(), bp.east(), bp.west())) {
            BlockState bsWorldAdj = level.getBlockState(bpAdj);
            if (isFenceOrAir && !bsWorldAdj.isAir() && BuildingUtils.isPosInsideAnyBuilding(level.isClientSide, bpAdj))
                return true;
        }
        boolean isSolid = level.getBlockState(bp).isSolid();
        boolean belowIsSolid = level.getBlockState(bp.below()).isSolid();

        return isSolid || (isFenceOrAir && belowIsSolid);
    }
}
