package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.resources.ResourceCosts;
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
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class VillagerHouse extends ProductionBuilding {

    public final static String buildingName = "Villager House";
    public final static String structureName = "villager_house";

    public VillagerHouse(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.OAK_LOG;
        this.spawnRadiusOffset = 1;
        this.icon = new ResourceLocation("minecraft", "textures/block/oak_log.png");

        this.foodCost = ResourceCosts.VillagerHouse.FOOD;
        this.woodCost = ResourceCosts.VillagerHouse.WOOD;
        this.oreCost = ResourceCosts.VillagerHouse.ORE;
        this.popSupply = ResourceCosts.VillagerHouse.SUPPLY;

        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.OAK_PLANKS);
        this.startingBlockTypes.add(Blocks.OAK_LOG);
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
            VillagerHouse.buildingName,
            Button.itemIconSize,
            new ResourceLocation("minecraft", "textures/block/oak_log.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == VillagerHouse.class,
            () -> !BuildingClientEvents.hasFinishedBuilding(TownCentre.buildingName),
            () -> true,
            () -> BuildingClientEvents.setBuildingToPlace(VillagerHouse.class),
            null,
            List.of(
                    FormattedCharSequence.forward(VillagerHouse.buildingName, Style.EMPTY),
                    FormattedCharSequence.forward("\uE001  " + ResourceCosts.VillagerHouse.WOOD, MyRenderer.iconStyle),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("A simple house that provides population supply.", Style.EMPTY),
                    FormattedCharSequence.forward("Supports " + ResourceCosts.VillagerHouse.SUPPLY + " population.", Style.EMPTY)
            ),
            null
        );
    }
}
