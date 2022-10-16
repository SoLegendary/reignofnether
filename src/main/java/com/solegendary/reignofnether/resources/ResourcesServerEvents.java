package com.solegendary.reignofnether.resources;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int startingFood = 3000;
    public static final int startingWood = 1000;
    public static final int startingOre = 1000;

    public static void addSubtractResources(Resources resourcesToAdd) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(resourcesToAdd.ownerName)) {

                System.out.println("ResourcesServerEvents addSubtractResources");
                System.out.println(resourcesToAdd.food);
                System.out.println(resourcesToAdd.wood);
                System.out.println(resourcesToAdd.ore);

                // change serverside instantly
                resources.changeInstantly(
                    resourcesToAdd.food,
                    resourcesToAdd.wood,
                    resourcesToAdd.ore
                );
                // change clientside over time
                ResourcesClientboundPacket.addSubtractResources(new Resources(
                    resourcesToAdd.ownerName,
                    resourcesToAdd.food,
                    resourcesToAdd.wood,
                    resourcesToAdd.ore
                ));
            }
        }
    }

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
        ResourcesClientboundPacket.syncResources(resourcesList);
    }
}
