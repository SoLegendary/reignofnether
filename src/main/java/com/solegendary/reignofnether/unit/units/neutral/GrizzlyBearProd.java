package com.solegendary.reignofnether.unit.units.neutral;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class GrizzlyBearProd extends ProductionItem {

    public final static String itemName = "Grizzly Bear";
    public final static ResourceCost cost = ResourceCosts.GRIZZLY_BEAR;

    public GrizzlyBearProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide()) {
                placement.produceUnit((ServerLevel) level, EntityRegistrar.GRIZZLY_BEAR_UNIT.get(), placement.ownerName, true);
            }
        };
    }

    public String getItemName() {
        return GrizzlyBearProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grizzly_bear.png"),
                List.of(
                        fcs(I18n.get("entity.reignofnether.grizzly_bear_unit"), true),
                        fcs(""),
                        fcs(I18n.get("entity.reignofnether.grizzly_bear_unit.tooltip1"))
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grizzly_bear.png"),
                hotkey,
                () -> false,
                () -> true,
                List.of(
                        fcs(I18n.get("entity.reignofnether.grizzly_bear_unit"), true),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPopAndTime(cost),
                        fcs(""),
                        fcs(I18n.get("entity.reignofnether.grizzly_bear_unit.tooltip1"))
                ),
                this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grizzly_bear.png"),
                prodBuilding,
                this,
                first
        );
    }
}
