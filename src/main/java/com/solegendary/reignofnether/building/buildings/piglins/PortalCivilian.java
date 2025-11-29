package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PortalCivilian extends AbstractPortal {

    public final static String buildingName = "Civilian Portal";
    public final static String structureName = "portal_civilian";

    public final static ResourceCost cost = ResourceCosts.CIVILIAN_PORTAL;

    public PortalCivilian() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.CYAN_GLAZED_TERRACOTTA;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cyan_glazed_terracotta.png");
        this.canSetRallyPoint = false;
        this.canAcceptResources = true;
        productions.add(ProductionItems.RESEARCH_RESOURCE_CAPACITY, Keybindings.keyQ);
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }
}