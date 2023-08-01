package com.solegendary.reignofnether.research;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;

// class to track status of research items for all players
public class ResearchServer {

    final private static ArrayList<Pair<String, String>> researchItems = new ArrayList<>();

    public static void syncResearch(String playerName) {
        for (Pair<String, String> researchItem : researchItems)
            if (playerName.equals(researchItem.getFirst()))
                ResearchClientboundPacket.addResearch(researchItem.getFirst(), researchItem.getSecond());
    }

    public static void addResearch(String playerName, String researchItemName) {
        researchItems.add(new Pair<>(playerName, researchItemName));
    }
    public static void removeResearch(String playerName, String researchItemName) {
        researchItems.removeIf(p -> p.getFirst().equals(playerName) && p.getSecond().equals(researchItemName));
    }
    public static boolean playerHasResearch(String playerName, String researchItemName) {
        if (playerHasCheat(playerName, "medievalman"))
            return true;
        for (Pair<String, String> researchItem : researchItems)
            if (researchItem.getFirst().equals(playerName) && researchItem.getSecond().equals(researchItemName))
                return true;
        return false;
    }

    final private static ArrayList<Pair<String, String>> cheatItems = new ArrayList<>();

    public static void syncCheats(String playerName) {
        for (Pair<String, String> cheatItem : cheatItems)
            if (playerName.equals(cheatItem.getFirst()))
                ResearchClientboundPacket.addCheat(cheatItem.getFirst(), cheatItem.getSecond());
    }

    public static void addCheat(String playerName, String cheatItemName) {
        cheatItems.add(new Pair<>(playerName, cheatItemName));
    }
    public static void removeCheat(String playerName, String cheatItemName) {
        cheatItems.removeIf(p -> p.getFirst().equals(playerName) && p.getSecond().equals(cheatItemName));
    }
    public static boolean playerHasCheat(String playerName, String cheatItemName) {
        for (Pair<String, String> cheatItem : cheatItems)
            if (cheatItem.getFirst().equals(playerName) && cheatItem.getSecond().equals(cheatItemName))
                return true;
        return false;
    }
}
