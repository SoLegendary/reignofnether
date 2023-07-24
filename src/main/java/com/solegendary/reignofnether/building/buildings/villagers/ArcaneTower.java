package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchEvokerVexes;
import com.solegendary.reignofnether.research.researchItems.ResearchEvokers;
import com.solegendary.reignofnether.research.researchItems.ResearchLingeringPotions;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.villagers.EvokerProdItem;
import com.solegendary.reignofnether.unit.units.villagers.PillagerProdItem;
import com.solegendary.reignofnether.unit.units.villagers.VindicatorProdItem;
import com.solegendary.reignofnether.unit.units.villagers.WitchProdItem;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class ArcaneTower extends ProductionBuilding {

    public final static String buildingName = "Arcane Tower";
    public final static String structureName = "arcane_tower";
    public final static ResourceCost cost = ResourceCosts.ARCANE_TOWER;

    public ArcaneTower(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.ENCHANTING_TABLE;
        this.icon = new ResourceLocation("minecraft", "textures/block/enchanting_table_top.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;

        this.startingBlockTypes.add(Blocks.STONE_BRICKS);
        this.startingBlockTypes.add(Blocks.ANDESITE_WALL);

        this.explodeChance = 0.2f;

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                WitchProdItem.getStartButton(this, Keybindings.keyQ),
                EvokerProdItem.getStartButton(this, Keybindings.keyW),
                ResearchLingeringPotions.getStartButton(this, Keybindings.keyE),
                ResearchEvokers.getStartButton(this, Keybindings.keyR),
                ResearchEvokerVexes.getStartButton(this, Keybindings.keyT)
            );
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
            ArcaneTower.buildingName,
            new ResourceLocation("minecraft", "textures/block/enchanting_table_top.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == ArcaneTower.class,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Blacksmith.buildingName) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            () -> BuildingClientEvents.setBuildingToPlace(ArcaneTower.class),
            null,
            List.of(
                FormattedCharSequence.forward(ArcaneTower.buildingName, Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward("A magical tower that is home to Witches and Evokers", Style.EMPTY)
            ),
            null
        );
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        return super.getIndoorSpawnPoint(level).offset(0,-10,0);
    }
}
