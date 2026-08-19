package com.solegendary.reignofnether.items.unititems;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.items.UnitItem;
import com.solegendary.reignofnether.items.UnitItemBuilder;
import com.solegendary.reignofnether.items.UnitItemType;
import com.solegendary.reignofnether.registrars.ItemRegistrar;
import net.minecraft.resources.ResourceLocation;

public class HeroExperienceBottle extends UnitItem {

    public HeroExperienceBottle() {
        super(UnitItemBuilder.of(ItemRegistrar.THROWN_HERO_EXPERIENCE_BOTTLE.get())
                .type(UnitItemType.CONSUMABLE)
                .stackQty(1)
                .sellValue(10)
                .icon(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                        "textures/item/hero_experience_bottle.png"))
                .descKey("item.reignofnether.hero_experience_bottle.desc")
                .bonuses(
                        "item.reignofnether.hero_experience_bottle.bonus1",
                        "item.reignofnether.hero_experience_bottle.bonus2"
                )
                .canUnitPickup(true)
                .canUnitAutopickup(false)
        );
    }
}
