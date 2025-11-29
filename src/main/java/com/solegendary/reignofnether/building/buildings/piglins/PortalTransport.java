package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ability.abilities.ConnectPortal;
import com.solegendary.reignofnether.ability.abilities.DisconnectPortal;
import com.solegendary.reignofnether.ability.abilities.GotoPortal;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

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

        this.abilities.add(new ConnectPortal(), Keybindings.keyQ);
        this.abilities.add(new GotoPortal(), Keybindings.keyW);
        this.abilities.add(new DisconnectPortal(), Keybindings.keyE);
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }
}