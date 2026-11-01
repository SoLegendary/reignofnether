package com.solegendary.reignofnether.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.production.ProductionItem;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// How a bot plays one faction, read from data/reignofnether/bot_plans/<faction>.json (see BotPlans). Names are
// registry ids without the "reignofnether:" prefix.
public record BotPlan(
        Building capitol, ProductionItem worker, ProductionItem scout,
        Building supply, Building farm, Building dropOff, @Nullable Building market, @Nullable Building library,
        Building heroBuilding, List<Hero> heroes,
        List<WorkerTarget> workers, double workersPerArmyPop,
        Shares gather, Shares openingGather,
        List<Step> opening, int openingMaxMinutes,
        int firstWaveMinutes, int firstWavePop, int waveGrowth,
        List<String> marketItems,
        Map<String, Matchup> matchups) {

    public enum StepType { BUILD, RESEARCH }

    // BUILD: own `count` of `building`. RESEARCH: research `item` at a finished `building`.
    // `needs`: buildings that must be finished first (RoN only checks these client-side, in its buttons)
    public record Step(StepType type, Building building, int count, @Nullable ProductionItem item, List<Building> needs) {}

    public record Hero(ProductionItem unit, ProductionItem revive) {}

    // from `minute` on, the bot wants `count` workers
    public record WorkerTarget(int minute, int count) {}

    public record Shares(double food, double wood, double ore) {}

    // `weight`: share of production. `cap`: never more than this many alive. `needs`: building that unlocks it
    public record ArmyUnit(ProductionItem item, double weight, int cap, @Nullable Building needs) {}

    // what to do against one enemy faction (key: the faction's id path, e.g. "monsters"; "default" for any other)
    public record Matchup(List<Step> tech, List<ArmyUnit> army, List<UnitAction> libraryEnchants,
                          Map<String, List<String>> heroSkills) {}

    public Matchup matchup(String enemyFaction) {
        Matchup m = matchups.get(enemyFaction);
        return m != null ? m : matchups.get("default");
    }

    // ---------------------------------------------------------------- parsing

    static BotPlan parse(JsonObject json) {
        Reader r = new Reader();
        JsonObject waves = r.obj(json, "waves");
        JsonObject gather = r.obj(json, "gather");
        Map<String, Matchup> matchups = new HashMap<>();
        for (var e : r.obj(json, "matchups").entrySet())
            matchups.put(e.getKey(), r.matchup(e.getValue().getAsJsonObject()));
        if (!matchups.containsKey("default"))
            throw new IllegalArgumentException("needs a \"default\" matchup");
        List<Hero> heroes = new ArrayList<>();
        for (JsonElement e : r.arr(json, "heroes")) {
            JsonObject h = e.getAsJsonObject();
            heroes.add(new Hero(r.item(h, "unit"), r.item(h, "revive")));
        }
        List<WorkerTarget> workers = new ArrayList<>();
        for (JsonElement e : r.arr(json, "workers")) {
            JsonObject w = e.getAsJsonObject();
            workers.add(new WorkerTarget(w.get("minute").getAsInt(), w.get("count").getAsInt()));
        }
        workers.sort(Comparator.comparingInt(WorkerTarget::minute));
        List<String> marketItems = new ArrayList<>();
        if (json.has("market_items"))
            for (JsonElement e : json.getAsJsonArray("market_items"))
                marketItems.add(e.getAsString());
        return new BotPlan(
                r.building(json, "capitol"), r.item(json, "worker"), r.item(json, "scout"),
                r.building(json, "supply"), r.building(json, "farm"), r.building(json, "drop_off"),
                r.optBuilding(json, "market"), r.optBuilding(json, "library"),
                r.building(json, "hero_building"), heroes,
                workers, json.has("workers_per_army_pop") ? json.get("workers_per_army_pop").getAsDouble() : 1,
                shares(gather), json.has("gather_opening") ? shares(json.getAsJsonObject("gather_opening")) : shares(gather),
                r.steps(r.arr(json, "opening")), json.has("opening_max_minutes") ? json.get("opening_max_minutes").getAsInt() : 8,
                waves.get("first_minutes").getAsInt(), waves.get("first_pop").getAsInt(), waves.get("growth").getAsInt(),
                marketItems, matchups);
    }

    private static Shares shares(JsonObject json) {
        return new Shares(json.get("food").getAsDouble(), json.get("wood").getAsDouble(), json.get("ore").getAsDouble());
    }

    // turns names into registry entries; an unknown name throws, and BotPlans logs it with the file
    private static final class Reader {

        JsonObject obj(JsonObject json, String key) {
            if (!json.has(key))
                throw new IllegalArgumentException("missing \"" + key + "\"");
            return json.getAsJsonObject(key);
        }

        JsonArray arr(JsonObject json, String key) {
            return json.has(key) ? json.getAsJsonArray(key) : new JsonArray();
        }

        static ResourceLocation id(String name) {
            return name.contains(":") ? ResourceLocation.parse(name) : ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, name);
        }

        Building building(String name) {
            Building b = ReignOfNetherRegistries.BUILDING.get(id(name));
            if (b == null)
                throw new IllegalArgumentException("unknown building \"" + name + "\"");
            return b;
        }

        ProductionItem item(String name) {
            ProductionItem i = ReignOfNetherRegistries.PRODUCTION_ITEM.get(id(name));
            if (i == null)
                throw new IllegalArgumentException("unknown production item \"" + name + "\"");
            return i;
        }

        Building building(JsonObject json, String key) {
            return building(json.get(key).getAsString());
        }

        @Nullable
        Building optBuilding(JsonObject json, String key) {
            return json.has(key) ? building(json, key) : null;
        }

        ProductionItem item(JsonObject json, String key) {
            return item(json.get(key).getAsString());
        }

        List<Building> buildings(JsonObject json, String key) {
            List<Building> list = new ArrayList<>();
            if (json.has(key))
                for (JsonElement e : json.getAsJsonArray(key))
                    list.add(building(e.getAsString()));
            return list;
        }

        // {"build": "barracks", "count": 2, "needs": ["town_centre"]} or {"research": "grand_library", "at": "library"}
        List<Step> steps(JsonArray arr) {
            List<Step> steps = new ArrayList<>();
            for (JsonElement e : arr) {
                JsonObject s = e.getAsJsonObject();
                List<Building> needs = buildings(s, "needs");
                if (s.has("build"))
                    steps.add(new Step(StepType.BUILD, building(s, "build"), s.has("count") ? s.get("count").getAsInt() : 1, null, needs));
                else
                    steps.add(new Step(StepType.RESEARCH, building(s, "at"), 1, item(s, "research"), needs));
            }
            return steps;
        }

        Matchup matchup(JsonObject json) {
            List<ArmyUnit> army = new ArrayList<>();
            for (JsonElement e : arr(json, "army")) {
                JsonObject a = e.getAsJsonObject();
                double weight = a.get("weight").getAsDouble();
                if (!(weight > 0))
                    throw new IllegalArgumentException("army weight must be above 0");
                army.add(new ArmyUnit(item(a, "unit"), weight,
                        a.has("cap") ? a.get("cap").getAsInt() : Integer.MAX_VALUE, optBuilding(a, "needs")));
            }
            List<UnitAction> enchants = new ArrayList<>();
            for (JsonElement e : arr(json, "library_enchants"))
                enchants.add(UnitAction.valueOf(e.getAsString().toUpperCase(Locale.ROOT)));
            Map<String, List<String>> skills = new HashMap<>();
            if (json.has("hero_skills"))
                for (var e : json.getAsJsonObject("hero_skills").entrySet()) {
                    List<String> order = new ArrayList<>();
                    for (JsonElement s : e.getValue().getAsJsonArray())
                        order.add(s.getAsString());
                    skills.put(e.getKey(), order);
                }
            return new Matchup(steps(arr(json, "tech")), army, enchants, skills);
        }
    }
}
