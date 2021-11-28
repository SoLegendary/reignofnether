package com.solegendary.ageofcraft.util;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/*
    This class is required to make sure that we don't accidentally try to load any client-side-only classes
      on a dedicated server.
    It is a rather convoluted way of doing it, but I haven't found a simpler way to do it which is robust
 */

public class ServerSideEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;
    private final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    public ServerSideEventRegistrar() { }

    /**
     * Register server only events. This method must only be called when it is certain that the mod is
     * is executing code on the server side and not the client.
     */
    public void registerServerEvents() {

    }
}
