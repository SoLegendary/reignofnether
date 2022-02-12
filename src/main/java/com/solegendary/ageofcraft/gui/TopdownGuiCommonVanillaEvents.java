package com.solegendary.ageofcraft.gui;

import com.solegendary.ageofcraft.orthoview.OrthoviewClientVanillaEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Handler for TopdownGui, the GUI screen that allows for cursor movement on screen
 * Doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiCommonVanillaEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static ServerPlayer serverPlayer = null;

    @SubscribeEvent
    public static void onPlayerJoin(OnDatapackSyncEvent evt) {
        serverPlayer = evt.getPlayer();
    }

    public static void openTopdownGui() {
        // containers have to be opened server side so that the server can track its data
        if (serverPlayer != null) {
            MenuConstructor provider = TopdownGuiContainer.getServerContainerProvider();
            MenuProvider namedProvider = new SimpleMenuProvider(provider, TopdownGuiContainer.TITLE);
            NetworkHooks.openGui(serverPlayer, namedProvider);
            serverPlayer.setGameMode(GameType.SPECTATOR);
        }
        else {
            System.out.println("serverPlayer is null, cannot open topdown gui");
        }
    }

    public static void closeTopdownGui() {
        MC.popGuiLayer();
        GameType previousGameMode = serverPlayer.gameMode.getPreviousGameModeForPlayer();
        if (previousGameMode != null)
            serverPlayer.setGameMode(previousGameMode);
    }

    @SubscribeEvent
    public static void beforeGuiRender(ScreenEvent.DrawScreenEvent.Pre evt) {
        String guiTitle = evt.getScreen().getTitle().getString();

        // cancel drawing the GUI
        if (guiTitle.equals("topdowngui_container")) {
            evt.setCanceled(true);
        }
    }

    private static Screen topdownGuiScreen = null;
    private static String lastScreenClosed = "Game Menu";

    // allow us to go between topdownGui <-> pauseMenu seamlessly by pressing escape
    @SubscribeEvent
    public static void onOpenGui(ScreenOpenEvent evt) {

        if (evt.getScreen() != null) {
            String screenName = evt.getScreen().getTitle().getString();

            if (screenName.contains("topdowngui_container"))
                topdownGuiScreen = evt.getScreen();
        }
        // this branch fires twice on screen close - once with screenClosed nonnull, then null
        else if (OrthoviewClientVanillaEvents.isEnabled()) {
            if (MC.screen != null) {

                String screenClosed = MC.screen.getTitle().getString();

                // when we set the menu screen on the same frame as pressing escape, the game tries to close it too
                // TODO: don't do this if we didn't close game menu by escape
                // TODO: if we closed menu by button click or just logged and previously logged out in topdown mode, reopen topdowngui
                if (lastScreenClosed != null &&
                    lastScreenClosed.contains("Game Menu") &&
                    screenClosed.contains("topdowngui_container")) {

                    evt.setCanceled(true);
                }
                lastScreenClosed = screenClosed;
            }
            // closed topdowngui with esc -> open menu screen
            else if (lastScreenClosed != null && lastScreenClosed.contains("topdowngui_container")) {
                evt.setScreen(new PauseScreen(true));
                System.out.println("Set pausescreen");
            }
            // closed menu screen while previously being on topdowngui -> open topdowngui
            else if (lastScreenClosed != null && lastScreenClosed.contains("Game Menu")) {
                evt.setScreen(topdownGuiScreen);
                System.out.println("Set topdownGuiScreen");
            }
        }
    }
}
