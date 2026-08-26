package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.hud.buttons.UnitItemButton;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class EmptyUnitItem extends UnitItem {

    public EmptyUnitItem() {
        super(UnitItemBuilder.of(Items.AIR)
                .type(UnitItemType.NONE)
                .enableTooltip(false));
    }

    public UnitItemButton getEmptySlotButton(int index, boolean enabled, Unit unit) {
        UnitItemButton button = new UnitItemButton(index, this, new ItemStack(item), unit, null);
        button.isEnabled = () -> enabled;
        return button;
    }
}
