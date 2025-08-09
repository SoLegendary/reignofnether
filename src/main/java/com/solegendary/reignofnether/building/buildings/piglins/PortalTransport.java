package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class PortalTransport extends AbstractPortal {

    public final static String buildingName = "Transport Portal";
    public final static String structureName = "portal_transport";

    public final static ResourceCost cost = ResourceCosts.BASIC_PORTAL;

    public PortalTransport() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.BLUE_GLAZED_TERRACOTTA;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/blue_glazed_terracotta.png");
        this.canSetRallyPoint = false;
        this.startingBlockTypes.add(Blocks.LAPIS_BLOCK);
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }

    @Override
    public boolean isBuildableBuildingForFaction(Faction faction) {
        return false;
    }
}