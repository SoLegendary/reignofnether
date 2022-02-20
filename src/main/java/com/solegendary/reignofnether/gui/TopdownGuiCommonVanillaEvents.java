package com.solegendary.reignofnether.gui;

import com.solegendary.reignofnether.orthoview.OrthoviewClientVanillaEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

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

    // ensure topdownGui is always open whenever Orthoview is enabled (if no other screens are open)
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent evt) {
        if (MC.screen == null && OrthoviewClientVanillaEvents.isEnabled())
            openTopdownGui();
    }
}
