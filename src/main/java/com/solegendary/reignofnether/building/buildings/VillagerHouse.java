package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;

public class VillagerHouse extends Building {

    public VillagerHouse(String structureName, LevelAccessor level, Rotation rotation) {
        super(structureName);
        this.blocks = getBlockData();
        for (BuildingBlock block : this.blocks)
            block.rotate(level, rotation);
    }

    public static ArrayList<BuildingBlock> getBlockData() {
        return (ArrayList) BuildingBlockData.VILLAGER_HOUSE_BLOCKS.clone();
    }
}
