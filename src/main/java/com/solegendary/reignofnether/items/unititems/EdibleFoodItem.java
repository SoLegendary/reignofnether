package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.ItemUtil;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItemType;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class EdibleFoodItem extends UnitItem {
    public EdibleFoodItem(Item item) {
         super(UnitItemBuilder.of(item)
            .type(UnitItemType.CONSUMABLE));
    }

    @Override
    public List<FormattedCharSequence> getTooltip(ItemStack itemStack) {
        String healAmount = "\uE007   " + Math.round(ItemUtil.getFoodHealAmount(itemStack));
        if (itemStack.getCount() > 1)
            healAmount += "   x" + itemStack.getCount();
        return List.of(
                fcs(itemStack.getItem().getName(itemStack).getString()),
                fcsIcons(healAmount)
        );
    }
}
