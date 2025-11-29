package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.shared.AbstractBridge;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class OakBridge extends AbstractBridge {

    public final static String buildingName = "Oak Bridge";
    public final static String structureNameOrthogonal = "bridge_oak_orthogonal";
    public final static String structureNameDiagonal = "bridge_oak_diagonal";
    public final static ResourceCost cost = ResourceCosts.OAK_BRIDGE;

    public OakBridge() {
        super(cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.OAK_FENCE;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/oak_fence.png");

        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.OAK_LOG);
    }

    @Override
    public String getDiagonalStructureName() {
        return structureNameDiagonal;
    }

    @Override
    public String getOrthogonalStructureName() {
        return structureNameOrthogonal;
    }

    @Override
    public Faction getFaction() {
        return Faction.VILLAGERS;
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/oak_fence.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.OAK_BRIDGE,
            () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.BUILD_BRIDGE),
            () -> TutorialClientEvents.isAtOrPastStage(TutorialStage.BUILD_BRIDGE) && (
                BuildingClientEvents.hasFinishedBuilding(Buildings.TOWN_CENTRE)
                    || BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) || ResearchClient.hasCheat(
                    "modifythephasevariance")
            ),
            List.of(FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.oak_bridge"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.oak_bridge.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.oak_bridge.tooltip2"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.oak_bridge.tooltip3"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.oak_bridge.tooltip4"),
                    Style.EMPTY
                )
            ),
            this
        );
    }
}
