package com.solegendary.reignofnether.building.buildings.shared;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.monsters.Mausoleum;
import com.solegendary.reignofnether.building.buildings.villagers.TownCentre;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.researchItems.ResearchResourceCapacity;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class Stockpile extends ProductionBuilding {

    public final static String buildingName = "Stockpile";
    public final static String structureName = "stockpile";
    public final static ResourceCost cost = ResourceCosts.STOCKPILE;
    public ResourceName mostAbundantNearbyResource = ResourceName.NONE;

    public Stockpile(Level level, BlockPos originPos, Rotation rotation, String ownerName) {
        super(level, originPos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, originPos, rotation), false);
        this.name = buildingName;
        this.ownerName = ownerName;
        this.portraitBlock = Blocks.CHEST;
        this.icon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/chest.png");

        this.foodCost = cost.food;
        this.woodCost = cost.wood;
        this.oreCost = cost.ore;
        this.popSupply = cost.population;
        this.canAcceptResources = true;
        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.OAK_LOG);

        this.findMostAbundantNearbyResource();

        if (level.isClientSide()) {
            this.productionButtons = Arrays.asList(
                ResearchResourceCapacity.getStartButton(this, Keybindings.keyQ)
            );
        }
    }

    public static ArrayList<BuildingBlock> getRelativeBlockData(LevelAccessor level) {
        return BuildingBlockData.getBuildingBlocks(structureName, level);
    }

    public static AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
                Stockpile.buildingName,
                new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/chest.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Stockpile.class,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(TownCentre.buildingName) ||
                        BuildingClientEvents.hasFinishedBuilding(Mausoleum.buildingName) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                () -> BuildingClientEvents.setBuildingToPlace(Stockpile.class),
                null,
                List.of(
                        FormattedCharSequence.forward(Stockpile.buildingName, Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward("Storage for units and players to drop off resources", Style.EMPTY)
                ),
                null
        );
    }

    public void findMostAbundantNearbyResource() {

        int nearbyFoodBlocks = 0;
        int nearbyWoodBlocks = 0;
        int nearbyOreBlocks = 0;

        for (ResourceName resourceName : List.of(ResourceName.FOOD, ResourceName.WOOD, ResourceName.ORE)) {

            Predicate<BlockPos> BLOCK_CONDITION = bp -> {
                BlockState bs = getLevel().getBlockState(bp);
                BlockState bsAbove = getLevel().getBlockState(bp.above());
                ResourceSource resBlock = ResourceSources.getFromBlockPos(bp, getLevel());

                // is a valid resource block and meets the target ResourceSource's blockstate condition
                if (resBlock == null || resBlock.resourceName != resourceName ||
                    resBlock.name.equals("Farmland") || resBlock.name.equals("Soul Sand"))
                    return false;
                if (!resBlock.blockStateTest.test(bs))
                    return false;

                    // is not part of a building (unless farming)
                else if (BuildingUtils.isPosInsideAnyBuilding(getLevel().isClientSide(), bp))
                    return false;

                // not covered by solid blocks
                boolean hasClearNeighbour = false;
                for (BlockPos adjBp : List.of(bp.north(), bp.south(), bp.east(), bp.west(), bp.above(), bp.below()))
                    if (ResourceSources.CLEAR_MATERIALS.contains(getLevel().getBlockState(adjBp).getMaterial()))
                        hasClearNeighbour = true;
                if (!hasClearNeighbour)
                    return false;

                return true;
            };

            for (BlockPos bp : BlockPos.withinManhattan(this.centrePos, 10, 5, 10))
                if (BLOCK_CONDITION.test(bp))
                    switch(resourceName) {
                        case FOOD -> nearbyFoodBlocks += 1;
                        case WOOD -> nearbyWoodBlocks += 1;
                        case ORE -> nearbyOreBlocks += 1;
                    }
        }
        if (nearbyFoodBlocks > 0 && nearbyFoodBlocks >= nearbyWoodBlocks && nearbyFoodBlocks >= nearbyOreBlocks)
            this.mostAbundantNearbyResource = ResourceName.FOOD;
        if (nearbyWoodBlocks > 0 && nearbyWoodBlocks >= nearbyFoodBlocks && nearbyWoodBlocks >= nearbyOreBlocks)
            this.mostAbundantNearbyResource = ResourceName.WOOD;
        if (nearbyOreBlocks > 0 && nearbyOreBlocks >= nearbyFoodBlocks && nearbyOreBlocks >= nearbyWoodBlocks)
            this.mostAbundantNearbyResource = ResourceName.ORE;
    }

    // collect items placed manually inside the chests by players
    public void checkAndConsumeChestItems() {
        if (!this.getLevel().isClientSide()) {
            BlockPos textPos = null;
            int food = 0;
            int wood = 0;
            int ore = 0;

            for (BuildingBlock block : blocks) {
                if (block.getBlockState().getBlock() == Blocks.CHEST) {
                    BlockEntity blockEntity = this.getLevel().getBlockEntity(block.getBlockPos());
                    if (blockEntity instanceof ChestBlockEntity chest) {

                        for (int i = 0; i < chest.items.size(); i++) {
                            ResourceSource resource = ResourceSources.getFromItem(chest.getItem(i).getItem());
                            if (resource != null) {
                                int numItems = chest.getItem(i).getCount();
                                food += resource.resourceName == ResourceName.FOOD ? resource.resourceValue * numItems: 0;
                                wood += resource.resourceName == ResourceName.WOOD ? resource.resourceValue * numItems : 0;
                                ore += resource.resourceName == ResourceName.ORE ? resource.resourceValue * numItems : 0;
                                chest.removeItem(i, numItems);
                                textPos = block.getBlockPos().offset(0,-2,0);
                            }
                        }

                    }
                }
            }
            if (food > 0 || wood > 0 || ore > 0) {
                Resources res = new Resources(this.ownerName, food, wood, ore);
                ResourcesServerEvents.addSubtractResources(res);
                ResourcesClientboundPacket.showFloatingText(res, textPos);
            }
        }
    }
}
