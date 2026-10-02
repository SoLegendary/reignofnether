package com.solegendary.reignofnether.building.buildings.monsters;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.shared.AbstractFarm;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.util.MyRenderer;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class PumpkinFarm extends AbstractFarm {

    public final static String buildingName = "Pumpkin Farm";
    public final static String structureName = "pumpkin_farm";
    public final static ResourceCost cost = ResourceCosts.PUMPKIN_FARM;

    private static final int ICE_CHECK_TICKS_MAX = 100;
    private int ticksToNextIceCheck = ICE_CHECK_TICKS_MAX;

    public PumpkinFarm() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.PUMPKIN;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/pumpkin_side.png");

        this.startingBlockTypes.add(Blocks.DARK_OAK_LOG);

        this.explodeChance = 0;
    }


    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(
                name,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/pumpkin_side.png"),
                hotkey,
                () -> BuildingClientEvents.getBuildingToPlace() == Buildings.PUMPKIN_FARM,
                () -> false,
                () -> BuildingClientEvents.hasFinishedBuilding(Buildings.MAUSOLEUM) ||
                        ResearchClient.hasCheat("modifythephasevariance"),
                List.of(
                        Component.translatable("buildings.reignofnether.pumpkin_farm").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.pumpkin_farm.tooltip1", cost.wood).withStyle(MyRenderer.iconStyle).getVisualOrderText(),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        Component.translatable("buildings.reignofnether.pumpkin_farm.tooltip2").getVisualOrderText(),
                        Component.translatable("buildings.reignofnether.pumpkin_farm.tooltip3").getVisualOrderText()
                ),
                this
        );
    }
}
