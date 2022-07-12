package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class VillagerHouse extends Building {

    public VillagerHouse(String structureName) {
        super(structureName);
        this.blocks = getBlockData();
        this.palette = getPaletteData();
    }

    public static ArrayList<BuildingBlock> getBlockData() {
        return BuildingBlockData.VILLAGER_HOUSE_BLOCKS;
    }
    public static ArrayList<BlockState> getPaletteData() {
        return BuildingBlockData.VILLAGER_HOUSE_PALETTE;
    }
}
