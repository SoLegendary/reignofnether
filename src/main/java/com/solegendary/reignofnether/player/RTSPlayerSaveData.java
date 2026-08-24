package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.ability.abilities.TradeResources;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(RTSPlayerSaveData::load, RTSPlayerSaveData::create, "saved-rtsplayer-data");
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
                Faction faction = Faction.valueOf(ptag.getString("faction"));
                int[] scores = ptag.contains("sources") ? ptag.getIntArray("scores") : new RTSPlayerScores().getScoreListAsArray();
                int scenarioRoleIndex = ptag.getInt("scenarioRoleIndex");

                Map<TradeAction, Integer> tradeRates = new HashMap<>();
                tradeRates.put(TradeAction.FOOD_FOR_WOOD, ptag.contains("foodWoodRate") ? ptag.getInt("foodWoodRate") : TradeResources.START_RATE);
                tradeRates.put(TradeAction.FOOD_FOR_ORE, ptag.contains("foodOreRate") ? ptag.getInt("foodOreRate") : TradeResources.START_RATE);
                tradeRates.put(TradeAction.WOOD_FOR_FOOD, ptag.contains("woodFoodRate") ? ptag.getInt("woodFoodRate") : TradeResources.START_RATE);
                tradeRates.put(TradeAction.WOOD_FOR_ORE, ptag.contains("woodOreRate") ? ptag.getInt("woodOreRate") : TradeResources.START_RATE);
                tradeRates.put(TradeAction.ORE_FOR_FOOD, ptag.contains("oreFoodRate") ? ptag.getInt("oreFoodRate") : TradeResources.START_RATE);
                tradeRates.put(TradeAction.ORE_FOR_WOOD, ptag.contains("oreWoodRate") ? ptag.getInt("oreWoodRate") : TradeResources.START_RATE);

                data.rtsPlayers.add(RTSPlayer.getFromSave(name, id, ticksWithoutCapitol, faction, beaconOwnerTicks, scores, scenarioRoleIndex, tradeRates));

                ReignOfNether.LOGGER.info("RTSPlayerSaveData.load: " + name + "|" + id + "|" + faction);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("RTSPlayerSaveData.save");

        ListTag list = new ListTag();
        this.rtsPlayers.forEach(p -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("name", p.name);
            cTag.putInt("id", p.id);
            cTag.putInt("ticksWithoutCapitol", p.ticksWithoutCapitol);
            cTag.putInt("beaconOwnerTicks", p.beaconOwnerTicks);
            cTag.putString("faction", p.faction.name());
            cTag.putIntArray("scores", p.scores.getScoreListAsArray());
            cTag.putInt("scenarioRoleIndex", p.scenarioRoleIndex);
            cTag.putInt("foodWoodRate", p.tradeRates.get(TradeAction.FOOD_FOR_WOOD));
            cTag.putInt("foodOreRate", p.tradeRates.get(TradeAction.FOOD_FOR_ORE));
            cTag.putInt("woodFoodRate", p.tradeRates.get(TradeAction.WOOD_FOR_FOOD));
            cTag.putInt("woodOreRate", p.tradeRates.get(TradeAction.WOOD_FOR_ORE));
            cTag.putInt("oreFoodRate", p.tradeRates.get(TradeAction.ORE_FOR_FOOD));
            cTag.putInt("oreWoodRate", p.tradeRates.get(TradeAction.ORE_FOR_WOOD));
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
