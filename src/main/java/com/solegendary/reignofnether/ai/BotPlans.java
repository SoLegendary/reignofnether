package com.solegendary.reignofnether.ai;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Loads data/<namespace>/bot_plans/<faction>.json (datapacks can override them) and decides which factions the
// bots may play. This is the one switch for a faction's AI: a missing file, a broken file or "status": "maintenance"
// all mean bots don't play that faction and don't take over its players.
public class BotPlans extends SimpleJsonResourceReloadListener {

    private static final Map<ResourceLocation, BotPlan> plans = new HashMap<>(); // faction key -> plan

    public BotPlans() {
        super(new Gson(), "bot_plans");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        plans.clear();
        files.forEach((id, json) -> {
            try {
                JsonElement status = json.getAsJsonObject().get("status");
                if (status == null || !"enabled".equals(status.getAsString())) {
                    ReignOfNether.LOGGER.info("[Bot] plan {} is in maintenance, bots won't play that faction", id);
                    return;
                }
                BotPlan plan = BotPlan.parse(json.getAsJsonObject());
                plans.put(id, plan); // data/<faction namespace>/bot_plans/<faction path>.json, same key as the faction
                ReignOfNether.LOGGER.info("[Bot] plan {} loaded", id);
            } catch (RuntimeException e) {
                ReignOfNether.LOGGER.error("[Bot] plan {} is broken, bots won't play that faction", id, e);
            }
        });
    }

    @Nullable
    public static BotPlan get(Faction faction) {
        return plans.get(faction.key);
    }

    public static boolean isAvailable(Faction faction) {
        return get(faction) != null;
    }

    // the classic factions bots can play right now, in their usual order
    public static List<Faction> available() {
        List<Faction> list = new ArrayList<>();
        for (Faction f : List.of(Factions.VILLAGERS, Factions.MONSTERS, Factions.PIGLINS))
            if (isAvailable(f))
                list.add(f);
        return list;
    }

    // what a bot seat set to RANDOM (or to a faction in maintenance) plays; NONE when no faction is available
    public static Faction resolve(Faction picked, RandomSource random) {
        if (isAvailable(picked))
            return picked;
        List<Faction> list = available();
        return list.isEmpty() ? Factions.NONE : list.get(random.nextInt(list.size()));
    }
}
