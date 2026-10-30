package com.solegendary.reignofnether.ai;

// difficulty only changes how fast the bot reacts: how often it thinks and how many actions per minute it gets.
// Every level plays the same way with the same resources and vision.
// ponytail: only Normal for now; harder levels come back once the bot plays well at this speed
public enum BotDifficulty {
    NORMAL(10, 360); // 2 thinks and 6 actions a second: a bot's edge is speed, not cleverness

    public final int ticksPerThink;
    public final int actionsPerMinute;

    BotDifficulty(int ticksPerThink, int actionsPerMinute) {
        this.ticksPerThink = ticksPerThink;
        this.actionsPerMinute = actionsPerMinute;
    }

    // actions earned per think; fractions carry over (see BotBrain.spendAction)
    public double actionsPerThink() {
        return actionsPerMinute * ticksPerThink / 1200.0;
    }

    public static BotDifficulty fromOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : NORMAL;
    }
}
