package com.solegendary.reignofnether.unit.units.villagers;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.production.ReviveHeroProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class RoyalGuardReviveProd extends ReviveHeroProductionItem {

    public RoyalGuardReviveProd() {
        super(
            "Revive Royal Guard",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/royal_guard.png"),
            "units.villagers.reignofnether.royal_guard.revive"
        );
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.ROYAL_GUARD_UNIT.get();
    }
}
