package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.monsters.WardenUnitProd;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnitProd;
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

public class Castle extends ProductionBuilding implements GarrisonableBuilding {

    public final static String buildingName = "Castle";
    public final static String structureName = "castle";
    public final static ResourceCost cost = ResourceCosts.CASTLE;

    private final static int MAX_OCCUPANTS = 10;

    public Castle(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.CHISELED_STONE_BRICKS;
        this.icon = new ResourceLocation("minecraft", "textures/block/chiseled_stone_bricks.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 0.8f;

        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_SLAB);

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                    RavagerUnitProd.getStartButton(this, Keybindings.keyQ)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Castle.buildingName,
                new ResourceLocation("minecraft", "textures/block/chiseled_stone_bricks.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Castle.class,
                () -> false,
                () -> (BuildingClientEvents.hasFinishedBuilding(Barracks.buildingName) &&
                        BuildingClientEvents.hasFinishedBuilding(ArcaneTower.buildingName)) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                () -> BuildingClientEvents.setBuildingToPlace(Castle.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Castle.buildingName, Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A grand castle that can produce wardens ", Style.EMPTY),
                        FormattedCharSequence.forward("and garrison up to " + MAX_OCCUPANTS + " units.", Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Requires an arcane tower and a barracks.", Style.EMPTY)
                ),
                null
        );
    }

    @Override
    public BlockPos getEntryPosition() {
        return new BlockPos(2,11,2);
    }

    @Override
    public BlockPos getExitPosition() {
        return new BlockPos(2,1,2);
    }

    @Override
    public boolean isFull() { return GarrisonableBuilding.getNumOccupants(this) >= MAX_OCCUPANTS; }
}
