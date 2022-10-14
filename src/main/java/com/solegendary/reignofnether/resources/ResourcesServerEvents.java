package com.solegendary.reignofnether.resources;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int startingFood = 1;
    public static final int startingWood = 123;
    public static final int startingOre = 12345;

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {

        String playerName = evt.getEntity().getName().getString();

        Resources playerResources = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(playerName))
                playerResources = resources;

        if (playerResources == null) {
            System.out.println("Assigning Resources object to: " + playerName + ", id: " + evt.getEntity().getId());
            playerResources = new Resources(playerName,
                startingFood,
                startingWood,
                startingOre);
            resourcesList.add(playerResources);
        }

        ResourcesClientboundPacket.syncClientResources(resourcesList);
    }

}
