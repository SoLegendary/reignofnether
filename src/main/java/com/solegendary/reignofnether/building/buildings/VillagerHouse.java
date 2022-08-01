package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;

public class VillagerHouse extends Building {

    public static final String buildingName = "Villager House";

    public VillagerHouse(LevelAccessor level, BlockPos originPos, Rotation rotation) {
        super(buildingName, level.isClientSide());
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(), level, originPos, rotation);
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData() {
        return (ArrayList) BuildingBlockData.VILLAGER_HOUSE_BLOCKS.clone();
    }
}
