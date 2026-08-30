package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class BatProd extends ProductionItem {

    public final static String itemName = "Bat";
    public final static ResourceCost cost = ResourceCosts.BAT;

    private final static ResourceLocation TEXTURE_LOCATION = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/bat.png");

    public BatProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                placement.produceUnit(
                        (ServerLevel) level,
                        EntityRegistrar.BAT_UNIT.get(),
                        placement.ownerName,
                        false,
                        new Vec3i(0,10,0)
                );
            }
        };
    }

    public String getItemName() {
        return BatProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                TEXTURE_LOCATION,
                List.of(
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.bat_unit"), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.bat_unit.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("entity.reignofnether.bat_unit.tooltip2"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            BatProd.itemName,
            TEXTURE_LOCATION,
            hotkey,
            () -> !FogOfWarClientEvents.isEnabled(),
            () -> true,
            List.of(
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.bat_unit"), Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.bat_unit.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("entity.reignofnether.bat_unit.tooltip2"), Style.EMPTY)
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            BatProd.itemName,
            TEXTURE_LOCATION,
            prodBuilding,
            this,
            first
        );
    }
}
