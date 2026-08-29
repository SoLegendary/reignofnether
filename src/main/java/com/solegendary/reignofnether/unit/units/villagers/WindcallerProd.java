package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.building.production.UnitProductionItem;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class WindcallerProd extends ProductionItem implements UnitProductionItem {

    public final static String itemName = "Windcaller";
    public final static ResourceCost cost = ResourceCosts.WINDCALLER;

    public WindcallerProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                boolean hasResearch = ResearchServerEvents.playerHasResearch(placement.ownerName, ProductionItems.RESEARCH_UPGRADED_WINDCALLERS);
                placement.produceUnit(
                        (ServerLevel) level,
                        EntityRegistrar.WINDCALLER_UNIT.get(),
                        placement.ownerName,
                        !hasResearch,
                        hasResearch ? new Vec3i(0,10,0) : new Vec3i(0,0,0)
                );
            }
        };
    }

    public String getItemName() {
        return WindcallerProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/windcaller.png"),
                List.of(
                        Component.translatable("entity.reignofnether.windcaller_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        fcs(""),
                        Component.translatable("entity.reignofnether.windcaller_unit.tooltip1").getVisualOrderText(),
                        Component.translatable("entity.reignofnether.windcaller_unit.tooltip2").getVisualOrderText()
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        List<FormattedCharSequence> tooltipLines = new ArrayList<>(List.of(
                Component.translatable("entity.reignofnether.windcaller_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                fcs(""),
                Component.translatable("entity.reignofnether.windcaller_unit.tooltip1").getVisualOrderText(),
                Component.translatable("entity.reignofnether.windcaller_unit.tooltip2").getVisualOrderText(),
                fcs(""),
                Component.translatable("entity.reignofnether.windcaller_unit.tooltip3").getVisualOrderText()
        ));
        return new StartProductionButton(
            WindcallerProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/windcaller.png"),
            hotkey,
            () -> false,
            () -> BuildingClientEvents.hasFinishedBuilding(Buildings.LIBRARY),
            tooltipLines,
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            WindcallerProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/windcaller.png"),
            prodBuilding,
            this,
            first
        );
    }
}
