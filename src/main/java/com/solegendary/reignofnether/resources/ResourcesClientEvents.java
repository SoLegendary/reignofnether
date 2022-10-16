package com.solegendary.reignofnether.resources;

import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class ResourcesClientEvents {

    // tracks all players' resources
    public static ArrayList<Resources> resourcesList = new ArrayList<>();

    public static void syncResources(Resources serverResources) {
        resourcesList.removeIf(resources -> resources.ownerName.equals(serverResources.ownerName));
        resourcesList.add(serverResources);
    }

    // should never be run from clientside except via packet
    public static void addSubtractResources(Resources serverResources) {
        System.out.println("ResourcesClientEvents addSubtractResources");
        System.out.println(serverResources.food);
        System.out.println(serverResources.wood);
        System.out.println(serverResources.ore);

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

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        for (Resources resources : resourcesList)
            resources.tick();
    }
}
