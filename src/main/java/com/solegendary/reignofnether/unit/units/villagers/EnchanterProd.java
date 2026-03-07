package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.HeroProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class EnchanterProd extends HeroProductionItem {

    public final static String itemName = "Enchanter";
    public final static ResourceCost cost = ResourceCosts.ENCHANTER;

    public EnchanterProd() {
        super(cost, itemName, ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/enchanter.png"));
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.ENCHANTER_UNIT.get(), placement.ownerName, true);
        };
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.ENCHANTER_UNIT.get();
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                iconRl,
                List.of(
                        FormattedCharSequence.forward(
                                I18n.get("units.villagers.reignofnether.enchanter") +
                                " (" + I18n.get("hud.units.reignofnether.hero") + ")",
                                Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.enchanter.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.enchanter.tooltip2"), Style.EMPTY)
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>(List.of(
                FormattedCharSequence.forward(
                        I18n.get("units.villagers.reignofnether.enchanter") +
                                " (" + I18n.get("hud.units.reignofnether.hero") + ")",
                        Style.EMPTY.withBold(true)),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.enchanter.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("units.villagers.reignofnether.enchanter.tooltip2"), Style.EMPTY)
        ));
        tooltips.addAll(getAdditionalHeroTooltips());

        return super.getStartButton(
                prodBuilding,
                hotkey,
                tooltips
        );
    }
}
