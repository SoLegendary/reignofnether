package com.solegendary.reignofnether.units;

import com.solegendary.reignofnether.registrars.EntityRegistrar;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class UnitServerModEvents {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent evt) {
        evt.put(EntityRegistrar.ZOMBIE_UNIT.get(), ZombieUnit.createAttributes().build());
        evt.put(EntityRegistrar.SKELETON_UNIT.get(), SkeletonUnit.createAttributes().build());
        evt.put(EntityRegistrar.CREEPER_UNIT.get(), CreeperUnit.createAttributes().build());
    }
}
