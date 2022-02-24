package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.cursor.CursorClientVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiClientVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiServerVanillaEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientVanillaEvents;
import com.solegendary.reignofnether.units.UnitClientVanillaEvents;
import com.solegendary.reignofnether.units.UnitServerVanillaEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;

    public ClientEventRegistrar() { }

    /**
     * Register client only events. This method must only be called when it is certain that the mod is
     * is executing code on the client side and not the dedicated server.
     */

    // TODO: check if we can just move all mod events outside (into the .init() classes)

    public void registerClientEvents() {
        Keybinds.init();
        vanillaEventBus.register(OrthoviewClientVanillaEvents.class);
        vanillaEventBus.register(CursorClientVanillaEvents.class);
        vanillaEventBus.register(TopdownGuiClientVanillaEvents.class);
        vanillaEventBus.register(UnitClientVanillaEvents.class);

        // to allow singleplayer integrated server to work
        vanillaEventBus.register(TopdownGuiServerVanillaEvents.class);
        vanillaEventBus.register(UnitServerVanillaEvents.class);
        //vanillaEventBus.register(CursorServerVanillaEvents.class);


    }
}
