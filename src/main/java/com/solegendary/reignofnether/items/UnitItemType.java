package com.solegendary.reignofnether.items;

import net.minecraft.client.resources.language.I18n;

public enum UnitItemType {
    UPGRADE,
    CONSUMABLE,
    PASSIVE,
    ACTIVE,
    QUEST;

    public String getLabel() {
        String key = "unititemtype.reignofnether." + name().toLowerCase();
        String translated = I18n.get(key);
        if (!translated.equals(key))
            return translated;
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
