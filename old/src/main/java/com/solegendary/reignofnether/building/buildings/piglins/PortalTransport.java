package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.ConnectPortal;
import com.solegendary.reignofnether.ability.abilities.DisconnectPortal;
import com.solegendary.reignofnether.ability.abilities.GotoPortal;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
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
        this.icon = new ResourceLocation("minecraft", "textures/block/blue_glazed_terracotta.png");
        this.canSetRallyPoint = false;
        this.startingBlockTypes.add(Blocks.LAPIS_BLOCK);
    }

    @Override
    public PortalPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        PortalPlacement portalPlacement = new PortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
        Ability connectPortal = new ConnectPortal(portalPlacement);
        portalPlacement.getAbilities().add(connectPortal);
        Ability gotoPortal = new GotoPortal(portalPlacement);
        portalPlacement.getAbilities().add(gotoPortal);
        Ability disconnectPortal = new DisconnectPortal(portalPlacement);
        portalPlacement.getAbilities().add(disconnectPortal);
        return portalPlacement;
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 1;
    }
}




















