package com.solegendary.reignofnether.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderWorkerPool;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.LockSupport;

// One headless bot match for the RoN-Bots trainer: fills two seats, runs as fast as asked, appends the result and
// exits. Only with -Dron.arena=<config.json> on a dedicated server; the trainer gives every match a fresh world copy.
public class ArenaServerEvents {

    public static final boolean ACTIVE = System.getProperty("ron.arena") != null;

    private static final int SEAT_AFTER_TICKS = 100;
    private static final int SEAT_TIMEOUT_TICKS = 20 * 60 * 10;
    private static final int LOG_EVERY_TICKS = 1200;
    private static final long PATH_WAIT_NANOS = 5_000_000; // paths get up to this long to land, as they would at 20 TPS

    private enum State { SEATING, STARTING, PLAYING }

    private static State state = State.SEATING;
    private static JsonObject config;
    private static long ticks = 0, matchTicks = 0, matchStartNanos, lastLogNanos, lastLogTick;
    private static double msptSum = 0, msptMax = 0, tickSum = 0;
    private static long tickStartNanos, previousLogTick = 0;
    private static final Map<String, String> factions = new HashMap<>();
    private static final Map<String, Integer> peakPop = new HashMap<>();
    private static final Map<String, Integer> teams = new HashMap<>(); // seat -> team
    private static final Set<String> netSeats = new HashSet<>(); // bots the trainer's net plays, the rest play the script

    // the trainer's net file for this bot, or null: the bot plays its usual net (if any) or the script
    @Nullable
    public static Path netFor(String botName) {
        return ACTIVE && config != null && config.has("net") && netSeats.contains(botName) ? Path.of(config.get("net").getAsString()) : null;
    }

    // training: the net tries choices by their odds instead of always taking its best one
    public static boolean sampling() {
        return ACTIVE && config != null && config.has("sample") && config.get("sample").getAsBoolean();
    }

