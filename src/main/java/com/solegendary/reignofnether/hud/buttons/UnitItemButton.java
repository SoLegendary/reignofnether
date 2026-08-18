package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.items.UnitItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class UnitItemButton extends Button {

    public UnitItemButton(UnitItem unitItem) {
        super(
                "button_" + unitItem.getItemStack().getItem().getDescriptionId(),
                Button.DEFAULT_ICON_SIZE,
                null,
                null,
                () -> false, // todo: if cursor action is on sell/drop/give
                () -> false,
                () -> true,
                () -> { }, // todo: left click use and drag to enable targeting for sell/drop/give
                () -> { }, // todo: right click to toggle targeting for sell/drop/give
                List.of()
        );
        this.iconItem = new ItemStack(unitItem.getItemStack().getItem());
    }

}
