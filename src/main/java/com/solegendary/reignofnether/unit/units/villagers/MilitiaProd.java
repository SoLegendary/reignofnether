package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class MilitiaProd extends ProductionItem {

    public final static String itemName = "Militia";
    public final static ResourceCost cost = ResourceCosts.MILITIA;

    public MilitiaProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.MILITIA_UNIT.get(), placement.ownerName, true);
        };
    }

    public String getItemName() {
        return MilitiaProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
            "Militia",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/militia.png"),
            List.of(
                    FormattedCharSequence.forward(I18n.get("entity.reignofnether.militia_unit"), Style.EMPTY.withBold(true)),
                    FormattedCharSequence.forward("", Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("entity.reignofnether.militia_unit.tooltip1"), Style.EMPTY),
                    FormattedCharSequence.forward(I18n.get("entity.reignofnether.militia_unit.tooltip2"), Style.EMPTY)
            )
        );
    }
    
    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            MilitiaProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/militia.png"),
            hotkey,
            () -> false,
            () -> true,
            List.of(
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.militia_unit"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.militia_unit.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.militia_unit.tooltip2"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            MilitiaProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/militia.png"),
            prodBuilding,
            this,
            first
        );
    }
}
