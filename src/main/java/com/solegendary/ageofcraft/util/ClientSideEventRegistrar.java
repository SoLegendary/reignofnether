package com.solegendary.ageofcraft.util;

import com.solegendary.ageofcraft.gui.TopdownGuiClientEvents;
import com.solegendary.ageofcraft.orthoview.OrthoViewClientEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientSideEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;
    private final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    public ClientSideEventRegistrar() { }

    /**
     * Register client only events. This method must only be called when it is certain that the mod is
     * is executing code on the client side and not the dedicated server.
     */
    public void registerClientEvents() {
        vanillaEventBus.register(OrthoViewClientEvents.class);
        modEventBus.register(TopdownGuiClientEvents.class);
    }
}
