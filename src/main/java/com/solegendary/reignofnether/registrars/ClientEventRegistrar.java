package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGuiClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGuiServerEvents;
import com.solegendary.reignofnether.healthbars.HealthBarClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.units.UnitClientEvents;
import com.solegendary.reignofnether.units.UnitServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public class ClientEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;

    public ClientEventRegistrar() { }

    /**
     * Register client only events. This method must only be called when it is certain that the mod is
     * is executing code on the client side and not the dedicated server.
     */

    public void registerClientEvents() {
        Keybinds.init();
        vanillaEventBus.register(OrthoviewClientEvents.class);
        vanillaEventBus.register(TopdownGuiClientEvents.class);
        vanillaEventBus.register(UnitClientEvents.class);
        vanillaEventBus.register(HealthBarClientEvents.class);
        vanillaEventBus.register(HudClientEvents.class); // ensure this is first so cursor is rendered above hud
        vanillaEventBus.register(CursorClientEvents.class);
        vanillaEventBus.register(MinimapClientEvents.class);

        // to allow singleplayer integrated server to work
        vanillaEventBus.register(TopdownGuiServerEvents.class);
        vanillaEventBus.register(UnitServerEvents.class);
        //vanillaEventBus.register(CursorServerEvents.class);
    }
}
