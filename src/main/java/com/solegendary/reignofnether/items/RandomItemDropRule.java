package com.solegendary.reignofnether.items;

public enum RandomItemDropRule {
    DISABLED(0), // no random item drops at all
    ENABLED_RANDOM(1), // all players have fully randomised drops
    ENABLED_SAME_RARITY(2), // all players get the same rarity on each drop
    ENABLED_SAME_ITEMS(3); // all players get the same item on each drop

    private final int value;

    private RandomItemDropRule(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static RandomItemDropRule fromValue(int value) {
        return java.util.Arrays.stream(RandomItemDropRule.values())
                .filter(status -> status.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown RandomItemDropRule value: " + value));
    }
}
