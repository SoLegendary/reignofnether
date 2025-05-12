package com.solegendary.reignofnether.building.buildings.placements;

import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.piglins.PortalCivilian;
import com.solegendary.reignofnether.building.buildings.piglins.PortalMilitary;
import com.solegendary.reignofnether.building.buildings.piglins.PortalTransport;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class PortalPlacement extends ProductionPlacement implements NetherConvertingBuilding {
    public enum PortalType {
        BASIC, CIVILIAN, MILITARY, TRANSPORT
    }

    public final static float NON_NETHER_BUILD_TIME_MODIFIER = 2.0f;

    public BlockPos destination; // for transport portals

    public NetherZone netherConversionZone = null;

    public PortalPlacement(Building building, Level level, BlockPos originPos, Rotation rotation, String ownerName, ArrayList<BuildingBlock> blocks, boolean isCapitol) {
        super(building, level, originPos, rotation, ownerName, blocks, isCapitol);
    }

    public PortalType getPortalType() {
        if (building instanceof PortalCivilian)
            return PortalType.CIVILIAN;
        else if (building instanceof PortalMilitary)
            return PortalType.MILITARY;
        else if (building instanceof PortalTransport)
            return PortalType.TRANSPORT;
        return PortalType.BASIC;
    }

    @Override
    public void tick(Level tickLevel) {
        super.tick(tickLevel);

        if (!this.getLevel().isClientSide() && this.getBlocksPlaced() >= getBlocksTotal() && this.getLevel()
                .getBlockState(this.centrePos)
                .isAir()) {
            this.getLevel().setBlockAndUpdate(this.centrePos, Blocks.FIRE.defaultBlockState());
        }
    }

    @Override
    public boolean shouldBeDestroyed() {
        boolean shouldBeDestroyed = super.shouldBeDestroyed();
        if (shouldBeDestroyed) {
            disconnectPortal();
        }
        return shouldBeDestroyed;
    }

    @Override
    public void destroy(ServerLevel serverLevel) {
        disconnectPortal();
        super.destroy(serverLevel);
    }

    @Override
    public void setNetherZone(NetherZone nz) {
        if (netherConversionZone == null) {
            netherConversionZone = nz;
            if (!level.isClientSide()) {
                BuildingServerEvents.netherZones.add(netherConversionZone);
                BuildingServerEvents.saveNetherZones((ServerLevel) level);
            }
        }
    }

    @Override
    public void onBuilt() {
        super.onBuilt();
        setNetherZone(new NetherZone(centrePos.offset(0, -2, 0), getMaxRange(), getStartingRange()));
    }

    public void disconnectPortal() {
        if (destination != null) {
            BuildingPlacement targetBuilding = BuildingUtils.findBuilding(getLevel().isClientSide(), destination);
            if (targetBuilding instanceof PortalPlacement targetPortal && getBuilding() instanceof PortalTransport) {
                targetPortal.destination = null;
            }
        }
        destination = null;
    }

    @Override
    public boolean canDestroyBlock(BlockPos relativeBp) {
        BlockPos worldBp = relativeBp.offset(this.originPos);
        Block block = this.getLevel().getBlockState(worldBp).getBlock();
        return block != Blocks.OBSIDIAN && block != Blocks.NETHER_PORTAL;
    }

    public void changeStructure(PortalType portalType) {
        String newStructureName = "";
        switch (portalType) {
            case CIVILIAN -> {
                this.building = Buildings.PORTAL_CIVILIAN;
                newStructureName = PortalCivilian.structureName;
                if (this.getLevel().isClientSide()) {
                    this.productionButtons = List.of(ProductionItems.RESEARCH_RESOURCE_CAPACITY.getStartButton(this, Keybindings.keyQ));
                }
            }
            case MILITARY -> {
                this.building = Buildings.PORTAL_MILITARY;
                newStructureName = PortalMilitary.structureName;
                if (this.getLevel().isClientSide()) {
                    this.productionButtons = Arrays.asList(ProductionItems.BRUTE.getStartButton(this, Keybindings.keyQ),
                            ProductionItems.HEADHUNTER.getStartButton(this, Keybindings.keyW),
                            ProductionItems.HOGLIN.getStartButton(this, Keybindings.keyE),
                            ProductionItems.BLAZE.getStartButton(this, Keybindings.keyR),
                            ProductionItems.WITHER_SKELETON.getStartButton(this, Keybindings.keyT),
                            ProductionItems.MAGMA_CUBE.getStartButton(this, Keybindings.keyY),
                            ProductionItems.GHAST.getStartButton(this, Keybindings.keyU)
                    );
                }
            }
            case TRANSPORT -> {
                this.building = Buildings.PORTAL_TRANSPORT;
                newStructureName = PortalTransport.structureName;
            }
        }
        if (!newStructureName.isEmpty()) {
            ArrayList<BuildingBlock> newBlocks = BuildingBlockData.getBuildingBlocks(newStructureName, this.getLevel());
            this.blocks = getAbsoluteBlockData(newBlocks, this.getLevel(), originPos, rotation);
            super.refreshBlocks();
        }
    }

    public double getMaxRange() { return 20; }

    public double getStartingRange() {
        return 3;
    }

    @Override
    public NetherZone getZone() {
        return netherConversionZone;
    }

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

    public boolean hasDestination() {
        return isBuilt &&
                getBuilding() instanceof PortalTransport &&
                destination != null &&
                !destination.equals(new BlockPos(0,0,0));
    }
}
