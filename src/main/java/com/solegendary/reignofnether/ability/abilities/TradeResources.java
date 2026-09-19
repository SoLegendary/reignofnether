package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.buttons.AbilityButton;
import com.solegendary.reignofnether.hud.TradeAbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.ability.TradeAction.*;
import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommandManual;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class TradeResources extends Ability {

    public static final int START_BUY_RATE = 80;
    public static final int START_SELL_RATE = 100;
    public static final int MAX_BUY_RATE = 160;
    public static final int MIN_BUY_RATE = 40;
    public static final int MAX_SELL_RATE = 160;
    public static final int MIN_SELL_RATE = 40;
    public static final int RATE_STEP = 2;
    public static final int ALT_RATE_STEP = 1;
    public static final int TRADE_AMOUNT = 100;

    private final TradeAction tradeAction;

    public TradeResources(UnitAction action) {
        super(action, 0, 0, 0, false);
        this.tradeAction = switch (action) {
            case SELL_FOOD -> FOOD_FOR_EMERALD;
            case SELL_WOOD -> WOOD_FOR_EMERALD;
            case SELL_ORE -> ORE_FOR_EMERALD;
            case BUY_FOOD -> EMERALD_FOR_FOOD;
            case BUY_WOOD -> EMERALD_FOR_WOOD;
            case BUY_ORE -> EMERALD_FOR_ORE;
            default -> null;
        };
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        String playerName = placement.ownerName;
        RTSPlayer player = PlayerClientEvents.getRTSPlayer(playerName);
        if (player == null)
            return null;
        if (player.tradeRates.isEmpty())
            return null;

        AbilityButton button = new TradeAbilityButton(
                "Trade Resources",
                getIconResource(),
                hotkey,
                () -> false,
                () -> isBuyAction() && placement.getUpgradeLevel() <= 0,
                () -> true,
                () -> sendUnitCommand(action),
                null,
                List.of(fcsIcons(getTooltip())),
                this,
                placement,
                player.tradeRates.get(tradeAction)
        );
        button.bgIconResource = getBgIconResource();
        return button;
    }

    private ResourceName getSellResource() {
        return switch (tradeAction) {
            case FOOD_FOR_EMERALD -> ResourceName.FOOD;
            case WOOD_FOR_EMERALD -> ResourceName.WOOD;
            case ORE_FOR_EMERALD -> ResourceName.ORE;
            case EMERALD_FOR_FOOD, EMERALD_FOR_WOOD, EMERALD_FOR_ORE -> ResourceName.EMERALD;
        };
    }

    private ResourceLocation getIconResource() {
        return switch (tradeAction) {
            case FOOD_FOR_EMERALD,
                WOOD_FOR_EMERALD,
                ORE_FOR_EMERALD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/emerald_half_right.png");
            case EMERALD_FOR_FOOD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat_half_right.png");
            case EMERALD_FOR_WOOD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood_half_right.png");
            case EMERALD_FOR_ORE -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore_half_right.png");
        };
    }

    private ResourceLocation getBgIconResource() {
        return switch (tradeAction) {
            case EMERALD_FOR_FOOD,
                EMERALD_FOR_WOOD,
                EMERALD_FOR_ORE -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/emerald_half_left.png");
            case FOOD_FOR_EMERALD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat_half_left.png");
            case WOOD_FOR_EMERALD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood_half_left.png");
            case ORE_FOR_EMERALD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore_half_left.png");
        };
    }

    private String getTooltip() {
        RTSPlayer rtsPlayer = PlayerClientEvents.getRTSPlayer();
        int rate = rtsPlayer != null ? rtsPlayer.tradeRates.get(tradeAction) : 0;
        return switch (tradeAction) {
            case FOOD_FOR_EMERALD -> I18n.get("abilities.reignofnether.sell_food", TRADE_AMOUNT, rate);
            case WOOD_FOR_EMERALD -> I18n.get("abilities.reignofnether.sell_wood", TRADE_AMOUNT, rate);
            case ORE_FOR_EMERALD -> I18n.get("abilities.reignofnether.sell_ore", TRADE_AMOUNT, rate);
            case EMERALD_FOR_FOOD -> I18n.get("abilities.reignofnether.buy_food", TRADE_AMOUNT, rate);
            case EMERALD_FOR_WOOD -> I18n.get("abilities.reignofnether.buy_wood", TRADE_AMOUNT, rate);
            case EMERALD_FOR_ORE -> I18n.get("abilities.reignofnether.buy_ore", TRADE_AMOUNT, rate);
        };
    }

    private boolean isBuyAction() {
        return tradeAction == EMERALD_FOR_FOOD ||
                tradeAction == EMERALD_FOR_WOOD ||
                tradeAction == EMERALD_FOR_ORE;
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) {
        doTrade(buildingUsing);
    }

    private void doTrade(BuildingPlacement placement) {
        if (placement.getLevel().isClientSide())
            return;
        if (placement.getUpgradeLevel() <= 0 && isBuyAction()) {
            return;
        }
        String playerName = placement.ownerName;
        RTSPlayer player = PlayerServerEvents.getRTSPlayer(playerName);
        if (player == null)
            return;
        if (player.tradeRates.isEmpty())
            return;

        int rate = player.tradeRates.get(tradeAction);

        if (ResourcesServerEvents.canAfford(playerName, getSellResource(), TRADE_AMOUNT)) {
            switch (tradeAction) {
                case FOOD_FOR_EMERALD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, -TRADE_AMOUNT, 0, 0, rate));
                case WOOD_FOR_EMERALD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, -TRADE_AMOUNT, 0, rate));
                case ORE_FOR_EMERALD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, 0, -TRADE_AMOUNT, rate));
                case EMERALD_FOR_FOOD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, rate, 0, 0, -TRADE_AMOUNT));
                case EMERALD_FOR_WOOD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, rate, 0, -TRADE_AMOUNT));
                case EMERALD_FOR_ORE -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, 0, rate, -TRADE_AMOUNT));
            }
            updateTradeRates(player);
        } else {
            ResourcesClientboundPacket.warnInsufficientResources(playerName,
                getSellResource() != ResourceName.FOOD,
                getSellResource() != ResourceName.WOOD,
                getSellResource() != ResourceName.ORE,
                    false
            );
        }
    }

    private void updateTradeRates(RTSPlayer player) {
        TradeAction altTrade1;
        TradeAction altTrade2;

        if (tradeAction == EMERALD_FOR_FOOD) {
            altTrade1 = EMERALD_FOR_WOOD;
            altTrade2 = EMERALD_FOR_ORE;
        } else if (tradeAction == EMERALD_FOR_WOOD) {
            altTrade1 = EMERALD_FOR_FOOD;
            altTrade2 = EMERALD_FOR_ORE;
        } else if (tradeAction == EMERALD_FOR_ORE) {
            altTrade1 = EMERALD_FOR_FOOD;
            altTrade2 = EMERALD_FOR_WOOD;
        } else if (tradeAction == FOOD_FOR_EMERALD) {
            altTrade1 = WOOD_FOR_EMERALD;
            altTrade2 = ORE_FOR_EMERALD;
        } else if (tradeAction == WOOD_FOR_EMERALD) {
            altTrade1 = FOOD_FOR_EMERALD;
            altTrade2 = ORE_FOR_EMERALD;
        } else {// if (tradeAction == ORE_FOR_EMERALD) {
            altTrade1 = FOOD_FOR_EMERALD;
            altTrade2 = WOOD_FOR_EMERALD;
        }
        int minRate = isBuyAction() ? MIN_BUY_RATE : MIN_SELL_RATE;
        int maxRate = isBuyAction() ? MAX_BUY_RATE : MAX_SELL_RATE;

        int rate = player.tradeRates.get(tradeAction);
        int altRate1 = player.tradeRates.get(altTrade1);
        int altRate2 = player.tradeRates.get(altTrade2);

        int newRate = Math.max(minRate, rate - RATE_STEP);
        int newAltRate1 = Math.min(maxRate, altRate1 + ALT_RATE_STEP);
        int newAltRate2 = Math.min(maxRate, altRate2 + ALT_RATE_STEP);

        player.tradeRates.put(tradeAction, newRate);
        player.tradeRates.put(altTrade1, newAltRate1);
        player.tradeRates.put(altTrade2, newAltRate2);
        PlayerClientboundPacket.setMarketRate(tradeAction, player.name, newRate);
        PlayerClientboundPacket.setMarketRate(altTrade1, player.name, newAltRate1);
        PlayerClientboundPacket.setMarketRate(altTrade2, player.name, newAltRate2);
    }
}
