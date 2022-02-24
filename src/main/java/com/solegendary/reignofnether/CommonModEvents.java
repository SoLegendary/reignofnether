package com.solegendary.reignofnether;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID, bus = Bus.MOD)
public class CommonModEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        System.out.println("Registering packet handler");
        event.enqueueWork(PacketHandler::init);
    }


}

