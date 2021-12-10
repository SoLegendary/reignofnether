package com.solegendary.ageofcraft.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fmllegacy.network.NetworkHooks;

/**
 * Handler for TopdownGui, doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiCommonVanillaEvents {

    //private static final String KEY_CATEGORY = "key.categories.ageofcraft";
    private static final Minecraft MC = Minecraft.getInstance();

    //private final KeyBinding keyToggleTdgui = new KeyBinding("key.ageofcraft.orthoview.toggleTdgui", GLFW.GLFW_KEY_L, KEY_CATEGORY);

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent evt) {

        // containers have to be opened server side so that the server can track its data
        ServerPlayer serverPlayer = evt.getPlayer();
        String chatMsg = evt.getMessage();

        if (serverPlayer != null) {
            MenuConstructor provider = TopdownGuiContainer.getServerContainerProvider();
            MenuProvider namedProvider = new SimpleMenuProvider(provider, TopdownGuiContainer.TITLE);
            NetworkHooks.openGui(serverPlayer, namedProvider);
        }
        else {
            System.out.println("serverPlayer is null");
        }
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
