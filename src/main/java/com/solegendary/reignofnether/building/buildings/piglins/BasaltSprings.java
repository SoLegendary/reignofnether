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

public class BasaltSprings extends ProductionBuilding {

    public final static String buildingName = "Basalt Springs";
    public final static String structureName = "basalt_springs";
    public final static ResourceCost cost = ResourceCosts.BASALT_SPRINGS;

    public BasaltSprings() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.POLISHED_BASALT;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_basalt_top.png");

        this.canSetRallyPoint = false;

        this.startingBlockTypes.add(Blocks.BASALT);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE);

        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.RESEARCH_CUBE_MAGMA, Keybindings.keyQ);
    }

    public Faction getFaction() {return Faction.PIGLINS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
            name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_basalt_top.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.BASALT_SPRINGS,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BASTION) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            List.of(
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.basalt_springs"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.basalt_springs.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.basalt_springs.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.piglins.reignofnether.basalt_springs.tooltip3"), Style.EMPTY)
            ),
            this
        );
    }
}
