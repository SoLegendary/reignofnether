package com.solegendary.reignofnether.research.researchItems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ResearchBogged extends ProductionItem {

    public final static String itemName = "Bogged Skeletons";
    public final static ResourceCost cost = ResourceCosts.RESEARCH_BOGGED;

    public ResearchBogged() {
        super(cost, ProdDupeRule.DISALLOW);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                ResearchServerEvents.addResearch(placement.ownerName, ProductionItems.RESEARCH_BOGGED);

                // convert all skeletons into bogged with the same stats/inventory/etc.
                /*
                UnitServerEvents.convertAllToUnit(
                        placement.ownerName,
                    (ServerLevel) level,
                    (LivingEntity entity) -> entity instanceof SkeletonUnit sUnit && sUnit.getOwnerName()
                        .equals(placement.ownerName),
                    EntityRegistrar.BOGGED_UNIT.get()
                );
                 */
            }
        };
    }

    public String getItemName() {
        return ResearchBogged.itemName;
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            ResearchBogged.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/bogged.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            hotkey,
            () -> ProductionItems.RESEARCH_BOGGED.itemIsBeingProduced(prodBuilding.ownerName)
                || ResearchClient.hasResearch(ProductionItems.RESEARCH_BOGGED),
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.GRAVEYARD),
            List.of(
                fcs(I18n.get("research.reignofnether.research_bogged"), true),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedTime(cost),
                fcs(""),
                fcs(I18n.get("research.reignofnether.research_bogged.tooltip1")),
                fcs(I18n.get("research.reignofnether.research_bogged.tooltip2")),
                fcs(""),
                fcs(I18n.get("research.reignofnether.research_bogged.tooltip3"))
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            ResearchBogged.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/bogged.png"),
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_bronze.png"),
            prodBuilding,
            this,
            first
        );
    }
}
