package com.solegendary.reignofnether.unit.units.piglins;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.production.ReviveHeroProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class PiglinMerchantReviveProd extends ReviveHeroProductionItem {

    public PiglinMerchantReviveProd() {
        super(
            "Revive Piglin Merchant",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/piglin_merchant.png"),
            "units.piglins.reignofnether.piglin_merchant.revive"
        );
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.PIGLIN_MERCHANT_UNIT.get();
    }
}
