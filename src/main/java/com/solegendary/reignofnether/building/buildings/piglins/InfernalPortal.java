package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class InfernalPortal extends ProductionBuilding {

    public final static String buildingName = "Infernal Portal";
    public final static String structureName = "infernal_portal";
    public final static ResourceCost cost = ResourceCosts.INFERNAL_PORTAL;

    public InfernalPortal() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.CRYING_OBSIDIAN;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crying_obsidian.png");

        this.startingBlockTypes.add(Blocks.NETHER_BRICKS);

        this.productions.add(ProductionItems.PIGLIN_MERCHANT, Keybindings.keyQ);
        this.productions.add(ProductionItems.PIGLIN_MERCHANT_REVIVE, Keybindings.keyQ);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    @Override
    public PortalPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        PortalPlacement pp = new PortalPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
        pp.allowProdWhileBuilding = true;
        return pp;
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crying_obsidian.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.INFERNAL_PORTAL,
                () -> !SandboxClientEvents.isSandboxPlayer() && !GameruleClient.allowHeroes,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.CENTRAL_PORTAL) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        fcs(I18n.get("buildings.piglins.reignofnether.infernal_portal"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.piglins.reignofnether.infernal_portal.tooltip1")),
                        fcs(I18n.get("buildings.piglins.reignofnether.infernal_portal.tooltip2"))
                ),
                this
        );
    }
}