package com.solegendary.reignofnether.ability.abilities;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.shared.Market;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.MarketTradeServerboundPacket;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

// One trade direction on a Market. Spending TRADE_CHUNK of `from` yields the player's current
// from->to rate of `to`, then degrades that rate by RATE_STEP (floor MIN_RATE) and improves the
// reverse by the same amount. Rate state lives on RTSPlayer.
public class TradeAbility extends Ability {

    private final ResourceName from;
    private final ResourceName to;

    public TradeAbility(ResourceName from, ResourceName to) {
        super(UnitAction.NONE, 0, 0, 0, false, false);
        this.from = from;
        this.to = to;
    }

    public ResourceName getFrom() { return from; }
    public ResourceName getTo() { return to; }

    private String pairKey() {
        return from.name().toLowerCase() + "_" + to.name().toLowerCase();
    }

    private static String iconGlyph(ResourceName r) {
        return switch (r) {
            case FOOD -> "";
            case WOOD -> "";
            case ORE -> "";
            default -> "?";
        };
    }

    private int localRate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return Market.START_RATE;
        RTSPlayer rtsPlayer = PlayerClientEvents.getRTSPlayer(mc.player.getName().getString());
        return rtsPlayer == null ? Market.START_RATE : rtsPlayer.getTradeRate(from, to);
    }

    private List<FormattedCharSequence> buildTooltip() {
        int rate = localRate();
        String header = I18n.get("abilities.reignofnether.trade_" + pairKey());
        String summary = iconGlyph(from) + " " + Market.TRADE_CHUNK + "   ->   " + iconGlyph(to) + " " + rate;

        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(FormattedCharSequence.forward(header, Style.EMPTY.withBold(true)));
        lines.add(FormattedCharSequence.forward(summary, MyRenderer.iconStyle));
        lines.add(FormattedCharSequence.forward("", Style.EMPTY));
        lines.add(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.trade.tooltip1",
                Market.RATE_STEP, Market.MIN_RATE), Style.EMPTY));
        lines.add(FormattedCharSequence.forward(I18n.get("abilities.reignofnether.trade.tooltip2"), Style.EMPTY));
        return lines;
    }

    private boolean canAffordLocally() {
        Resources r = ResourcesClientEvents.getOwnResources();
        if (r == null) return false;
        return switch (from) {
            case FOOD -> r.food >= Market.TRADE_CHUNK;
            case WOOD -> r.wood >= Market.TRADE_CHUNK;
            case ORE -> r.ore >= Market.TRADE_CHUNK;
            default -> false;
        };
    }

    private static ResourceLocation iconFor(ResourceName r) {
        return switch (r) {
            case FOOD -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/wheat.png");
            case WOOD -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/oak_planks.png");
            case ORE  -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_ore.png");
            default   -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/emerald_block.png");
        };
    }

    @Override
    public AbilityButton getButton(Keybinding hotkey, BuildingPlacement placement) {
        AbilityButton btn = new AbilityButton(
                "Trade " + from.name() + " -> " + to.name(),
                iconFor(to),
                hotkey,
                () -> false,
                () -> false,
                this::canAffordLocally,
                () -> PacketHandler.INSTANCE.sendToServer(new MarketTradeServerboundPacket(from, to)),
                null,
                buildTooltip(),
                this,
                placement
        );
        // isSelected runs every frame during HUD render; piggyback to refresh the tooltip
        // so the displayed rate reflects the current RTSPlayer state after each trade.
        btn.isSelected = () -> {
            btn.tooltipLines = buildTooltip();
            return false;
        };
        return btn;
    }
}
