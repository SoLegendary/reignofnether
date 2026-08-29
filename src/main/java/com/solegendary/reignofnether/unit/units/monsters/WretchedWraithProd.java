package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.HeroProductionItem;
import com.solegendary.reignofnether.building.production.StartProductionButton;
import com.solegendary.reignofnether.building.production.UnitProductionItem;
import com.solegendary.reignofnether.hud.buttons.UnitSpawnButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class WretchedWraithProd extends HeroProductionItem implements UnitProductionItem {

    public final static String itemName = "Wretched Wraith";
    public final static ResourceCost cost = ResourceCosts.WRETCHED_WRAITH;

    public WretchedWraithProd() {
        super(cost, itemName, ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/wretched_wraith.png"));
        this.onComplete = (Level level, ProductionPlacement placement) -> {
            if (!level.isClientSide())
                placement.produceUnit((ServerLevel) level, EntityRegistrar.WRETCHED_WRAITH_UNIT.get(), placement.ownerName, true);
        };
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.WRETCHED_WRAITH_UNIT.get();
    }

    public UnitSpawnButton getPlaceButton() {
        return new UnitSpawnButton(
                itemName,
                iconRl,
                List.of(
                        Component.translatable("entity.reignofnether.wretched_wraith_unit").append(Component.literal(" (")).append(Component.translatable("hud.units.reignofnether.hero")).append(Component.literal(")")).withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("entity.reignofnether.wretched_wraith_unit.tooltip1").getVisualOrderText(),
                        Component.translatable("entity.reignofnether.wretched_wraith_unit.tooltip2").getVisualOrderText(),
                        FormattedCharSequence.EMPTY,
                        Component.translatable("entity.reignofnether.wretched_wraith_unit.tooltip3").getVisualOrderText()
                )
        );
    }

    public StartProductionButton getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>(List.of(
                Component.translatable("entity.reignofnether.wretched_wraith_unit").append(Component.literal(" (")).append(Component.translatable("hud.units.reignofnether.hero")).append(Component.literal(")")).withStyle(Style.EMPTY.withBold(true)).getVisualOrderText(),
                ResourceCosts.getFormattedCost(cost),
                ResourceCosts.getFormattedPopAndTime(cost),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("entity.reignofnether.wretched_wraith_unit.tooltip1").getVisualOrderText(),
                Component.translatable("entity.reignofnether.wretched_wraith_unit.tooltip2").getVisualOrderText(),
                FormattedCharSequence.forward("", Style.EMPTY),
                Component.translatable("entity.reignofnether.wretched_wraith_unit.tooltip3").getVisualOrderText()
        ));
        tooltips.addAll(getAdditionalHeroTooltips());

        return super.getStartButton(
                prodBuilding,
                hotkey,
                tooltips
        );
    }
}
