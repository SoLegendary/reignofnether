package com.solegendary.ageofcraft.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
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
        }
        else {
            System.out.println("serverPlayer is null, cannot open topdown gui");
        }
    }

    public static void closeTopdownGui() {
        MC.popGuiLayer();
    }

    // open the menu that would normally be opened on pressing esc, while the topdown gui is open
    // we should also register an event to reopen the topdown gui (if it was open) when the esc menu is closed
    // TODO: make this actually work...
    public static void openEscMenu() {
        System.out.println("Opening esc menu!");
        MC.pauseGame(true);
    }

    @SubscribeEvent
    public static void beforeGuiRender(GuiScreenEvent.DrawScreenEvent.Pre evt) {
        String guiTitle = evt.getGui().getTitle().getString();

        // cancel drawing the GUI
        if (guiTitle.equals("topdowngui_container")) {
            evt.setCanceled(true);
        }
    }
}
