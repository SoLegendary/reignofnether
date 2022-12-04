package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.hud.HudClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesClientEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        for (Resources resources : resourcesList)
            resources.tick();
    }

    public static void syncResources(Resources serverResources) {
        resourcesList.removeIf(resources -> resources.ownerName.equals(serverResources.ownerName));
        resourcesList.add(serverResources);
    }

    // should never be run from clientside except via packet
    public static void addSubtractResources(Resources serverResources) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(serverResources.ownerName))
                resources.changeOverTime(
                    serverResources.food,
                    serverResources.wood,
                    serverResources.ore
                );
    }

    public static void addSubtractResourcesInstantly(Resources serverResources) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(serverResources.ownerName))
                resources.changeInstantly(
                    serverResources.food,
                    serverResources.wood,
                    serverResources.ore
                );
    }

    public static Resources getOwnResources() {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null)
            return getResources(MC.player.getName().getString());
        return null;
    }

    public static Resources getResources(String playerName) {
        for (Resources resources : resourcesList)
            if (resources.ownerName.equals(playerName))
                return resources;
        return null;
    }

    public static void warnInsufficientResources(String ownerName, int food, int wood, int ore) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getName().getString().equals(ownerName)) {

            String msg = "You don't have enough ";
            int countTotal = food + wood + ore;
            int count = 0;
            if (food <= 0) {
                count += 1;
                msg += "food";
            }
            if (wood <= 0) {
                count += 1;
                if (count == 1)
                    msg += "wood";
                else if (count == countTotal)
                    msg += "and wood";
                else
                    msg += ", wood";
            }
            if (ore <= 0) {
                count += 1;
                if (count == 1)
                    msg += "ore";
                else if (count == countTotal)
                    msg += "and ore";
                else
                    msg += ", ore";
            }
            HudClientEvents.showTemporaryMessage(msg);
        }
    }

    public static void warnInsufficientPopulation(String ownerName) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getName().getString().equals(ownerName))
            HudClientEvents.showTemporaryMessage("You don't have enough population supply");
    }

    public static void warnMaxPopulation(String ownerName) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.getName().getString().equals(ownerName))
            HudClientEvents.showTemporaryMessage("You have reached the maximum population");
    }
}
