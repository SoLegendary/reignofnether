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
        productions.add(ProductionItems.BRUTE, Keybindings.abilitySlot1);
        productions.add(ProductionItems.HEADHUNTER, Keybindings.abilitySlot2);
        productions.add(ProductionItems.MARAUDER, Keybindings.abilitySlot3);
        productions.add(ProductionItems.HOGLIN, Keybindings.abilitySlot4);
        productions.add(ProductionItems.BLAZE, Keybindings.abilitySlot5);
        productions.add(ProductionItems.WITHER_SKELETON, Keybindings.abilitySlot6);
        productions.add(ProductionItems.MAGMA_CUBE, Keybindings.abilitySlot7);
        productions.add(ProductionItems.GHAST, Keybindings.abilitySlot8);
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }
}