package com.solegendary.reignofnether.guiscreen;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.client.event.ScreenEvent;
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
    public static void onOpenGui(ScreenEvent.Opening evt) {

        // if getScreen is null, we closed a screen instead of opening it
        // this branch fires twice on screen close - once with screenClosed nonnull, then null
        if (evt.getScreen() == null && OrthoviewClientEvents.isEnabled()) {
            if (MC.screen != null) {

                String screenClosed = MC.screen.getTitle().getString();

                // when we set the menu screen on the same frame as pressing escape, the game tries to close it too
                if (lastScreenClosed != null &&
                    lastScreenClosed.contains("Game Menu") &&
                    screenClosed.contains("topdowngui_container")) {

                    evt.setCanceled(true);
                }
                lastScreenClosed = screenClosed;
            }
            // closed topdowngui with esc -> open menu screen
            else if (lastScreenClosed != null && lastScreenClosed.contains("topdowngui_container"))
                evt.setNewScreen(new PauseScreen(true));
        }
    }

    @SubscribeEvent
    public static void beforeGuiRender(ScreenEvent.Render.Pre evt) {
        String guiTitle = evt.getScreen().getTitle().getString();

        // cancel drawing the GUI
        if (guiTitle.equals("topdowngui_container"))
            evt.setCanceled(true);
    }
}
