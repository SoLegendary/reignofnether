package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

import static com.solegendary.reignofnether.ability.TradeAction.*;
import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;
import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class TradeResources extends Ability {

    public static final int START_RATE = 75;
    public static final int MAX_RATE = 200;
    public static final int MIN_RATE = 15;
    public static final int RATE_STEP = 2;
    public static final int TRADE_AMOUNT = 100;

    private final TradeAction tradeAction;

    public TradeResources(UnitAction action) {
        super(action, 0, 0, 0, false);
        this.tradeAction = switch (action) {
            case TRADE_FOOD_FOR_WOOD -> FOOD_FOR_WOOD;
            case TRADE_FOOD_FOR_ORE -> FOOD_FOR_ORE;
            case TRADE_WOOD_FOR_FOOD -> WOOD_FOR_FOOD;
            case TRADE_WOOD_FOR_ORE -> WOOD_FOR_ORE;
            case TRADE_ORE_FOR_FOOD -> ORE_FOR_FOOD;
            case TRADE_ORE_FOR_WOOD -> ORE_FOR_WOOD;
            default -> null;
        };
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        AbilityButton button = new AbilityButton(
                "Trade Resources",
                getIconResource(),
                hotkey,
                () -> false,
                () -> false,
                () -> true,
                () -> sendUnitCommand(action),
                null,
                List.of(fcsIcons(getTooltip())),
                this,
                placement
        );
        button.bgIconResource = getBgIconResource();
        return button;
    }

    private ResourceName getSellResource() {
        return switch (tradeAction) {
            case FOOD_FOR_WOOD, FOOD_FOR_ORE -> ResourceName.FOOD;
            case WOOD_FOR_FOOD, WOOD_FOR_ORE -> ResourceName.WOOD;
            case ORE_FOR_FOOD, ORE_FOR_WOOD -> ResourceName.ORE;
        };
    }

    private ResourceName getBuyResource() {
        return switch (tradeAction) {
            case FOOD_FOR_WOOD, ORE_FOR_WOOD -> ResourceName.WOOD;
            case FOOD_FOR_ORE, WOOD_FOR_ORE -> ResourceName.ORE;
            case WOOD_FOR_FOOD, ORE_FOR_FOOD -> ResourceName.FOOD;
        };
    }

    private TradeAction getOppositeTradeAction() {
        return switch (tradeAction) {
            case FOOD_FOR_WOOD -> WOOD_FOR_FOOD;
            case FOOD_FOR_ORE -> ORE_FOR_FOOD;
            case WOOD_FOR_FOOD -> FOOD_FOR_WOOD;
            case WOOD_FOR_ORE -> ORE_FOR_WOOD;
            case ORE_FOR_FOOD -> FOOD_FOR_ORE;
            case ORE_FOR_WOOD -> WOOD_FOR_ORE;
        };
    }

    private ResourceLocation getIconResource() {
        return switch (tradeAction) {
            case FOOD_FOR_WOOD, FOOD_FOR_ORE -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
            case WOOD_FOR_FOOD, WOOD_FOR_ORE -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
            case ORE_FOR_FOOD, ORE_FOR_WOOD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
        };
    }

    private ResourceLocation getBgIconResource() {
        return switch (tradeAction) {
            case WOOD_FOR_FOOD, ORE_FOR_FOOD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
            case FOOD_FOR_WOOD, ORE_FOR_WOOD -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
            case FOOD_FOR_ORE, WOOD_FOR_ORE -> ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");
        };
    }

    private String getTooltip() {
        RTSPlayer rtsPlayer = PlayerClientEvents.getRTSPlayer();
        int rate = rtsPlayer != null ? rtsPlayer.tradeRates.get(tradeAction) : 0;
        return switch (tradeAction) {
            case FOOD_FOR_WOOD -> I18n.get("abilities.reignofnether.trade_food_wood", TRADE_AMOUNT, rate);
            case FOOD_FOR_ORE -> I18n.get("abilities.reignofnether.trade_food_ore", TRADE_AMOUNT, rate);
            case WOOD_FOR_FOOD -> I18n.get("abilities.reignofnether.trade_wood_food", TRADE_AMOUNT, rate);
            case WOOD_FOR_ORE -> I18n.get("abilities.reignofnether.trade_wood_ore", TRADE_AMOUNT, rate);
            case ORE_FOR_FOOD -> I18n.get("abilities.reignofnether.trade_ore_food", TRADE_AMOUNT, rate);
            case ORE_FOR_WOOD -> I18n.get("abilities.reignofnether.trade_ore_wood", TRADE_AMOUNT, rate);
        };
    }

    @Override
    public void use(Level level, BuildingPlacement buildingUsing, BlockPos targetBp) {
        doTrade(buildingUsing);
    }

    private void doTrade(BuildingPlacement placement) {
        if (placement.getLevel().isClientSide())
            return;
        String playerName = placement.ownerName;
        RTSPlayer player = PlayerServerEvents.getRTSPlayer(playerName);
        if (player == null)
            return;
        if (player.tradeRates.isEmpty())
            return;
        int rate = player.tradeRates.get(tradeAction);

        if (ResourcesServerEvents.canAfford(playerName, getSellResource(), TRADE_AMOUNT)) {
            switch (tradeAction) {
                case FOOD_FOR_WOOD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, -TRADE_AMOUNT, rate, 0));
                case FOOD_FOR_ORE -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, -TRADE_AMOUNT, 0, rate));
                case WOOD_FOR_FOOD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, rate, -TRADE_AMOUNT, 0));
                case WOOD_FOR_ORE -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, -TRADE_AMOUNT, rate));
                case ORE_FOR_FOOD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, rate, 0, -TRADE_AMOUNT));
                case ORE_FOR_WOOD -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, rate, -TRADE_AMOUNT));
            }
            player.tradeRates.put(tradeAction, Math.max(MIN_RATE, rate - RATE_STEP));
            player.tradeRates.put(getOppositeTradeAction(), Math.min(MAX_RATE, rate + RATE_STEP));
            // TODO: update rates on clientside
            // TODO: update rates on login
        } else {
            // TODO: send floating can't afford warning
        }
    }
}
