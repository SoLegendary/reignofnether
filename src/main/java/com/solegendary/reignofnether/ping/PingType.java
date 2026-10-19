package com.solegendary.reignofnether.ping;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

// the ping wheel's options. Each has one symbol and colour used everywhere: the wheel, team chat, and the
// minimap, which draws the same shape (MinimapClientEvents.isMarkerPixel)
public enum PingType {
    HELP("◎", 0xFF3030), DEFEND("□", 0x3A8CFF), ATTACK("⚔", 0xFF8A00), ENEMY("◇", 0xFFE020);

    public final String symbol;
    public final int rgb;

    PingType(String symbol, int rgb) {
        this.symbol = symbol;
        this.rgb = rgb;
    }

    // "◎ Help me!" with the symbol in the ping's colour
    public MutableComponent label() {
        return Component.literal(symbol + " ").withStyle(s -> s.withColor(rgb))
                .append(Component.translatable("ping.reignofnether." + name().toLowerCase(Locale.ROOT)).withStyle(s -> s.withColor(0xFFFFFF)));
    }
}
