package com.solegendary.reignofnether.building.buildings.villagers;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.villagers.IronGolemProd;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

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

    public Faction getFaction() {
        return Faction.VILLAGERS;
    }

    public BuildingPlaceButton getBuildButton(Keybinding hotkey) {
        ResourceLocation key = ReignOfNetherRegistries.BUILDING.getKey(this);
        String name = I18n.get("buildings." + getFaction().name().toLowerCase() + "." + key.getNamespace() + "." + key.getPath());
        return new BuildingPlaceButton(name,
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == Buildings.IRON_GOLEM_BUILDING,
            TutorialClientEvents::isEnabled,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.BLACKSMITH) && (
                ResearchClient.hasResearch(ProductionItems.RESEARCH_GOLEM_SMITHING) || ResearchClient.hasCheat(
                    "modifythephasevariance")
            ),
            List.of(FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.iron_golem_building"),
                    Style.EMPTY.withBold(true)
                ),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(IronGolemProd.cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.iron_golem_building.tooltip1"),
                    Style.EMPTY
                ),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(
                    I18n.get("buildings.villagers.reignofnether.iron_golem_building.tooltip2"),
                    Style.EMPTY
                )
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
