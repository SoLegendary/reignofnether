package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class EdibleFoodItem extends UnitItem {
    public EdibleFoodItem(Item item, int qty) {
         super(UnitItemBuilder.of(item)
            .type(UnitItemType.CONSUMABLE)
            .stackQty(qty));
    }

    @Override
    public List<FormattedCharSequence> getTooltip() {
        String healAmount = "\uE007   " + Math.round(ItemUtil.getFoodHealAmount(getItemStack()));
        if (stackQty > 1)
            healAmount += "   x" + stackQty;
        return List.of(
                fcs(getItemStack().getItem().getName(getItemStack()).getString()),
                fcsIcons(healAmount)
        );
    }
}
