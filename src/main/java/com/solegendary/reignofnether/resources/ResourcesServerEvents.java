package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesServerEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static final int STARTING_FOOD = 100;
    public static final int STARTING_WOOD = 400;
    public static final int STARTING_ORE = 250;

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

    public static boolean canAfford(String ownerName, ResourceName resourceName, int cost) {
        for (Resources resources : ResourcesServerEvents.resourcesList)
            if (resources.ownerName.equals(ownerName)) {
                switch(resourceName) {
                    case FOOD -> {
                        return resources.food >= cost;
                    }
                    case WOOD -> {
                        return resources.wood >= cost;
                    }
                    case ORE -> {
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
        ResourcesClientboundPacket.syncResources(resourcesList);
    }

    // commands for ops to give resources
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent.Submitted evt) {

        if (evt.getPlayer().hasPermissions(4)) {
            String msg = evt.getMessage().getString();
            String[] words = msg.split(" ");
            if (words.length == 4 && words[0].equals("giveresources")) {
                try {
                    String playerName = words[1];
                    int amount = Integer.parseInt(words[2]);
                    String resourceName = words[3].toLowerCase();

                    for (Player player : PlayerServerEvents.players) {
                        if (player.getName().getString().equals(playerName)) {
                            switch (resourceName) {
                                case "food" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, amount, 0, 0));
                                case "wood" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, amount, 0));
                                case "ore" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, 0, amount));
                                case "all" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, amount, amount, amount));
                            }
                        }
                    }
                }
                catch(NumberFormatException err) {
                    System.out.println(err);
                    return;
                }
            }
        }
    }
}
