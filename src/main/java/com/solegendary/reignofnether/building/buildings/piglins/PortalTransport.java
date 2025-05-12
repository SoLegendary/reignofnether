package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.ConnectPortal;
import com.solegendary.reignofnether.ability.abilities.DisconnectPortal;
import com.solegendary.reignofnether.ability.abilities.GotoPortal;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

public class PortalTransport extends AbstractPortal {

    public final static String buildingName = "Transport Portal";
    public final static String structureName = "portal_transport";

    public final static ResourceCost cost = ResourceCosts.BASIC_PORTAL;

    public PortalTransport() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.BLUE_GLAZED_TERRACOTTA;
        this.icon = new ResourceLocation("minecraft", "textures/block/blue_glazed_terracotta.png");
        this.canSetRallyPoint = false;
        this.startingBlockTypes.add(Blocks.LAPIS_BLOCK);

        Ability connectPortal = new ConnectPortal();
        this.abilities.add(connectPortal, Keybindings.keyQ);
        Ability gotoPortal = new GotoPortal();
        this.abilities.add(gotoPortal, Keybindings.keyW);
        Ability disconnectPortal = new DisconnectPortal();
        this.abilities.add(disconnectPortal, Keybindings.keyE);
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }
}




















