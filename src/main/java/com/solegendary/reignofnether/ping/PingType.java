package com.solegendary.reignofnether.ping;

// the ping wheel's options: each posts a team chat line (lang key ping.reignofnether.<name>) and has its own colour on the map and in the world
public enum PingType {
    HELP(0xFF3030), DEFEND(0x3A8CFF), ATTACK(0xFF8A00), ENEMY(0xFFE020);

    public final int rgb;

    PingType(int rgb) {
        this.rgb = rgb;
    }

    public String langKey() {
        return "ping.reignofnether." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
