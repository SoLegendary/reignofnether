package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingServerboundPacket;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.building.production.HeroProductionItem;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.sandbox.SandboxAction;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;

import java.util.List;

public class PiglinMerchantProd extends HeroProductionItem {

    public final static String itemName = "Piglin Merchant";
    public final static ResourceCost cost = ResourceCosts.PIGLIN_MERCHANT;

    public PiglinMerchantProd() {
        super(cost, itemName, new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/piglin_merchant.png"));
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.PIGLIN_MERCHANT_UNIT.get();
    }

    public AbilityButton getPlaceButton() {
        return new AbilityButton(
                itemName,
                iconRl,
                null,
                () -> SandboxClientEvents.spawnUnitName.equals(itemName),
                () -> false,
                () -> true,
                () -> {
                    CursorClientEvents.setLeftClickSandboxAction(SandboxAction.SPAWN_UNIT);
                    SandboxClientEvents.spawnUnitName = itemName;
                },
                null,
                List.of(
                        FormattedCharSequence.forward(
                                I18n.get("units.piglins.reignofnether.piglin_merchant") +
                                        " (" + I18n.get("hud.units.reignofnether.hero") + ")",
                                Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.piglin_merchant.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.piglin_merchant.tooltip2"), Style.EMPTY)
                ),
                null
        );
    }

    public Button getStartButton(ProductionPlacement prodBuilding, Keybinding hotkey) {
        return new Button(
                itemName,
                14,
                iconRl,
                hotkey,
                () -> false,
                () -> itemIsBeingProduced(prodBuilding.ownerName) || heroOwned(prodBuilding.level.isClientSide(), prodBuilding.ownerName),
                () -> true,
                () -> BuildingServerboundPacket.startProduction(prodBuilding.originPos, this),
                null,
                List.of(
                        FormattedCharSequence.forward(
                                I18n.get("units.piglins.reignofnether.piglin_merchant") +
                                        " (" + I18n.get("hud.units.reignofnether.hero") + ")",
                                Style.EMPTY.withBold(true)),
                        ResourceCosts.getFormattedCost(cost),
                        ResourceCosts.getFormattedPopAndTime(cost),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.piglin_merchant.tooltip1"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("units.piglins.reignofnether.piglin_merchant.tooltip2"), Style.EMPTY)
                )
        );
    }
}
