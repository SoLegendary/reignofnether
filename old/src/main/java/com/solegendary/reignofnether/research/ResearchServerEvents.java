package com.solegendary.reignofnether.research;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.production.ProductionItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

// class to track status of research items for all players
public class ResearchServerEvents {

    private static final ArrayList<Pair<String, ResourceLocation>> researchItems = new ArrayList<>();

    private static ServerLevel serverLevel = null;

    public static void saveResearch() {
        if (serverLevel != null) {
            ResearchSaveData researchData = ResearchSaveData.getInstance(serverLevel);
            researchData.researchItems.clear();
            researchData.researchItems.addAll(researchItems);
            researchData.save();
            serverLevel.getDataStorage().save();

            //ReignOfNether.LOGGER.info("saved " + researchItems.size() + " researchItems in serverevents");
        }
    }

    @SubscribeEvent
    public static void loadResearch(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            serverLevel = level;
            ResearchSaveData researchData = ResearchSaveData.getInstance(level);
            researchItems.clear();
            researchItems.addAll(researchData.researchItems);
            for (Pair<String, ResourceLocation> researchItem : researchItems)
                syncResearch(researchItem.getFirst());

            ReignOfNether.LOGGER.info("loaded " + researchItems.size() + " researchItems in serverevents");
        }
    }

    public static void removeAllResearch() {
        researchItems.clear();
        saveResearch();
    }

    public static void removeAllResearchFor(String playerName) {
        researchItems.removeIf(r -> r.getFirst().equals(playerName));
        saveResearch();
    }

    public static void syncResearch(String playerName) {
        for (Pair<String, ResourceLocation> researchItem : researchItems)
            if (playerName.equals(researchItem.getFirst())) {
                ResearchClientboundPacket.addResearch(researchItem.getFirst(), researchItem.getSecond().toString());
            }
    }

    public static void addResearch(String playerName, ProductionItem researchItem) {
        addResearch(playerName, ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(researchItem));
    }

    public static void addResearch(String playerName, ResourceLocation researchItemName) {
        researchItems.add(new Pair<>(playerName, researchItemName));
        saveResearch();
    }

    public static void removeResearch(String playerName, ResourceLocation researchItemName) {
        researchItems.removeIf(p -> p.getFirst().equals(playerName) && p.getSecond().equals(researchItemName));
        saveResearch();
    }

    public static boolean playerHasResearch(String playerName, ProductionItem researchItem) {
        return playerHasResearch(playerName, ReignOfNetherRegistries.PRODUCTION_ITEM.getKey(researchItem));
    }

    public static boolean playerHasResearch(String playerName, ResourceLocation researchItemName) {
        if (playerHasCheat(playerName, "medievalman")) {
            return true;
        }
        for (Pair<String, ResourceLocation> researchItem : researchItems)
            if (researchItem.getFirst().equals(playerName) && researchItem.getSecond().equals(researchItemName)) {
                return true;
            }
        return false;
    }

    final private static ArrayList<Pair<String, String>> cheatItems = new ArrayList<>();

    public static void removeAllCheats() {
        cheatItems.clear();
    }

    public static void removeAllCheatsFor(String playerName) {
        cheatItems.removeIf(r -> r.getFirst().equals(playerName));
    }

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
            if (cheatItem.getFirst().equals(playerName) && cheatItem.getSecond().equals(cheatItemName)) {
                return true;
            }
        return false;
    }
}
