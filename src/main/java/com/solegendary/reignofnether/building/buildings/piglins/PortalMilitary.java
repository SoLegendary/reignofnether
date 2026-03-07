package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PortalMilitary extends AbstractPortal {

    public final static String buildingName = "Military Portal";
    public final static String structureName = "portal_military";

    public final static ResourceCost cost = ResourceCosts.BASIC_PORTAL;

    public PortalMilitary() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.RED_GLAZED_TERRACOTTA;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/red_glazed_terracotta.png");
        this.canSetRallyPoint = true;
        productions.add(ProductionItems.BRUTE, Keybindings.keyQ);
        productions.add(ProductionItems.HEADHUNTER, Keybindings.keyW);
        productions.add(ProductionItems.MARAUDER, Keybindings.keyE);
        productions.add(ProductionItems.HOGLIN, Keybindings.keyR);
        productions.add(ProductionItems.BLAZE, Keybindings.keyT);
        productions.add(ProductionItems.WITHER_SKELETON, Keybindings.keyY);
        productions.add(ProductionItems.MAGMA_CUBE, Keybindings.keyU);
        productions.add(ProductionItems.GHAST, Keybindings.keyI);
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }
}