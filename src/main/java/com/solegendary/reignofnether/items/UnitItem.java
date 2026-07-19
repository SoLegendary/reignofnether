package com.solegendary.reignofnether.items;

import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;

import java.util.List;

// items that can be held and used by RTS units, especially heroes
// they are still registered as actual Minecraft items
// being a vanilla MC item or a RoN custom item does not determine whether you are a UnitItem, eg.
//    - vanilla golden apple ✔
//    - vanilla dirt block ❌
//    - vanilla fire aspect trident ✔
//    - RoN mana potion ✔
//    - Ron RTS Start Block ❌

public class UnitItem {
    final Item item;
    final ResourceLocation iconRl;
    List<FormattedCharSequence> tooltip;

    public UnitItem(Item item, ResourceLocation iconRl) {
        this.item = item;
        this.iconRl = iconRl;

    }

    public Button getButton() {
        return new ButtonBuilder("button_" + item.getDescriptionId())
                .tooltipLines(tooltip)
                .build();
    }

    public boolean canUnitPickup() {
        return true;
    }

    // usually for stuff like resources and piglin merchant loot
    public boolean canUnitAutopickup() {
        return false;
    }
}
