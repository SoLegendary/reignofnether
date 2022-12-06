package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.unit.ResourceCosts;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnitProd;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorUnitProd;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Barracks extends ProductionBuilding {

    public final static String buildingName = "Barracks";
    public final static String structureName = "barracks";

    public Barracks(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.FLETCHING_TABLE;
        this.spawnRadiusOffset = 1;
        this.icon = new ResourceLocation("minecraft", "textures/block/fletching_table_front.png");

        this.foodCost = ResourceCosts.Barracks.FOOD;
        this.woodCost = ResourceCosts.Barracks.WOOD;
        this.oreCost = ResourceCosts.Barracks.ORE;
        this.popSupply = ResourceCosts.Barracks.SUPPLY;

        this.explodeChance = 0.2f;

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                    VindicatorUnitProd.getStartButton(this),
                    PillagerUnitProd.getStartButton(this)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton() {
        return new AbilityButton(
                Barracks.buildingName,
                Button.itemIconSize,
                new ResourceLocation("minecraft", "textures/block/fletching_table_front.png"),
                null,
                () -> BuildingClientEvents.getBuildingToPlace() == Barracks.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Barracks.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Barracks.buildingName, Style.EMPTY),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Barracks.WOOD, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A training ground for Pillagers and Vindicators", Style.EMPTY)
                ),
                0,0,0
        );
    }
}
