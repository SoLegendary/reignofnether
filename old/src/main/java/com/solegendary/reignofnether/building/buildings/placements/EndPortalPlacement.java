package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class EndPortalPlacement extends ProductionPlacement{
    public EndPortalPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(centrePos.above(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().north(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().south(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().east(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().west(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().north().east(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().north().west(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().south().east(), Blocks.END_PORTAL.defaultBlockState());
            level.setBlockAndUpdate(centrePos.above().south().west(), Blocks.END_PORTAL.defaultBlockState());
        }
    }
}
