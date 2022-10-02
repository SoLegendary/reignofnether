package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VillagerHouse extends ProductionBuilding {

    public final static String buildingName = "Villager House";
    public final static String structureName = "villager_house";

    public VillagerHouse(LevelAccessor level, BlockPos originPos, Rotation rotation, String ownerName) {
        super();
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.OAK_LOG;

        this.productionButtons = Arrays.asList(
                CreeperUnitProd.getStartButton(this),
                ZombieUnitProd.getStartButton(this),
                SkeletonUnitProd.getStartButton(this)
        );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }
}
