package com.solegendary.reignofnether.items;

import net.minecraft.network.chat.Component;

/** A translation key plus the (possibly empty) args to format it with, resolved lazily. */
public record LocalizedText(String key, Object... args) {
    public String resolve() {
        return Component.translatable(key, args).getString();
    }
}