package com.solegendary.reignofnether.player;

import net.minecraft.core.HolderLookup;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.ability.abilities.TradeResources;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.solegendary.reignofnether.util.SavedDataCompat;
import com.solegendary.reignofnether.player.RTSPlayerScores;
import com.solegendary.reignofnether.player.RTSPlayerSaveData;
import com.solegendary.reignofnether.player.RTSPlayer;

public class RTSPlayerSaveData extends SavedData {

    public final ArrayList<RTSPlayer> rtsPlayers = new ArrayList<>();

    private static RTSPlayerSaveData create() {
        return new RTSPlayerSaveData();
    }

    @Nonnull
    public static RTSPlayerSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return SavedDataCompat.computeIfAbsent(server, "saved-rtsplayer-data", RTSPlayerSaveData::create, RTSPlayerSaveData::load);
    }

    public static RTSPlayerSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("RTSPlayerSaveData.load");

        RTSPlayerSaveData data = create();
        ListTag ltag = (ListTag) tag.get("rtsplayers");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag ptag = (CompoundTag) ctag;

                String name = ptag.getString("name");
                int id = ptag.getInt("id");
                int ticksWithoutCapitol = ptag.getInt("ticksWithoutCapitol");
                int beaconOwnerTicks = ptag.getInt("beaconOwnerTicks");
                Faction faction;
                try {
                    faction = Factions.getFaction(ResourceLocation.parse(ptag.getString("faction")));
                } catch (Exception e) {
                    faction = switch (ptag.getString("faction")) {
                        case "VILLAGERS" -> Factions.VILLAGERS;
                        case "MONSTERS" -> Factions.MONSTERS;
                        case "PIGLINS" -> Factions.PIGLINS;
                        default -> Factions.NEUTRAL;
                    };
                }
                int[] scores = ptag.contains("sources") ? ptag.getIntArray("scores") : new RTSPlayerScores().getScoreListAsArray();
                int scenarioRoleIndex = ptag.getInt("scenarioRoleIndex");

                Map<TradeAction, Integer> tradeRates = new HashMap<>();
                // 1.5.0 reworked the market: rates are now per-direction (buy vs sell), so the
                // single START_RATE default became START_BUY_RATE / START_SELL_RATE and the
                // persisted keys changed from <a>For<b>Rate to <a>Rate/<b>Rate pairs.
                tradeRates.put(TradeAction.FOOD_FOR_EMERALD, ptag.contains("foodEmeraldRate") ? ptag.getInt("foodEmeraldRate") : TradeResources.START_SELL_RATE);
                tradeRates.put(TradeAction.EMERALD_FOR_FOOD, ptag.contains("emeraldFoodRate") ? ptag.getInt("emeraldFoodRate") : TradeResources.START_BUY_RATE);
                tradeRates.put(TradeAction.WOOD_FOR_EMERALD, ptag.contains("woodEmeraldRate") ? ptag.getInt("woodEmeraldRate") : TradeResources.START_SELL_RATE);
                tradeRates.put(TradeAction.EMERALD_FOR_WOOD, ptag.contains("emeraldWoodRate") ? ptag.getInt("emeraldWoodRate") : TradeResources.START_BUY_RATE);
                tradeRates.put(TradeAction.ORE_FOR_EMERALD, ptag.contains("oreEmeraldRate") ? ptag.getInt("oreEmeraldRate") : TradeResources.START_SELL_RATE);
                tradeRates.put(TradeAction.EMERALD_FOR_ORE, ptag.contains("emeraldOreRate") ? ptag.getInt("emeraldOreRate") : TradeResources.START_BUY_RATE);

                // 1.5.0 persists a per-player item-drop seed and two counters alongside the trade rates
        int creepScore = ptag.contains("creepScore") ? ptag.getInt("creepScore") : 0;
        int itemsDropped = ptag.contains("itemsDropped") ? ptag.getInt("itemsDropped") : 0;
        Long itemSeed = ptag.contains("itemSeed") ? ptag.getLong("itemSeed") : new java.util.Random().nextLong();

        data.rtsPlayers.add(RTSPlayer.getFromSave(
                name, id, ticksWithoutCapitol, faction,
                beaconOwnerTicks, scores, scenarioRoleIndex,
                tradeRates, creepScore, itemsDropped, itemSeed
        ));

                ReignOfNether.LOGGER.info("RTSPlayerSaveData.load: " + name + "|" + id + "|" + faction);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        //ReignOfNether.LOGGER.info("RTSPlayerSaveData.save");

        ListTag list = new ListTag();
        this.rtsPlayers.forEach(p -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("name", p.name);
            cTag.putInt("id", p.id);
            cTag.putInt("ticksWithoutCapitol", p.ticksWithoutCapitol);
            cTag.putInt("beaconOwnerTicks", p.beaconOwnerTicks);
            cTag.putString("faction", p.faction.key.toString());
            cTag.putIntArray("scores", p.scores.getScoreListAsArray());
            cTag.putInt("scenarioRoleIndex", p.scenarioRoleIndex);
            // 1.5.0 made rates per-direction, so the persisted keys changed too
            cTag.putInt("foodEmeraldRate", p.tradeRates.get(TradeAction.FOOD_FOR_EMERALD));
            cTag.putInt("emeraldFoodRate", p.tradeRates.get(TradeAction.EMERALD_FOR_FOOD));
            cTag.putInt("woodEmeraldRate", p.tradeRates.get(TradeAction.WOOD_FOR_EMERALD));
            cTag.putInt("emeraldWoodRate", p.tradeRates.get(TradeAction.EMERALD_FOR_WOOD));
            cTag.putInt("oreEmeraldRate", p.tradeRates.get(TradeAction.ORE_FOR_EMERALD));
            cTag.putInt("emeraldOreRate", p.tradeRates.get(TradeAction.EMERALD_FOR_ORE));
            list.add(cTag);

            //ReignOfNether.LOGGER.info("RTSPlayerSaveData.save: " + p.name + "|" + p.id + "|" + p.faction);
        });
        tag.put("rtsplayers", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
