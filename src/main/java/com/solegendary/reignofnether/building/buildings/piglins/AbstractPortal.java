package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public abstract class AbstractPortal extends ProductionBuilding {

    public final static String buildingName = "Basic Portal";
    public final static String structureName = "portal_basic";

    public AbstractPortal(String structureName, ResourceCost cost) {
        super(structureName, cost, false);
        this.name = buildingName;
        this.buildTimeModifier = 1.2f;
        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);
        this.canSetRallyPoint = false;
    }

    public Faction getFaction() {
        return Faction.PIGLINS;
    }

    @Override
    public int getUpgradeLevel(BuildingPlacement placement) {
        return 0;
    }

    @Override
    public PortalPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new PortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    // Only basic portals actually have a build button but include this here as a placeholder
    @Override
    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        return new BuildingPlaceButton(name,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
                hotkey,
                () -> false,
                () -> false,
                () -> false,
                List.of(),
                this
        );
    }

    @Override
    public boolean isTypeOf(Building building) {
        return super.isTypeOf(building) || building == Buildings.PORTAL_BASIC;
    }
}