    // where every bot writes its decisions for the trainer, or null; outside the arena, the recorded match's folder
    @Nullable
    public static Path decisionsDir() {
        if (!ACTIVE)
            return BotRecorder.dir();
        return config != null && config.has("decisions") ? Path.of(config.get("decisions").getAsString()) : null;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (!ACTIVE)
            return;
        if (evt.phase == TickEvent.Phase.START) {
            tickStartNanos = System.nanoTime();
            return;
        }
        MinecraftServer server = evt.getServer();
        ticks++;
        try {
            if (config == null)
                begin(server);
            switch (state) {
                case SEATING -> seat(server);
                case STARTING -> {
                    if (PlayerServerEvents.rtsPlayers.size() >= teams.size()) {
                        state = State.PLAYING;
                        // teammates are allies from the start, as a team lobby would make them
                        teams.forEach((a, ta) -> teams.forEach((b, tb) -> {
                            if (!a.equals(b) && ta.equals(tb) && !AlliancesServerEvents.isAllied(a, b))
                                AlliancesServerEvents.addAlliance(a, b);
                        }));
                        AlliancesServerEvents.syncAlliances();
                        // fog like a real match: scouting and what the net sees under it only mean something with it on
                        com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents.setEnabled(true);
                        matchStartNanos = lastLogNanos = System.nanoTime();
                        ReignOfNether.LOGGER.info("[Arena] match started");
                    } else if (ticks > SEAT_TIMEOUT_TICKS)
                        finish("error: match never started");
                }
                case PLAYING -> play(server);
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("[Arena] failed", e);
            finish("error: " + e);
        }
        applySpeed(server);
    }

    private static void begin(MinecraftServer server) throws Exception {
        config = new Gson().fromJson(Files.readString(Path.of(System.getProperty("ron.arena"))), JsonObject.class);
        for (ServerLevel level : server.getAllLevels())
            level.noSave = true; // every match gets a fresh copy, nothing here is worth writing
    }

    private static void seat(MinecraftServer server) {
        if (ticks > SEAT_TIMEOUT_TICKS) {
            finish("error: no two seats on a bot-ready map");
            return;
        }
        List<StartPos> seats = new ArrayList<>(StartPosServerEvents.startPoses.stream().filter(sp -> sp.enabled).toList());
        if (ticks < SEAT_AFTER_TICKS || seats.size() < 2 || !BotServerEvents.botsAvailable())
            return;
        ServerLevel level = server.overworld();
        JsonArray picks = config.getAsJsonArray("factions");
        int n = picks.size(); // 2: 1v1, 4: 2v2
        if (seats.size() < n) {
            finish("error: the map has " + seats.size() + " seats, the match needs " + n);
            return;
        }
        // 2v2: seat 0 and the seat nearest it are one team, the other two the other
        if (n == 4) {
            StartPos first = seats.get(0);
            StartPos mate = seats.subList(1, seats.size()).stream().min(java.util.Comparator.comparingDouble(sp -> sp.pos.distSqr(first.pos))).orElseThrow();
            seats.remove(mate);
            seats.add(1, mate);
        }
        for (StartPos extra : seats.subList(n, seats.size()))
            StartPosServerEvents.setPosEnabled(extra.pos, false);
        for (int i = 0; i < n; i++) {
            StartPos seat = seats.get(i);
            // vanilla stops ticking entities in a level with no players unless a chunk is forced
            ChunkPos chunk = new ChunkPos(seat.pos);
            level.setChunkForced(chunk.x, chunk.z, true);
            StartPosServerEvents.reserveBotPos(seat.pos, faction(picks.get(i).getAsString()), BotDifficulty.NORMAL.ordinal());
            factions.put(seat.playerName, picks.get(i).getAsString());
            teams.put(seat.playerName, n == 4 ? i / 2 : i);
            if (config.has("netSeats") && config.getAsJsonArray("netSeats").contains(new JsonPrimitive(i)))
                netSeats.add(seat.playerName);
        }
        if (config.has("mapImage") && !Files.exists(Path.of(config.get("mapImage").getAsString())))
            writeMapImage(level, seats.subList(0, n).stream().map(sp -> sp.pos).toList(), Path.of(config.get("mapImage").getAsString()));
        state = State.STARTING;
        ReignOfNether.LOGGER.info("[Arena] seated {}", factions);
    }

    private static final int MAP_PIXELS = 512;

    // a top-down picture of the map for the RoN-Bots Watch page, once per map: the top block's map colour, shaded
    // by height like a vanilla map, plus <png>.json with where it sits in the world
    private static void writeMapImage(ServerLevel level, List<BlockPos> starts, Path png) {
        double[] bounds = BotGrid.bounds(level, starts); // the same square the bots' grid covers
        double minX = bounds[0], minZ = bounds[1], size = bounds[2];
        int px = (int) Math.min(MAP_PIXELS, size);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(px, px, java.awt.image.BufferedImage.TYPE_INT_RGB);
        int[] above = new int[px];
        for (int z = 0; z < px; z++) {
            for (int x = 0; x < px; x++) {
                int bx = (int) (minX + (x + 0.5) * size / px), bz = (int) (minZ + (z + 0.5) * size / px);
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1;
                BlockPos pos = new BlockPos(bx, top, bz);
                int rgb = level.getBlockState(pos).getMapColor(level, pos).col;
                double shade = z == 0 ? 1 : top > above[x] ? 1.15 : top < above[x] ? 0.8 : 1;
                above[x] = top;
                int r = Math.min(255, (int) ((rgb >> 16 & 255) * shade)), g = Math.min(255, (int) ((rgb >> 8 & 255) * shade)), bl = Math.min(255, (int) ((rgb & 255) * shade));
                image.setRGB(x, z, r << 16 | g << 8 | bl);
            }
        }
        try {
            Files.createDirectories(png.getParent());
            javax.imageio.ImageIO.write(image, "png", png.toFile());
            Files.writeString(Path.of(png + ".json"), "{\"minX\":" + minX + ",\"minZ\":" + minZ + ",\"size\":" + size + "}");
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("[Arena] couldn't draw the map", e);
        }
    }

    private static Faction faction(String name) {
        return switch (name) {
            case "monsters" -> Factions.MONSTERS;
            case "piglins" -> Factions.PIGLINS;
            case "villagers" -> Factions.VILLAGERS;
            default -> Factions.RANDOM;
        };
    }

    private static void play(MinecraftServer server) {
        matchTicks++;
        // not getAverageTickTime(): that stops before Forge's end-of-tick events, so it leaves the bots out
        double mspt = (System.nanoTime() - tickStartNanos) / 1e6;
        tickSum += mspt;
        msptSum += mspt;
        msptMax = Math.max(msptMax, mspt);
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                peakPop.merge(p.name, UnitServerEvents.getCurrentPopulation(p.name), Math::max);
        }
        if (matchTicks % LOG_EVERY_TICKS == 0) {
            long now = System.nanoTime();
            double tps = (matchTicks - lastLogTick) / ((now - lastLogNanos) / 1e9);
            lastLogNanos = now;
            lastLogTick = matchTicks;
            ReignOfNether.LOGGER.info("[Arena] minute {}: {} TPS, {} mspt, {} units", matchTicks / 1200,
                    String.format("%.1f", tps), String.format("%.2f", tickSum / (matchTicks - previousLogTick)), UnitServerEvents.getAllUnits().size());
            tickSum = 0;
            previousLogTick = matchTicks;
        }
        if (teamsLeft() <= 1)
            finish("defeat");
        else if (matchTicks >= config.get("maxMinutes").getAsLong() * 1200)
            finish("timeout");
    }

