package com.solegendary.reignofnether.building.buildings.piglins;

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

public class BlackstoneBridge extends AbstractBridge {

    public final static String buildingName = "Blackstone Bridge";
    public final static String structureNameOrthogonal = "bridge_blackstone_orthogonal";
    public final static String structureNameDiagonal = "bridge_blackstone_diagonal";
    public final static ResourceCost cost = ResourceCosts.BLACKSTONE_BRIDGE;

    public BlackstoneBridge() {
        super(cost);
        this.name = buildingName;
        this.portraitBlock = Blocks.NETHER_BRICK_FENCE;
        this.icon = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/netherbrick_fence.png");

        this.buildTimeModifier = 1.0f;

        this.startingBlockTypes.add(Blocks.CHISELED_POLISHED_BLACKSTONE);
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
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/netherbrick_fence.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.BLACKSTONE_BRIDGE,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.CENTRAL_PORTAL) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        Component.translatable("buildings.reignofnether.blackstone_bridge").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        ResourceCosts.getFormattedCost(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.blackstone_bridge.tooltip1").getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.blackstone_bridge.tooltip2").getVisualOrderText(),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.blackstone_bridge.tooltip3").getVisualOrderText()
                ),
                this
        );
    }
}
