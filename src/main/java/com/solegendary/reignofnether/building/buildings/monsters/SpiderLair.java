package com.solegendary.reignofnether.building.buildings.monsters;

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

public class SpiderLair extends ProductionBuilding {

    public final static String buildingName = "Spider Lair";
    public final static String structureName = "spider_lair";
    public final static ResourceCost cost = ResourceCosts.SPIDER_LAIR;

    public SpiderLair() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.COBWEB;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobweb.png");

        this.startingBlockTypes.add(Blocks.DEEPSLATE);
        this.startingBlockTypes.add(Blocks.COBBLED_DEEPSLATE);

        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.SPIDER, Keybindings.keyQ);
        this.productions.add(ProductionItems.POISON_SPIDER, Keybindings.keyW);
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/cobweb.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.SPIDER_LAIR,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.GRAVEYARD) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.spider_lair"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.spider_lair.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.spider_lair.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }
}