    private static void applySpeed(MinecraftServer server) {
        if (config == null)
            return;
        double speed = config.get("speed").getAsDouble();
        if (speed <= 1)
            return;
        // a slow-landing path costs more ticks the faster we run, which would change how the match plays
        long deadline = System.nanoTime() + PATH_WAIT_NANOS;
        while (PathfinderWorkerPool.inFlight() > 0 && System.nanoTime() < deadline)
            LockSupport.parkNanos(100_000);
        server.nextTickTime -= 50 - Math.round(50 / speed);
    }

    static int team(String name) {
        return teams.getOrDefault(name, 0);
    }

    private static long teamsLeft() {
        synchronized (PlayerServerEvents.rtsPlayers) {
            return PlayerServerEvents.rtsPlayers.stream().map(p -> teams.getOrDefault(p.name, -1)).distinct().count();
        }
    }

    private static List<String> teamOf(String name) {
        return teams.entrySet().stream().filter(e -> e.getValue().equals(teams.get(name))).map(Map.Entry::getKey).sorted().toList();
    }

    // no server.halt(): saving a throwaway world only slows the next match down
    private static void finish(String reason) {
        BotSupervisor.closeLogs();
        JsonObject result = new JsonObject();
        result.addProperty("reason", reason);
        result.addProperty("matchTicks", matchTicks);
        result.addProperty("gameMinutes", matchTicks / 1200.0);
        result.addProperty("wallSeconds", matchStartNanos == 0 ? 0 : (System.nanoTime() - matchStartNanos) / 1e9);
        result.addProperty("avgMspt", matchTicks == 0 ? 0 : msptSum / matchTicks);
        result.addProperty("maxMspt", msptMax);
        List<RTSPlayer> left = new ArrayList<>(PlayerServerEvents.rtsPlayers);
        // a team: its players, comma separated, whoever of them is still alive
        result.addProperty("winner", teamsLeft() == 1 ? String.join(",", teamOf(left.get(0).name)) : "");
        JsonArray players = new JsonArray();
        factions.forEach((name, faction) -> {
            JsonObject p = new JsonObject();
            p.addProperty("name", name);
            p.addProperty("faction", faction);
            p.addProperty("peakPop", peakPop.getOrDefault(name, 0));
            p.addProperty("net", netSeats.contains(name));
            p.addProperty("team", teams.getOrDefault(name, 0));
            p.addProperty("finalPop", PlayerServerEvents.inRtsList(name) ? UnitServerEvents.getCurrentPopulation(name) : 0);
            RTSPlayer rts = PlayerServerEvents.getRTSPlayer(name);
            for (RTSPlayer gone : PlayerServerEvents.postGameRtsPlayers)
                if (rts == null && gone != null && gone.name.equals(name))
                    rts = gone;
            if (rts != null)
                rts.scores.getScoreList().forEach((score, value) -> p.addProperty(score.name(), value));
            players.add(p);
        });
        result.add("players", players);
        try {
            Files.writeString(Path.of(config.get("results").getAsString()), result + "\n",
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("[Arena] couldn't write the result", e);
        }
        ReignOfNether.LOGGER.info("[Arena] finished: {}", result);
        Runtime.getRuntime().halt(reason.startsWith("error") ? 1 : 0);
    }
}
