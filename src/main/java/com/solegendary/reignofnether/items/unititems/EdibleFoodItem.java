package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.items.*;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

import static com.solegendary.reignofnether.items.UnitItems.descStr;
import static com.solegendary.reignofnether.util.MiscUtil.fcsIcons;

public class EdibleFoodItem extends UnitItem {

    public static final int GOLDEN_APPLE_ABSORB = 12;
    public static final int ENCHANTED_GOLDEN_APPLE_ABSORB = 24;

    public EdibleFoodItem(Item item) {
         super(UnitItemBuilder.of(item)
             .type(UnitItemType.CONSUMABLE)
             .desc(descStr("item.reignofnether.edible_food_item.desc"))
             .pointDesc(getPointDesc(item))
             .consumeOnUse()
             .onUse(unit -> {
                 Mob mob = (Mob) unit;
                 boolean isApple = item == Items.ENCHANTED_GOLDEN_APPLE || item == Items.GOLDEN_APPLE;
                 boolean noAbsorb = mob.getAbsorptionAmount() <= 0;
                 boolean isHurt = mob.getHealth() < ((Mob) unit).getMaxHealth();
                 if ((isApple && noAbsorb) || (!isApple && isHurt)) {
                     Unit.startEatingFood(unit, new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), new ItemStack(item)));
                     return true;
                 }
                 return false;
             })
         );
    }

    private static String getPointDesc(Item item) {
        if (item == Items.GOLDEN_APPLE) {
            return descStr("item.reignofnether.edible_food_item_absorb.point1", GOLDEN_APPLE_ABSORB);
        } else if (item == Items.ENCHANTED_GOLDEN_APPLE) {
            return descStr("item.reignofnether.edible_food_item_absorb.point1", ENCHANTED_GOLDEN_APPLE_ABSORB);
        } else {
            return descStr("item.reignofnether.edible_food_item_heal.point1", (int) ItemUtil.getFoodHealAmount(new ItemStack(item)));
        }
    }

    @Override
    public List<FormattedCharSequence> getEntityTooltip(ItemStack itemStack) {
        String healAmount = "";
        if (itemStack.getItem() == Items.GOLDEN_APPLE) {
            healAmount = "\uE011   " + Math.round(GOLDEN_APPLE_ABSORB);
        } else if (itemStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            healAmount = "\uE011   " + Math.round(ENCHANTED_GOLDEN_APPLE_ABSORB);
        } else {
            healAmount = "\uE007   " + (int) Math.round(ItemUtil.getFoodHealAmount(itemStack));
        }
        if (itemStack.getCount() > 1)
            healAmount += "   (x" + itemStack.getCount() + ")";
        return List.of(fcsIcons(healAmount));
    }
}
