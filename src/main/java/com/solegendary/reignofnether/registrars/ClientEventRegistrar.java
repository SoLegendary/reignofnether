package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.cursor.CursorClientVanillaEvents;
import com.solegendary.reignofnether.cursor.CursorCommonVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiClientModEvents;
import com.solegendary.reignofnether.gui.TopdownGuiClientVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiServerVanillaEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.reignofnether.units.UnitClientModEvents;
import com.solegendary.reignofnether.units.UnitClientVanillaEvents;
import com.solegendary.reignofnether.units.UnitCommonVanillaEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;
    private final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    public ClientEventRegistrar() { }

    /**
     * Register client only events. This method must only be called when it is certain that the mod is
     * is executing code on the client side and not the dedicated server.
     */
    public void registerClientEvents() {
        Keybinds.init();
        vanillaEventBus.register(OrthoviewClientVanillaEvents.class);
        vanillaEventBus.register(CursorClientVanillaEvents.class);
        modEventBus.register(UnitClientModEvents.class);
        modEventBus.register(TopdownGuiClientModEvents.class);

        vanillaEventBus.register(TopdownGuiClientVanillaEvents.class);
        vanillaEventBus.register(UnitClientVanillaEvents.class);

        vanillaEventBus.register(TopdownGuiServerVanillaEvents.class);
        vanillaEventBus.register(CursorCommonVanillaEvents.class);
        vanillaEventBus.register(UnitCommonVanillaEvents.class);


    }
}
