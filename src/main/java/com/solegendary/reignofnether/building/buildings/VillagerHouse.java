package com.solegendary.reignofnether.building.buildings;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.productionitems.CreeperUnitProd;
import com.solegendary.reignofnether.building.productionitems.SkeletonUnitProd;
import com.solegendary.reignofnether.building.productionitems.ZombieUnitProd;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class VillagerHouse extends ProductionBuilding {

    public final static String buildingName = "Villager House";
    public final static String structureName = "villager_house";

    public VillagerHouse(LevelAccessor level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.OAK_LOG;
        this.spawnRadiusOffset = 1;

        this.foodCost = ResourceCosts.VillagerHouse.FOOD;
        this.woodCost = ResourceCosts.VillagerHouse.WOOD;
        this.oreCost = ResourceCosts.VillagerHouse.ORE;
        this.popSupply = ResourceCosts.VillagerHouse.SUPPLY;

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                CreeperUnitProd.getStartButton(this),
                SkeletonUnitProd.getStartButton(this),
                ZombieUnitProd.getStartButton(this)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton() {
        return new AbilityButton(
            VillagerHouse.buildingName,
            Button.itemIconSize,
            new ResourceLocation("minecraft", "textures/block/oak_log.png"),
            null,
            () -> BuildingClientEvents.getBuildingToPlace() == VillagerHouse.class,
            () -> false,
            () -> true,
            () -> BuildingClientEvents.setBuildingToPlace(VillagerHouse.class),
            List.of(
                    FormattedCharSequence.forward(VillagerHouse.buildingName, Style.EMPTY),
                    FormattedCharSequence.forward("\uE001  " + ResourceCosts.VillagerHouse.WOOD, MyRenderer.iconStyle),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("A simple house that can support " + ResourceCosts.VillagerHouse.SUPPLY + " population.", Style.EMPTY)
            ),
            0,0,0
        );
    }
}
