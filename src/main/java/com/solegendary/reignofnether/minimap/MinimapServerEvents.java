package com.solegendary.reignofnether.minimap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class MinimapServerEvents {

    private static final ArrayList<ServerPlayer> serverPlayers = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        System.out.println("Player logged in: " + evt.getEntity().getName().getString() + ", id: " + evt.getEntity().getId());
        serverPlayers.add((ServerPlayer) evt.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        System.out.println("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        serverPlayers.removeIf(player -> player.getId() == id);
    }
}