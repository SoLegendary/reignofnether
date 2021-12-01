package com.solegendary.ageofcraft.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkHooks;

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
        System.out.println(evt.getMessage());

        // containers have to be opened server side so that the server can track its data
        ServerPlayerEntity serverPlayer = evt.getPlayer();
        String chatMsg = evt.getMessage();

        if (serverPlayer != null) {
            IContainerProvider provider = TopdownGuiContainer.getServerContainerProvider();
            INamedContainerProvider namedProvider = new SimpleNamedContainerProvider(provider, TopdownGuiContainer.TITLE);
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
