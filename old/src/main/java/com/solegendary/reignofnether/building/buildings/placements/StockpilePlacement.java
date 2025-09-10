package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingBlock;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.resources.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class StockpilePlacement extends ProductionPlacement {
    public ResourceName mostAbundantNearbyResource = ResourceName.NONE;
    public StockpilePlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
        this.findMostAbundantNearbyResource();
        findMostAbundantNearbyResource();
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
                    if (ResourceSources.isClearMaterial(getLevel().getBlockState(adjBp)))
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
        if (!getLevel().isClientSide()) {
            BlockPos textPos = null;
            int food = 0;
            int wood = 0;
            int ore = 0;

            for (BuildingBlock block : getBlocks()) {
                if (block.getBlockState().getBlock() == Blocks.CHEST) {
                    BlockEntity blockEntity = getLevel().getBlockEntity(block.getBlockPos());
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
                Resources res = new Resources(ownerName, food, wood, ore);
                ResourcesServerEvents.addSubtractResources(res);
                ResourcesClientboundPacket.showFloatingText(res, textPos);
            }
        }
    }
}
