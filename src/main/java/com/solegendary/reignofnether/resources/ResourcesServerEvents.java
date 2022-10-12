package com.solegendary.reignofnether.resources;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {

        String playerName = evt.getEntity().getName().getString();

        Resources playerResources = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName == playerName)
                playerResources = resources;

        if (playerResources == null) {
            System.out.println("Assigning Resources object to: " + playerName + ", id: " + evt.getEntity().getId());
            playerResources = new Resources(playerName,0,0,0);
        }
        resourcesList.add(playerResources);

        ResourcesClientboundPacket.syncServerResources(resourcesList);
    }

}
