package com.solegendary.reignofnether.minimap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class MinimapServerEvents {

    private static final ArrayList<ServerPlayer> serverPlayers = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        System.out.println("Player logged in: " + evt.getPlayer().getName().getString() + ", id: " + evt.getPlayer().getId());
        serverPlayers.add((ServerPlayer) evt.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getPlayer().getId();
        System.out.println("Player logged out: " + evt.getPlayer().getName().getString() + ", id: " + id);
        serverPlayers.removeIf(player -> player.getId() == id);
    }
}