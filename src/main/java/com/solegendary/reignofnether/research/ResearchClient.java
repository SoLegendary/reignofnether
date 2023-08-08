package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;

import static com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents.resetFogChunks;

// class to track status of research items for the client player - we generally don't care about other players' research
public class ResearchClient {

    private final static Minecraft MC = Minecraft.getInstance();

    final private static ArrayList<String> researchItems = new ArrayList<>();

    public static void removeAllResearch() {
        researchItems.clear();
    }

    public static void addResearch(String researchItemName) {
        researchItems.add(researchItemName);
        HudClientEvents.showTemporaryMessage("Upgrade completed: " + researchItemName);
    }

    public static boolean hasResearch(String researchItemName) {
        if (hasCheat("medievalman"))
            return true;
        for (String researchItem : researchItems)
            if (researchItem.equals(researchItemName))
                return true;
        return false;
    }

    final private static ArrayList<String> cheatItems = new ArrayList<>();

    public static void removeAllCheats() {
        cheatItems.clear();
    }

    public static void addCheat(String cheatItemName) {
        cheatItems.add(cheatItemName);
        if (cheatItemName.equals("iseedeadpeople"))
            resetFogChunks();
        if (MC.player != null)
            MC.player.sendSystemMessage(Component.literal("Enabled cheat: " + cheatItemName));
    }

    public static void removeCheat(String cheatItemName) {
        cheatItems.removeIf(r -> r.equals(cheatItemName));
        if (cheatItemName.equals("iseedeadpeople"))
            resetFogChunks();
        if (MC.player != null)
            MC.player.sendSystemMessage(Component.literal("Disabled cheat: " + cheatItemName));
    }

    public static boolean hasCheat(String cheatItemName) {
        for (String cheatItem : cheatItems)
            if (cheatItem.equals(cheatItemName))
                return true;
        return false;
    }

}
