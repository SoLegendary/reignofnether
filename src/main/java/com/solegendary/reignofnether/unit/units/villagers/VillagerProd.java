package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.building.production.StopProductionButton;
import com.solegendary.reignofnether.building.production.UnitProductionItem;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;

import java.util.List;

public class VillagerProd extends ProductionItem implements UnitProductionItem {

    public final static String itemName = "Villager";
    public final static ResourceCost cost = ResourceCosts.VILLAGER;

    public VillagerProd() {
        super(cost);
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.VILLAGER_UNIT.get(), placement.ownerName, true);
        };
    }

    public String getItemName() {
        return VillagerProd.itemName;
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
                List.of(
                        Component.translatable("entity.reignofnether.villager_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("entity.reignofnether.villager_unit.tooltip1").getVisualOrderText(),
                        Component.translatable("entity.reignofnether.villager_unit.tooltip2").getVisualOrderText(),
                        Component.translatable("entity.reignofnether.villager_unit.tooltip3").getVisualOrderText()
                )
        );
    }
    
    public UnitSpawnButton getMilitiaPlaceButton() {
        return new UnitSpawnButton(
            "Militia",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/militia.png"),
            List.of(
                Component.translatable("entity.reignofnether.militia_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText()
            )
        );
    }
    
    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new StartProductionButton(
            VillagerProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            hotkey,
            () -> false,
            () -> true,
            List.of(
                Component.translatable("entity.reignofnether.villager_unit").withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("entity.reignofnether.villager_unit.tooltip1").getVisualOrderText(),
                Component.translatable("entity.reignofnether.villager_unit.tooltip2").getVisualOrderText(),
                Component.translatable("entity.reignofnether.villager_unit.tooltip3").getVisualOrderText()
            ),
            this
        );
    }

    public StopProductionButton getCancelButton(ProductionPlacement prodBuilding, boolean first) {
        return new StopProductionButton(
            VillagerProd.itemName,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            prodBuilding,
            this,
            first
        );
    }
}
