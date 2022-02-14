package com.solegendary.reignofnether.units;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UnitCommonModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent evt) {
        evt.put(EntityRegistrar.ZOMBIE_UNIT.get(), ZombieUnit.createAttributes().build());
        evt.put(EntityRegistrar.SKELETON_UNIT.get(), ZombieUnit.createAttributes().build());
        evt.put(EntityRegistrar.CREEPER_UNIT.get(), ZombieUnit.createAttributes().build());
    }
}
