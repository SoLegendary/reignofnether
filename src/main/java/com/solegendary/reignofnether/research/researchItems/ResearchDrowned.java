package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProdDupeRule;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResearchDrowned extends ProductionItem {

    public final static String itemName = "Drowned Zombies";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_DROWNED;

    public ResearchDrowned() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_DROWNED);

                // convert all zombies into drowned with the same stats/inventory/etc.
                UnitServerEvents.convertAllToUnit(
                        placement.ownerName,
                    (ServerLevel) level,
                    (LivingEntity entity) -> entity instanceof ZombieUnit zUnit &&
                            zUnit.getOwnerName().equals(placement.ownerName) &&
                            !zUnit.isSummoned(),
                    EntityRegistrar.DROWNED_UNIT.get()
                );
            }
        };
    }

    public String getItemName() {
        return ResearchDrowned.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            ResearchDrowned.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> ProductionItems.RESEARCH_DROWNED.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED),
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.GRAVEYARD),
            List.of(
                fcs(I18n.get("research.reignofnether.research_drowned"), true),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                fcs(""),
                fcs(I18n.get("research.reignofnether.research_drowned.tooltip1")),
                fcs(I18n.get("research.reignofnether.research_drowned.tooltip2")),
                fcs(""),
                fcs(I18n.get("research.reignofnether.research_drowned.tooltip3"))
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            ResearchDrowned.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
