package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.production.ReviveHeroProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class WretchedWraithReviveProd extends ReviveHeroProductionItem {

    public WretchedWraithReviveProd() {
        super(
            "Revive Wretched Wraith",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/wretched_wraith.png"),
            "units.monsters.reignofnether.wretched_wraith.revive"
        );
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.WRETCHED_WRAITH_UNIT.get();
    }
}
