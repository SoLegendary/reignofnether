package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class PortalPocket extends AbstractPortal {

    public final static String buildingName = "Pocket Portal";
    public final static String structureName = "pocket_portal";

    public final static ResourceCost cost = ResourceCosts.POCKET_PORTAL;

    public PortalPocket() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.GRAY_GLAZED_TERRACOTTA;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gray_glazed_terracotta.png");
        this.canSetRallyPoint = false;
        this.maxHealth = 50f;
        this.buildTimeModifier = 2.0f;
        this.startingBlockTypes.add(Blocks.OBSIDIAN);
        this.startingBlockTypes.add(Blocks.GRAY_GLAZED_TERRACOTTA);
    }

    @Override
    public PortalPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        PortalPlacement pp = new PortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
        pp.selfBuilding = true;
        return pp;
    }

    @Override
    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/portal.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.PORTAL_POCKET,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.CENTRAL_PORTAL) || ResearchClient.hasCheat(
                "modifythephasevariance"),
            List.of(fcs(I18n.get("buildings.reignofnether.portal_pocket"), true),
                ResourceCosts.getFormattedCost(cost),
                fcs(""),
                fcs(I18n.get("buildings.reignofnether.portal_pocket.tooltip1")),
                fcs(I18n.get("buildings.reignofnether.portal_pocket.tooltip2"))
            ),
            this
        );
    }
}