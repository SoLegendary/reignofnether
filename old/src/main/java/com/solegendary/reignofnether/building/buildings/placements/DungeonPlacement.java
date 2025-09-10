package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class DungeonPlacement extends ProductionPlacement{
    public DungeonPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    //Can't you define this in the structure (datapack)? Then you wouldn't need this custom placement
    @Override
    public void onBlockBuilt(BlockPos bp, BlockState bs) {
        if (!this.getLevel().isClientSide()) {
            if (bs.hasBlockEntity()) {
                BlockEntity be = this.getLevel().getBlockEntity(bp);
                if (be instanceof SpawnerBlockEntity sbe)
                    sbe.getSpawner().setEntityId(EntityType.CREEPER, getLevel(), getLevel().random, bp);
            }
        }
    }
}
