package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class HauntedHouse extends Building {

    public final static String buildingName = "Haunted House";
    public final static String structureName = "haunted_house";
    public final static ResourceCost cost = ResourceCosts.HAUNTED_HOUSE;

    public HauntedHouse() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.DARK_OAK_LOG;
        this.icon = new ResourceLocation("minecraft", "textures/block/dark_oak_log.png");

        this.buildTimeModifier = 0.8f;

        this.startingBlockTypes.add(Blocks.SPRUCE_PLANKS);
        this.startingBlockTypes.add(Blocks.DARK_OAK_LOG);
    }

    public Faction getFaction() {return Faction.MONSTERS;}

    public AbilityButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new AbilityButton(
            name,
            new ResourceLocation("minecraft", "textures/block/dark_oak_log.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.HAUNTED_HOUSE,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                    ResearchClient.hasCheat("modifythephasevariance"),
            () -> BuildingClientEvents.setBuildingToPlace(Buildings.HAUNTED_HOUSE),
            null,
            List.of(
                    FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.haunted_house"), Style.EMPTY.withBold(true)),
                    ResourceCosts.getFormattedCost(cost),
                    ResourceCosts.getFormattedPop(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("buildings.monsters.reignofnether.haunted_house.tooltip1"), Style.EMPTY)
            ),
            null
        );
    }
}
