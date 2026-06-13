package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.shared.AbstractMarket;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class MonsterMarket extends AbstractMarket {

    public static final String buildingName = "Conversion Crucible";
    public static final String structureName = "market_monsters";
    public static final ResourceCost cost = ResourceCosts.MONSTER_MARKET;

    public MonsterMarket() {
        super(structureName, cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.POLISHED_DEEPSLATE;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_deepslate.png");

        this.startingBlockTypes.add(Blocks.POLISHED_DEEPSLATE);
        this.startingBlockTypes.add(Blocks.POLISHED_BLACKSTONE_BRICKS);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_TILES);
        this.startingBlockTypes.add(Blocks.SMOOTH_BASALT);
        this.startingBlockTypes.add(Blocks.POLISHED_BASALT);
        this.startingBlockTypes.add(Blocks.DEEPSLATE_TILE_SLAB);
        this.startingBlockTypes.add(Blocks.POLISHED_DEEPSLATE_SLAB);
        this.startingBlockTypes.add(Blocks.DARK_PRISMARINE_SLAB);
    }

    public Faction getFaction() { return Faction.MONSTERS; }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/polished_deepslate.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == this,
                () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.EXPLAIN_BUILDINGS),
                () -> BuildingClientEvents.numFinishedBuildings(Buildings.SCULK_CATALYST) >= 5 ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        fcs(I18n.get("buildings.reignofnether.monster_market"), true),
                        ResourceCosts.getFormattedCost(cost),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.monster_market.tooltip1")),
                        fcs(""),
                        fcs(I18n.get("buildings.reignofnether.monster_market.tooltip2"))
                ),
                this
        );
    }
}