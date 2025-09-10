package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.hud.HudClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// class to track status of research items for the client player - we generally don't care about other players' research
public class ResearchClient {

    private final static Minecraft MC = Minecraft.getInstance();

    final private static List<ResourceLocation> researchItems = Collections.synchronizedList(new ArrayList<>());

    public static void removeAllResearch() {
        synchronized (researchItems) {
            researchItems.clear();
        }
    }

    public static void addResearch(String ownerName, ProductionItem researchItem) {
        addResearch(ownerName, ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(researchItem));
    }

    public static void addResearch(String ownerName, ResourceLocation researchItemName) {
        synchronized (researchItems) {
            if (MC.player != null && MC.player.getName().getString().equals(ownerName)) {
                researchItems.add(researchItemName);
                HudClientEvents.showTemporaryMessage(I18n.get(
                    "research.reignofnether.upgrade_completed",
                    I18n.get("research." + researchItemName.getNamespace() + "." + researchItemName.getPath())
                ));
            }
        }
    }

    public static boolean hasResearch(ProductionItem researchItem) {
        return hasResearch(ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(researchItem));
    }

    public static boolean hasResearch(ResourceLocation researchItemName) {
        synchronized (researchItems) {
            if (hasCheat("medievalman")) {
                return true;
            }
            for (ResourceLocation researchItem : researchItems)
                if (researchItem.equals(researchItemName)) {
                    return true;
                }
            return false;
        }
    }

    final private static List<String> cheatItems = Collections.synchronizedList(new ArrayList<>());

    public static void removeAllCheats() {
        synchronized (cheatItems) {
            cheatItems.clear();
        }
    }

    public static void addCheatWithValue(String cheatItemName, int value) {
    }

    public static void addCheat(String cheatItemName) {
        synchronized (cheatItems) {
            cheatItems.add(cheatItemName);
        }
    }

    public static void removeCheat(String cheatItemName) {
        synchronized (cheatItems) {
            cheatItems.removeIf(r -> r.equals(cheatItemName));
        }
    }

    public static boolean hasCheat(String cheatItemName) {
        synchronized (cheatItems) {
            for (String cheatItem : cheatItems)
                if (cheatItem.equals(cheatItemName)) {
                    return true;
                }
            return false;
        }
    }
}
