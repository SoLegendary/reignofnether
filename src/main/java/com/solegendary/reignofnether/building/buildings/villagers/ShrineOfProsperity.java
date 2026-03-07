package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ShrineOfProsperity extends ProductionBuilding {

    public final static String buildingName = "Shrine of Prosperity";
    public final static String structureName = "shrine_of_prosperity";
    public final static ResourceCost cost = ResourceCosts.SHRINE_OF_PROSPERITY;

    public ShrineOfProsperity() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.ACACIA_LOG;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/acacia_log_top.png");

        this.startingBlockTypes.add(Blocks.COBBLESTONE);

        this.productions.add(ProductionItems.ROYAL_GUARD, Keybindings.keyQ);
        this.productions.add(ProductionItems.ROYAL_GUARD_REVIVE, Keybindings.keyQ);
        this.productions.add(ProductionItems.ENCHANTER, Keybindings.keyW);
        this.productions.add(ProductionItems.ENCHANTER_REVIVE, Keybindings.keyW);
    }

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        ProductionPlacement pp = (ProductionPlacement) super.createBuildingPlacement(level, pos, rotation, ownerName);
        pp.allowProdWhileBuilding = true;
        return pp;
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/acacia_log_top.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.SHRINE_OF_PROSPERITY,
                () -> (!SandboxClientEvents.isSandboxPlayer() && GameruleClient.allowedHeroes <= 0) || TutorialClientEvents.isEnabled(),
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.TOWN_CENTRE) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        fcs(I18n.get("buildings.villagers.reignofnether.shrine_of_prosperity"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.villagers.reignofnether.shrine_of_prosperity.tooltip1")),
                        fcs(I18n.get("buildings.villagers.reignofnether.shrine_of_prosperity.tooltip2"))
                ),
                this
        );
    }
}
