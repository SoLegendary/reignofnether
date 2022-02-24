package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.gui.TopdownGuiClientModEvents;
import com.solegendary.reignofnether.gui.TopdownGuiServerVanillaEvents;
import com.solegendary.reignofnether.units.UnitServerModEvents;
import com.solegendary.reignofnether.units.UnitServerVanillaEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/*
    This class is required to make sure that we don't accidentally try to load any client-side-only classes
      on a dedicated server.
    It is a rather convoluted way of doing it, but I haven't found a simpler way to do it which is robust
 */

public class ModEventRegistrar {
    private static final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    public ModEventRegistrar() { }

    public static void registerModEvents() {
        modEventBus.register(UnitServerModEvents.class);
        modEventBus.register(TopdownGuiClientModEvents.class);
    }
}
