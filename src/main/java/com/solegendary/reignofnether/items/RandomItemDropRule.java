package com.solegendary.reignofnether.items;

public enum RandomItemDropRule {
    DISABLED(0), // no random item drops at all
    ENABLED_NON_STRICT(1), // randomised item drops for all players
    ENABLED_STRICT(2); // mirrored drops for all players

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
