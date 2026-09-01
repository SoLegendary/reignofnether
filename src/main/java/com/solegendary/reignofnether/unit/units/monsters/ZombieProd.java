package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.GraveyardPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.*;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class ZombieProd extends GraveyardUnitProductionItem {

    public final static String itemName = "Zombie";
    public final static ResourceCost cost = ResourceCosts.ZOMBIE;

    public ZombieProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement building) -> {
            if (!level.isClientSide()) {
                if (building instanceof GraveyardPlacement gy && building.getUpgradeLevel() > 0) {
                    if (ResearchServerEvents.playerHasResearch(building.ownerName, ProductionItems.RESEARCH_HUSKS))
                        gy.createSkull(EntityRegistrar.HUSK_UNIT.get());
                    else if (ResearchServerEvents.playerHasResearch(building.ownerName, ProductionItems.RESEARCH_DROWNED))
                        gy.createSkull(EntityRegistrar.DROWNED_UNIT.get());
                    else
                        gy.createSkull(EntityRegistrar.ZOMBIE_UNIT.get());
                } else {
                    if (ResearchServerEvents.playerHasResearch(building.ownerName, ProductionItems.RESEARCH_HUSKS) && !(building instanceof CustomBuildingPlacement))
                        building.produceUnit((ServerLevel) level, EntityRegistrar.HUSK_UNIT.get(), building.ownerName, true);
                    else if (ResearchServerEvents.playerHasResearch(building.ownerName, ProductionItems.RESEARCH_DROWNED) && !(building instanceof CustomBuildingPlacement))
                        building.produceUnit((ServerLevel) level, EntityRegistrar.DROWNED_UNIT.get(), building.ownerName, true);
                    else
                        building.produceUnit((ServerLevel) level, EntityRegistrar.ZOMBIE_UNIT.get(), building.ownerName, true);
                }
            }
        };
    }

    public String getItemName() {
        return ZombieProd.itemName;
    }

    private static ResourceLocation getIcon(ProductionPlacement prodBuilding) {
        if (ResearchClient.hasResearch(ProductionItems.RESEARCH_HUSKS) && !(prodBuilding instanceof CustomBuildingPlacement))
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/husk.png");
        else if (ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED) && !(prodBuilding instanceof CustomBuildingPlacement))
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/drowned.png");
        else
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/zombie.png");
    }

    private static String getCancelName(ProductionPlacement prodBuilding) {
        if (ResearchClient.hasResearch(ProductionItems.RESEARCH_HUSKS) && !(prodBuilding instanceof CustomBuildingPlacement))
            return "Husk";
        else if (ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED) && !(prodBuilding instanceof CustomBuildingPlacement))
            return "Drowned";
        else
            return "Zombie";
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                ZombieProd.itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/zombie.png"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.zombie_unit"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.zombie_unit.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.zombie_unit.tooltip2"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                ZombieProd.itemName,
                getIcon(prodBuilding),
                hotkey,
                () -> !(prodBuilding instanceof CustomBuildingPlacement) &&
                        (ResearchClient.hasResearch(ProductionItems.RESEARCH_HUSKS) || ResearchClient.hasResearch(ProductionItems.RESEARCH_DROWNED)),
                () -> true,
                List.of(
                    FormattedCharSequence.forward(I18n.get("entity.reignofnether.zombie_unit"), Style.EMPTY.withBold(true)),
                    ResourceCosts.getFormattedCost(cost),
                    ResourceCosts.getFormattedPopAndTime(cost),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("entity.reignofnether.zombie_unit.tooltip1"), Style.EMPTY),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("entity.reignofnether.zombie_unit.tooltip2"), Style.EMPTY)
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            getCancelName(prodBuilding),
            getIcon(prodBuilding),
            prodBuilding,
            this,
            first
        );
    }
}
