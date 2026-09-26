package com.solegendary.reignofnether.building.buildings.monsters;

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

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class SpruceBridge extends AbstractBridge {

    public final static String buildingName = "Spruce Bridge";
    public final static String structureNameOrthogonal = "bridge_spruce_orthogonal";
    public final static String structureNameDiagonal = "bridge_spruce_diagonal";
    public final static ResourceCost cost = ResourceCosts.SPRUCE_BRIDGE;

    public SpruceBridge() {
        super(cost);

        this.name = buildingName;
        this.portraitBlock = Blocks.DARK_OAK_FENCE;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/spruce_fence.png");

        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.SPRUCE_LOG);
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
    

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        Minecraft MC = Minecraft.getInstance();
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/spruce_fence.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.SPRUCE_BRIDGE,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.TOWN_CENTRE) ||
                        BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        Component.translatable("buildings.reignofnether.spruce_bridge").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.spruce_bridge.tooltip1").getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.spruce_bridge.tooltip2").getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.spruce_bridge.tooltip3").getVisualOrderText(),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.spruce_bridge.tooltip4").getVisualOrderText()
                ),
                this
        );
    }
}
