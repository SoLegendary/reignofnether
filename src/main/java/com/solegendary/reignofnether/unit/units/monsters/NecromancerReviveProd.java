package com.solegendary.reignofnether.unit.units.monsters;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.production.ReviveHeroProductionItem;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public class NecromancerReviveProd extends ReviveHeroProductionItem {

    public NecromancerReviveProd() {
        super(
            "Revive Necromancer",
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/necromancer.png"),
            "units.monsters.reignofnether.necromancer.revive"
        );
    }

    @Override
    protected EntityType<? extends HeroUnit> getHeroEntityType() {
        return EntityRegistrar.NECROMANCER_UNIT.get();
    }
}
