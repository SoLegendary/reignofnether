package com.solegendary.reignofnether.building.buildings.neutral;

import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
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

public class Beacon extends ProductionBuilding {

    public final static String buildingName = "Beacon";
    public final static String structureName = "beacon_t0";
    public final static String structureNameT1 = "beacon_t1";
    public final static String structureNameT2 = "beacon_t2";
    public final static String structureNameT3 = "beacon_t3";
    public final static String structureNameT4 = "beacon_t4";
    public final static String structureNameT5 = "beacon_t5";
    public final static ResourceCost cost = ResourceCosts.BEACON;

    public final static int MAX_UPGRADE_LEVEL = 5;

    public static int getTicksToWin(Level level) {
        if (level.isClientSide)
            return (int) (GameruleClient.beaconWinMinutes * 20 * 60);
        else if (level.getServer() != null)
            return level.getServer().getGameRules().getRule(GameRuleRegistrar.BEACON_WIN_MINUTES).get() * 20 * 60;
        return 24000;
    }

    public Beacon() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.BEACON;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png");

        this.buildTimeModifier = 2.0f;

        this.capturable = false;
        this.invulnerable = false;
        this.shouldDestroyOnReset = true;

        this.startingBlockTypes.add(Blocks.CHISELED_STONE_BRICKS);

        this.explodeChance = 0.2f;

        this.abilities.add(new BeaconWealth(), Keybindings.keyQ);
        this.abilities.add(new BeaconHaste(), Keybindings.keyW);
        this.abilities.add(new BeaconRegeneration(), Keybindings.keyE);
        this.abilities.add(new BeaconResistance(), Keybindings.keyR);
        this.abilities.add(new BeaconStrength(), Keybindings.keyT);

        this.productions.add(ProductionItems.RESEARCH_BEACON_LEVEL_1, null);
        this.productions.add(ProductionItems.RESEARCH_BEACON_LEVEL_2, null);
        this.productions.add(ProductionItems.RESEARCH_BEACON_LEVEL_3, null);
        this.productions.add(ProductionItems.RESEARCH_BEACON_LEVEL_4, null);
        this.productions.add(ProductionItems.RESEARCH_BEACON_LEVEL_5, null);
    }

    public ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocksFromNbt(structureName, level);
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new BeaconPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        return new BuildingPlaceButton(
                buildingName,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> TutorialClientEvents.isEnabled() || !GameruleClient.allowBeacons,
                () -> {
                    List<BuildingPlacement> list = new ArrayList<>();
                    for (BuildingPlacement b : BuildingClientEvents.getBuildings()) {
                        if (b instanceof BeaconPlacement) {
                            list.add(b);
                        }
                    }
                    return list.isEmpty() && (
                        BuildingClientEvents.hasFinishedBuilding(Buildings.CASTLE) ||
                        BuildingClientEvents.hasFinishedBuilding(Buildings.STRONGHOLD) ||
                        BuildingClientEvents.hasFinishedBuilding(Buildings.FORTRESS) ||
                        ResearchClient.hasCheat("modifythephasevariance")
                    );
                },
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.beacon"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.beacon.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.beacon.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.beacon.tooltip4"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.beacon.tooltip3"), Style.EMPTY)
                ),
                this
        );
    }

    public int getUpgradeLevel(BuildingPlacement placement) {
        for (BuildingBlock block : placement.getBlocks()) {
            if (block.getBlockState().getBlock() == Blocks.NETHERITE_BLOCK)
                return 5;
            if (block.getBlockState().getBlock() == Blocks.DIAMOND_BLOCK)
                return 4;
            if (block.getBlockState().getBlock() == Blocks.EMERALD_BLOCK)
                return 3;
            if (block.getBlockState().getBlock() == Blocks.GOLD_BLOCK)
                return 2;
            if (block.getBlockState().getBlock() == Blocks.IRON_BLOCK)
                return 1;
        }
        return 0;
    }

    @Override
    public Faction getFaction() {
        return Faction.NONE;
    }
}











