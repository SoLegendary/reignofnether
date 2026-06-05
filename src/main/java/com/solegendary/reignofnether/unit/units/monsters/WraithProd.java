package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class WraithProd extends ProductionItem {

    public final static String itemName = "Wraith";
    public final static ResourceCost cost = ResourceCosts.WRAITH;

    public WraithProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.WRAITH_UNIT.get(), placement.ownerName, true);
        };
    }

    public String getItemName() {
        return WraithProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/wraith.png"),
                List.of(
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit.tooltip2"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit.tooltip3"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            WraithProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/wraith.png"),
            hotkey,
            () -> false,
            () -> true,
            List.of(
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit.tooltip2"), Style.EMPTY),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.wraith_unit.tooltip3"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            WraithProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/wraith.png"),
            prodBuilding,
            this,
            first
        );
    }
}
