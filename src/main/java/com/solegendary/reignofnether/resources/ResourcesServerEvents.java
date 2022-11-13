package com.solegendary.reignofnether.resources;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int STARTING_FOOD = 1000;
    public static final int STARTING_WOOD = 1000;
    public static final int STARTING_ORE = 1000;

    public static void addSubtractResources(Resources resourcesToAdd) {
        for (Resources resources : resourcesList) {
            if (resources.ownerName.equals(resourcesToAdd.ownerName)) {
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

    public static boolean canAfford(String ownerName, String resourceType, int cost) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName)) {
                switch(resourceType.toLowerCase()) {
                    case "food" -> {
                        return resources.food >= cost;
                    }
                    case "wood" -> {
                        return resources.wood >= cost;
                    }
                    case "ore" -> {
                        return resources.ore >= cost;
                    }
                }
            }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {

        String playerName = evt.getEntity().getName().getString();


        Resources playerResources = null;
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(playerName))
                playerResources = resources;

        if (playerResources == null) {
            playerResources = new Resources(playerName,
                    STARTING_FOOD,
                    STARTING_WOOD,
                    STARTING_ORE);
            resourcesList.add(playerResources);
        }
        // TODO: server doesn't like us loading LocalPlayer class in here...
        ResourcesClientboundPacket.syncResources(resourcesList);
    }
}
