package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class VillagerHouse extends Building {

    public VillagerHouse(String structureName, LevelAccessor level, Rotation rotation) {
        super(structureName);
        this.blocks = getBlockData();
        for (BuildingBlock block : this.blocks)
            block.rotate(level, rotation);
    }

    public static ArrayList<BuildingBlock> getBlockData() {
        return BuildingBlockData.VILLAGER_HOUSE_BLOCKS;
    }
}
