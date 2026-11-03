package com.solegendary.reignofnether.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

// A public server where people play against the net: with -Dron.record=<dir> (or the property set by an add-on before
// the server starts) every match gets a folder with
//   <bot>.jsonl.gz   the bots' decisions, the same files the arena writes
//   match.jsonl      who played (faction, bot or human, allies), who was defeated when, who was left at the end
//   commands.jsonl.gz every order a human gave: unit actions, building placements, production and research
//   snapshots.jsonl.gz every unit and building on the map plus everyone's resources, every 5 s
public class BotRecorder {

    private static final int SNAPSHOT_SECONDS = 5;

    @Nullable
    private static Path dir;
    @Nullable
    private static BufferedWriter match, commands, snapshots;
    private static Set<String> alive = new HashSet<>();
    private static long ticks;

    @Nullable
    static Path dir() {
        return dir;
    }

    @Nullable
    private static String root() {
        return ArenaServerEvents.ACTIVE ? null : System.getProperty("ron.record");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || root() == null || ++ticks % 20 != 0)
            return;
        List<RTSPlayer> players;
        synchronized (PlayerServerEvents.rtsPlayers) {
            players = new ArrayList<>(PlayerServerEvents.rtsPlayers);
        }
        Set<String> now = new HashSet<>();
        players.forEach(p -> now.add(p.name));
        if (dir == null && !now.isEmpty())
            start(players);
        if (dir == null)
            return;
        for (String name : alive)
            if (!now.contains(name))
                write(match, event("defeat", name));
        if (now.isEmpty()) {
            end();
            return;
        }
        alive = now;
        if (ticks / 20 % SNAPSHOT_SECONDS == 0)
            write(snapshots, snapshot(players));
        flush();
    }

    // every order a human gives, from the server's packet handlers; bots are already in their decision logs
    public static void command(String owner, String kind, String action, @Nullable String item, @Nullable BlockPos pos,
                               @Nullable BlockPos target, @Nullable int[] units) {
        if (dir == null || owner == null || BotServerEvents.isControlled(owner))
            return;
        JsonObject o = line(kind);
        o.addProperty("owner", owner);
        o.addProperty("action", action);
        if (item != null)
            o.addProperty("item", item);
        if (pos != null)
            o.add("pos", xyz(pos));
        if (target != null)
            o.add("target", xyz(target));
        if (units != null && units.length > 0) {
            JsonArray a = new JsonArray();
            for (int id : units)
                a.add(id);
            o.add("units", a);
        }
        write(commands, o);
    }

    private static void start(List<RTSPlayer> players) {
        dir = Path.of(root()).resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
        ticks = 0;
        try {
            Files.createDirectories(dir);
            match = Files.newBufferedWriter(dir.resolve("match.jsonl"), StandardCharsets.UTF_8);
            commands = gzip("commands.jsonl.gz");
            snapshots = gzip("snapshots.jsonl.gz");
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[Bot] can't record the match to {}", dir, e);
            dir = null;
            return;
        }
        JsonObject o = line("start");
        JsonArray list = new JsonArray();
        for (RTSPlayer p : players) {
            JsonObject j = new JsonObject();
            j.addProperty("name", p.name);
            j.addProperty("faction", p.faction.key.getPath());
            j.addProperty("bot", BotServerEvents.isControlled(p.name));
            j.addProperty("net", BotNets.get(p.faction) != null);
            j.addProperty("color", p.startPosColorId);
            JsonArray allies = new JsonArray();
            players.stream().filter(q -> q != p && AlliancesServerEvents.isAllied(p.name, q.name)).forEach(q -> allies.add(q.name));
            j.add("allies", allies);
            list.add(j);
        }
        o.add("players", list);
        write(match, o);
        ReignOfNether.LOGGER.info("[Bot] recording this match to {}", dir);
    }

    private static BufferedWriter gzip(String name) throws IOException {
        // sync flush: what's written so far stays readable if the server dies mid-match
        return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(dir.resolve(name)), true), StandardCharsets.UTF_8));
    }

    private static JsonObject snapshot(List<RTSPlayer> players) {
        JsonObject o = line("snapshot");
        JsonArray units = new JsonArray();
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            if (!e.isAlive() || !(e instanceof Unit u))
                continue;
            JsonArray a = new JsonArray(); // id, type, owner, x, y, z, health, max health
            a.add(e.getId());
            a.add(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath());
            a.add(u.getOwnerName());
            a.add(e.getBlockX());
            a.add(e.getBlockY());
            a.add(e.getBlockZ());
            a.add(Math.round(e.getHealth()));
            a.add(Math.round(e.getMaxHealth()));
            units.add(a);
        }
        o.add("units", units);
        JsonArray buildings = new JsonArray();
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            if (b.isDestroyedServerside)
                continue;
            JsonArray a = new JsonArray(); // name, owner, x, y, z, built, health, max health
            a.add(b.getBuilding().name);
            a.add(b.ownerName);
            a.add(b.originPos.getX());
            a.add(b.originPos.getY());
            a.add(b.originPos.getZ());
            a.add(b.isBuilt);
            a.add(b.getHealth());
            a.add(b.getMaxHealth());
            buildings.add(a);
        }
        o.add("buildings", buildings);
        JsonObject resources = new JsonObject();
        for (RTSPlayer p : players) {
            Resources r = ResourcesServerEvents.resourcesList.stream().filter(x -> p.name.equals(x.ownerName)).findFirst().orElse(null);
            if (r == null)
                continue;
            JsonArray a = new JsonArray(); // food, wood, ore, pop, supply
            a.add(r.food);
            a.add(r.wood);
            a.add(r.ore);
            a.add(UnitServerEvents.getCurrentPopulation(p.name));
            a.add(BuildingServerEvents.getTotalPopulationSupply(p.name));
            resources.add(p.name, a);
        }
        o.add("resources", resources);
        return o;
    }

    private static JsonObject event(String type, String name) {
        JsonObject o = line(type);
        o.addProperty("name", name);
        return o;
    }

    // the game has reset: whoever was still standing won
    private static void end() {
        JsonObject o = line("end");
        JsonArray left = new JsonArray();
        alive.forEach(left::add);
        o.add("left", left);
        write(match, o);
        BotSupervisor.closeLogs();
        for (BufferedWriter w : new BufferedWriter[] { match, commands, snapshots }) {
            try {
                if (w != null)
                    w.close();
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("[Bot] couldn't finish a match recording", e);
            }
        }
        ReignOfNether.LOGGER.info("[Bot] match recorded to {}", dir);
        match = commands = snapshots = null;
        dir = null;
        alive = new HashSet<>();
    }

    private static JsonObject line(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        o.addProperty("seconds", ticks / 20);
        return o;
    }

    private static JsonArray xyz(BlockPos p) {
        JsonArray a = new JsonArray();
        a.add(p.getX());
        a.add(p.getY());
        a.add(p.getZ());
        return a;
    }

    private static void write(@Nullable BufferedWriter w, JsonObject o) {
        if (w == null)
            return;
        try {
            w.write(o + "\n");
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[Bot] can't record the match", e);
        }
    }

    private static void flush() {
        for (BufferedWriter w : new BufferedWriter[] { match, commands, snapshots }) {
            try {
                if (w != null)
                    w.flush();
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("[Bot] can't record the match", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        if (dir != null)
            end();
        ticks = 0;
    }
}
