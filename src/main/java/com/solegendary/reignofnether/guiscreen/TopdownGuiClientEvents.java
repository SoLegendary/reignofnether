package com.solegendary.reignofnether.guiscreen;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handler for TopdownGui, the GUI screen that allows for cursor movement on screen
 * Doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static String lastScreenClosed = "Game Menu";

    // allow us to go between topdownGui <-> pauseMenu seamlessly by pressing escape
    @SubscribeEvent
    public static void onCloseScreen(ScreenEvent.Closing evt) {
        if (OrthoviewClientEvents.isEnabled() && evt.getScreen() instanceof PauseScreen)
            TopdownGuiServerboundPacket.openTopdownGui(MC.player.getId());
    }
    // if no other screen is open and we've got orthoview enabled, open pause screen
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (OrthoviewClientEvents.isEnabled() && Minecraft.getInstance().screen == null)
            MC.setScreen(new PauseScreen(true));
    }

    @SubscribeEvent
    public static void beforeGuiRender(ScreenEvent.Render.Pre evt) {
        String guiTitle = evt.getScreen().getTitle().getString();

        // cancel drawing the GUI
        if (guiTitle.equals("topdowngui_container"))
            evt.setCanceled(true);
    }
}
