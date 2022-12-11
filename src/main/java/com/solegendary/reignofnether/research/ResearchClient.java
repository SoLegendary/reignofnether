package com.solegendary.reignofnether.research;

import java.util.ArrayList;

// class to track status of research items for the client player - we generally don't care about other players' research
public class ResearchClient {

    final private static ArrayList<String> researchItems = new ArrayList<>();

    public static void addResearch(String researchItemName) {
        researchItems.add(researchItemName);
    }

    public static boolean hasResearch(String researchItemName) {
        for (String researchItem : researchItems)
            if (researchItem.equals(researchItemName))
                return true;
        return false;
    }

}
