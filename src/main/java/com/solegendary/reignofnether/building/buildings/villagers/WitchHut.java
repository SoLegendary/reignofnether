package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.production.ProductionItems;
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
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class WitchHut extends ProductionBuilding {

    public final static String buildingName = "Witch Hut";
    public final static String structureName = "witch_hut";
    public final static ResourceCost cost = ResourceCosts.WITCH_HUT;

    public WitchHut() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.STRIPPED_CRIMSON_STEM;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stripped_crimson_stem_top.png");

        this.startingBlockTypes.add(Blocks.SPRUCE_STAIRS);
        this.startingBlockTypes.add(Blocks.STRIPPED_SPRUCE_WOOD);

        this.buildTimeModifier = 0.7f;
        this.explodeChance = 0.2f;

        this.productions.add(ProductionItems.WITCH, Keybindings.abilitySlot1);
        this.productions.add(ProductionItems.RESEARCH_LINGERING_POTIONS, Keybindings.abilitySlot2);
        this.productions.add(ProductionItems.RESEARCH_HEALING_POTIONS, Keybindings.abilitySlot3);
        this.productions.add(ProductionItems.RESEARCH_WATER_POTIONS, Keybindings.abilitySlot4);
    }

    public Faction getFaction() {return Faction.VILLAGERS;}

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/stripped_crimson_stem_top.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.WITCH_HUT,
                TutorialClientEvents::isEnabled,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BARRACKS) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.witch_hut"), Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.witch_hut.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.witch_hut.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("buildings.villagers.reignofnether.witch_hut.tooltip3"), Style.EMPTY)
                ),
                this
        );
    }

    @Override
    public BlockPos getIndoorSpawnPoint(ServerLevel level, BuildingPlacement placement) {
        return super.getIndoorSpawnPoint(level, placement).offset(0,0,0);
    }
}
