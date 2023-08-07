package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;

import java.util.ArrayList;

// class to track status of research items for the client player - we generally don't care about other players' research
public class ResearchClient {

    final private static ArrayList<String> researchItems = new ArrayList<>();

    public static void removeAllResearch() {
        researchItems.clear();
    }

    public static void addResearch(String researchItemName) {
        researchItems.add(researchItemName);
    }

    public static void removeResearch(String researchItemName) {
        researchItems.removeIf(r -> r.equals(researchItemName));
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
    }

    public static void removeCheat(String cheatItemName) {
        cheatItems.removeIf(r -> r.equals(cheatItemName));
    }

    public static boolean hasCheat(String cheatItemName) {
        for (String cheatItem : cheatItems)
            if (cheatItem.equals(cheatItemName))
                return true;
        return false;
    }

}
