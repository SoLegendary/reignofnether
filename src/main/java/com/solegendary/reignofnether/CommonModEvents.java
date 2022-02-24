package com.solegendary.reignofnether;

import com.solegendary.reignofnether.gui.TopdownGui;
import com.solegendary.reignofnether.registrars.ContainerRegistrar;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.units.CreeperUnit;
import com.solegendary.reignofnether.units.SkeletonUnit;
import com.solegendary.reignofnether.units.ZombieUnit;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID, bus = Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        System.out.println("Registering packet handler");
        event.enqueueWork(PacketHandler::init);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerEntityRenderer(EntityRegistrar.ZOMBIE_UNIT.get(), ZombieRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.SKELETON_UNIT.get(), SkeletonRenderer::new);
        evt.registerEntityRenderer(EntityRegistrar.CREEPER_UNIT.get(), CreeperRenderer::new);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent evt) {
        evt.put(EntityRegistrar.ZOMBIE_UNIT.get(), ZombieUnit.createAttributes().build());
        evt.put(EntityRegistrar.SKELETON_UNIT.get(), SkeletonUnit.createAttributes().build());
        evt.put(EntityRegistrar.CREEPER_UNIT.get(), CreeperUnit.createAttributes().build());
    }

    @SubscribeEvent
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        MenuScreens.register(ContainerRegistrar.TOPDOWNGUI_CONTAINER.get(), TopdownGui::new);
    }
}

