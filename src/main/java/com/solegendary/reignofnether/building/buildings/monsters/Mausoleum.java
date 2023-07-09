package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.unit.units.monsters.ZombieVillagerUnitProd;
import com.solegendary.reignofnether.hud.AbilityButton;
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

public class Mausoleum extends ProductionBuilding {

    public final static String buildingName = "Mausoleum";
    public final static String structureName = "mausoleum";
    public final static int nightRange = 80;

    public Mausoleum(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), true);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.blocks = getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation);
        this.portraitBlock = Blocks.DEEPSLATE_TILES;
        this.icon = new ResourceLocation("minecraft", "textures/block/deepslate_tiles.png");

        this.foodCost = ResourceCosts.Mausoleum.FOOD;
        this.woodCost = ResourceCosts.Mausoleum.WOOD;
        this.oreCost = ResourceCosts.Mausoleum.ORE;
        this.popSupply = ResourceCosts.Mausoleum.SUPPLY;
        this.buildTimeModifier = 0.66f;
        this.canAcceptResources = true;

        this.startingBlockTypes.add(Blocks.STONE);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);

        if (level.isClientSide())
            this.productionButtons = List.of(
                ZombieVillagerUnitProd.getStartButton(this, Keybindings.keyQ)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Mausoleum.buildingName,
                new ResourceLocation("minecraft", "textures/block/deepslate_tiles.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Mausoleum.class,
                () -> false,
                () -> true,
                () -> BuildingClientEvents.setBuildingToPlace(Mausoleum.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Mausoleum.buildingName, Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("\uE001  " + ResourceCosts.Mausoleum.WOOD + "  \uE002  " + ResourceCosts.Mausoleum.ORE, MyRenderer.iconStyle),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("A tomb of the dead that produces zombie villagers.", Style.EMPTY),
                        FormattedCharSequence.forward("Distorts time to midnight within a " + nightRange + " block radius.", Style.EMPTY),
                        FormattedCharSequence.forward("You may only have one mausoleum at any time.", Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Supports " + ResourceCosts.Mausoleum.SUPPLY + " population.", Style.EMPTY)
                ),
                null
        );
    }
}