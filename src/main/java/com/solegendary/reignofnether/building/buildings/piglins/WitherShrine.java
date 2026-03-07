package com.solegendary.reignofnether.building.buildings.piglins;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class WitherShrine extends ProductionBuilding {

    public final static String buildingName = "Wither Shrine";
    public final static String structureName = "wither_shrine";
    public final static ResourceCost cost = ResourceCosts.WITHER_SHRINE;

    public WitherShrine() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.GILDED_BLACKSTONE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gilded_blackstone.png");

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_STAIRS);

        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.RESEARCH_WITHER_CLOUDS, Keybindings.keyQ);
        this.productions.add(ProductionItems.RESEARCH_FIRE_RESISTANCE, Keybindings.keyW);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/gilded_blackstone.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.WITHER_SHRINE,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BASTION) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.wither_shrine"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.wither_shrine.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.wither_shrine.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.wither_shrine.tooltip3"), Style.EMPTY)
            ),
            this
        );
    }
}