package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlaceButton;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.IronGolemProd;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class IronGolemBuilding extends Building {

    public final static String buildingName = "Iron Golem";
    public final static String structureName = "iron_golem";
    public final static ResourceCost cost = ResourceCosts.IRON_GOLEM_BUILDING;

    public IronGolemBuilding() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.IRON_BLOCK;
        this.icon = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");

        this.buildTimeModifier = 3.4f;

        this.startingBlockTypes.add(Blocks.JUNGLE_FENCE);
    }

    

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = key != null ? Component.translatable("buildings." + getFaction().getName() + "." + key.getNamespace() + "." + key.getPath()).getString() : buildingName;
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.IRON_GOLEM_BUILDING,
            TutorialClientEvents::isEnabled,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BLACKSMITH) && (
                ResearchClient.hasResearch(ProductionItems.RESEARCH_GOLEM_SMITHING) || ResearchClient.hasCheat(
                    "modifythephasevariance")
            ),
            List.of(Component.translatable("buildings.reignofnether.iron_golem_building").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(IronGolemProd.cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.iron_golem_building.tooltip1").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("buildings.reignofnether.iron_golem_building.tooltip2").getVisualOrderText()
            ),
            this
        );
    }

    @Override
    public void onBuilt(BuildingPlacement placement) {
        if (!placement.getLevel().isClientSide()) {
            placement.destroy((ServerLevel) placement.getLevel());
            Entity entity = EntityRegistrar.IRON_GOLEM_UNIT.get().spawn((ServerLevel) placement.getLevel(),
                    (CompoundTag) null,
                    null,
                    placement.centrePos.offset(0, -1, 0),
                    MobSpawnType.SPAWNER,
                    true,
                    false
            );
            if (entity instanceof Unit unit) {
                unit.setOwnerName(placement.ownerName);
            }
        }
    }
}
