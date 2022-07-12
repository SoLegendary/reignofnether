package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingBlockData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class VillagerTower extends Building {

    public VillagerTower(String structureName) {
        super(structureName);
        this.blocks = getBlockData();
        this.palette = getPaletteData();
    }

    public static ArrayList<BuildingBlock> getBlockData() {
        return BuildingBlockData.VILLAGER_TOWER_BLOCKS;
    }
    public static ArrayList<BlockState> getPaletteData() {
        return BuildingBlockData.VILLAGER_TOWER_PALETTE;
    }
}
