package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.Dungeon;
import com.solegendary.reignofnether.building.buildings.monsters.Graveyard;
import com.solegendary.reignofnether.building.buildings.monsters.SpiderLair;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchAdvancedPortals;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.units.monsters.WardenProd;
import com.solegendary.reignofnether.util.Faction;
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

public class Fortress extends ProductionBuilding implements GarrisonableBuilding {

    public final static String buildingName = "Fortress";
    public final static String structureName = "stronghold";
    public final static ResourceCost cost = ResourceCosts.FORTRESS;

    private final static int MAX_OCCUPANTS = 10;

    public Fortress(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.CHISELED_NETHER_BRICKS;
        this.icon = new ResourceLocation("minecraft", "textures/block/chiseled_nether_bricks.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.buildTimeModifier = 0.5f;

        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_TILE_SLAB);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_WALL);
        this.startingBlockTypes.add(Blocks.DEEPSLATE);

        if (level.isClientSide())
            this.productionButtons = Arrays.asList(
                ResearchAdvancedPortals.getStartButton(this, Keybindings.keyQ)
            );
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    // don't use this for abilities as it may not be balanced
    public int getAttackRange() { return 27; }
    // bonus for units attacking garrisoned units
    public int getExternalAttackRangeBonus() { return 15; }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public boolean canDestroyBlock(BlockPos relativeBp) {
        return relativeBp.getY() != 13 &&
                relativeBp.getY() != 14;
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
            Fortress.buildingName,
            new ResourceLocation("minecraft", "textures/block/chiseled_nether_bricks.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Fortress.class,
            () -> false,
            () -> (BuildingClientEvents.hasFinishedBuilding(FlameSanctuary.buildingName) &&
                    BuildingClientEvents.hasFinishedBuilding(WitherShrine.buildingName)) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            () -> BuildingClientEvents.setBuildingToPlace(Fortress.class),
            null,
            List.of(
                    FormattedCharSequence.forward(Fortress.buildingName, Style.EMPTY.withBold(true)),
                    ResourceCosts.getFormattedCost(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("An imposing nether fortress that allows military portals", Style.EMPTY),
                    FormattedCharSequence.forward("to produce ghasts and garrisons up to " + MAX_OCCUPANTS + " units.", Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward("Requires a Flame Sanctuary and a Wither Shrine.", Style.EMPTY)
            ),
            null
        );
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level) {
        return this.originPos.offset(getExitPosition());
    }

    @Override
    public BlockPos getEntryPosition() {
        return new BlockPos(5,14,5);
    }

    @Override
    public BlockPos getExitPosition() {
        return new BlockPos(5,2,6);
    }

    @Override
    public boolean isFull() { return GarrisonableBuilding.getNumOccupants(this) >= MAX_OCCUPANTS; }
}
