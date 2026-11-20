package com.solegendary.reignofnether.rtsmap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.guide.GuideServer;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.taskscheduler.TaskSchedulerServerEvents;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

// a map's own folder, <world>/ron/: map.json (what the map is) and .mcfunction command files (what it does)
public class RonFiles {

    // files running right now: one that sets off its own event again (research_done adding research) is skipped
    // instead of recursing until the server crashes
    private static final Set<String> running = WorldState.server(new HashSet<>());
    @Nullable
    private static String currentOwner = null; // $owner of the file running now, for the files it runs

    @Nullable
    public static String currentOwner() {
        return currentOwner;
    }

    public static Path dir(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().resolve("ron");
    }

    // runs ron/<path>.mcfunction as an op would, read fresh every time so hand edits apply at once;
    // one command per line like a datapack function, # comments, a missing file does nothing.
    // Each line runs on its own, so one bad line is reported and the rest still run
    public static void run(MinecraftServer server, String path, CommandSourceStack source) {
        run(server, path, source, null);
    }

    // owner: the seat this runs for (a building's or research's owner), see withOwner
    public static void run(MinecraftServer server, String path, CommandSourceStack source, @Nullable String owner) {
        Path dir = dir(server);
        Path file = dir.resolve(path + ".mcfunction").normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file))
            return;
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (Exception e) {
            error(server, "ron/" + path + ".mcfunction: " + e.getMessage());
            return;
        }
        runLines(server, path, lines, 0, source, owner);
    }

    // rtsapi wait <time> / rtsapi wait for <event> / rtsapi wait until <condition>: the rest of the file runs after that
    // long (vanilla time: 60, 60t, 3s, 1d), when the event next fires (for the same owner when both have one), or on the
    // first second the condition (what goes between execute and run: if score #map army matches 3..) holds,
    // as the same source and owner
    private static final Pattern WAIT = Pattern.compile("/?rtsapi wait (?:until (.+)|for (\\S+)|([0-9.]+[tsd]?))");

    private record Paused(String path, List<String> lines, int from, CommandSourceStack source, @Nullable String owner, int generation,
                          @Nullable String until) { }

    private static final Map<String, List<Paused>> waiting = WorldState.server(new HashMap<>());

    // the events some file waits for, so the client sends those inputs (GuideServer.listened)
    public static Set<String> waitedEvents() {
        return waiting.keySet();
    }

    private static void runLines(MinecraftServer server, String path, List<String> lines, int from, CommandSourceStack source, @Nullable String owner) {
        // a file going on after a wait isn't inside itself any more, so it may run itself again: that's a loop
        boolean guarded = from == 0;
        if (guarded && !running.add(path)) {
            error(server, "ron/" + path + ".mcfunction runs itself again, skipped");
            return;
        }
        String outerOwner = currentOwner;
        if (owner != null)
            currentOwner = owner; // files it runs (rtsapi run) get the same $owner
        try {
            // op level for parsing too, or op-only commands don't exist for a unit's or player's source
            CommandSourceStack opSource = source.withPermission(2).withSuppressedOutput();
            for (int i = from; i < lines.size(); i++) {
                String line = withOwner(lines.get(i).strip(), currentOwner);
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                Matcher wait = WAIT.matcher(line);
                if (wait.matches()) {
                    pause(server, new Paused(path, lines, i + 1, source, currentOwner, scheduleGeneration, wait.group(1)),
                            wait.group(1) != null ? "second" : wait.group(2), wait.group(3));
                    return;
                }
                line = withScores(server, line, currentOwner);
                if (!ensureObjective(server, line))
                    continue;
                // "?" first: a check that may fail (no building found, objective already there), never reported
                boolean quiet = line.startsWith("?");
                if (quiet)
                    line = line.substring(1).strip();
                try {
                    server.getCommands().getDispatcher().execute(line.startsWith("/") ? line.substring(1) : line, opSource);
                } catch (Exception e) {
                    if (!quiet)
                        error(server, "ron/" + path + ".mcfunction line " + (i + 1) + ": " + e.getMessage());
                }
            }
        } finally {
            currentOwner = outerOwner;
            if (guarded)
                running.remove(path);
        }
    }

    /** The scoreholder a map's own variables are kept on (a # name, so the vanilla sidebar doesn't list it). */
    public static final String MAP_HOLDER = "#map";

    private static final Pattern SCORE_REF = Pattern.compile("\\{([A-Za-z0-9_.+-]{1,16})}");
    private static final Pattern SCORE_USE = Pattern.compile("(?:scoreboard players \\w+|\\bscore) (\\S+) ([A-Za-z0-9_.+-]{1,16})(?:\\s|$)");

    // {wave} in a line is the wave scoreboard's value: the owner's when they have one, else the map's (#map);
    // a name that isn't an objective, or has no value yet, stays as written
    private static String withScores(MinecraftServer server, String line, @Nullable String owner) {
        if (line.indexOf('{') < 0)
            return line;
        var board = server.getScoreboard();
        return SCORE_REF.matcher(line).replaceAll(m -> {
            var objective = board.getObjective(m.group(1));
            if (objective == null)
                return Matcher.quoteReplacement(m.group());
            String holder = owner != null && board.hasPlayerScore(owner, objective) ? owner : MAP_HOLDER;
            return board.hasPlayerScore(holder, objective) ? String.valueOf(board.getOrCreatePlayerScore(holder, objective).getScore())
                    : Matcher.quoteReplacement(m.group());
        });
    }

    private static final Pattern OBJECTIVE_ADD = Pattern.compile("\\??/?scoreboard objectives add (\\S+) (\\S+).*");
    // the objectives made that way: a later scoreboard objectives add for one of them replaces it or is skipped
    private static final Set<String> autoMade = WorldState.server(new HashSet<>());

    // a variable is just a vanilla scoreboard: the first line that uses one makes it (a dummy objective), so no file
    // has to add it first. False: the line adds an objective that's already there that way, so it's skipped
    private static boolean ensureObjective(MinecraftServer server, String line) {
        var board = server.getScoreboard();
        Matcher add = OBJECTIVE_ADD.matcher(line);
        if (add.matches() && autoMade.remove(add.group(1))) {
            var made = board.getObjective(add.group(1));
            if (made == null || !add.group(2).equals("dummy")) { // a real criteria (resources.food...) takes its place
                if (made != null)
                    board.removeObjective(made);
                return true;
            }
            return false;
        }
        Matcher m = SCORE_USE.matcher(line);
        while (m.find())
            if (board.getObjective(m.group(2)) == null) {
                board.addObjective(m.group(2), ObjectiveCriteria.DUMMY, Component.literal(m.group(2)),
                        ObjectiveCriteria.RenderType.INTEGER);
                autoMade.add(m.group(2));
            }
        return true;
    }

    // vanilla's time units: a plain number is ticks, t ticks, s seconds, d days
    private static int ticks(String time) {
        char unit = time.charAt(time.length() - 1);
        float n = Float.parseFloat(Character.isLetter(unit) ? time.substring(0, time.length() - 1) : time);
        return Math.round(n * (unit == 's' ? 20 : unit == 'd' ? 24000 : 1));
    }

    private static void pause(MinecraftServer server, Paused paused, @Nullable String event, @Nullable String seconds) {
        if (event != null) {
            boolean input = !waiting.containsKey(event);
            waiting.computeIfAbsent(event, k -> new ArrayList<>()).add(paused);
            if (input) // an input event (unit_selected...) is only sent by clients that know someone listens
                GuideServer.syncListened(server, server.getPlayerList().getPlayers());
            return;
        }
        int ticks;
        try {
            ticks = Math.max(1, ticks(seconds));
        } catch (NumberFormatException e) {
            error(server, "ron/" + paused.path + ".mcfunction: bad wait " + seconds);
            return;
        }
        TaskSchedulerServerEvents.schedule(ticks - 1, () -> resume(server, paused));
    }

    private static void resume(MinecraftServer server, Paused p) {
        if (p.generation == scheduleGeneration)
            runLines(server, p.path, p.lines, p.from, p.source, p.owner);
    }

    // files waiting for this event go on; one for another owner keeps waiting
    private static void wake(MinecraftServer server, String event, @Nullable String owner) {
        List<Paused> list = waiting.remove(event);
        if (list == null)
            return;
        List<Paused> stay = new ArrayList<>();
        for (Paused p : list)
            if (p.owner != null && owner != null && !p.owner.equals(owner) || p.until != null && !holds(server, p))
                stay.add(p);
            else
                resume(server, p);
        if (!stay.isEmpty())
            waiting.computeIfAbsent(event, k -> new ArrayList<>()).addAll(stay);
    }

    // a wait until's condition, run as execute <condition> run return 1; a broken one is reported and never holds
    private static boolean holds(MinecraftServer server, Paused p) {
        try {
            return server.getCommands().getDispatcher().execute("execute " + withScores(server, p.until, p.owner) + " run return 1",
                    p.source.withPermission(2).withSuppressedOutput()) > 0;
        } catch (Exception e) {
            error(server, "ron/" + p.path + ".mcfunction: wait until " + p.until + ": " + e.getMessage());
            return false;
        }
    }

    // $owner is the owner's name, always quoted so seat names with spaces work. @s is left as it is: it's the
    // source's (a unit, the building at the source's position, or the owner itself via asOwner). With no owner
    // (rtsapi run outside a script, the Editor's Run, a neutral building or unit) $owner stays as written
    public static String withOwner(String line, @Nullable String owner) {
        if (owner == null || owner.isBlank())
            return line;
        // bare when it can be (vanilla score holders would keep the quotes), quoted when it has spaces or symbols
        String name = owner.matches("[A-Za-z0-9_.+-]+") ? owner : '"' + owner.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        return line.replace("$owner", name);
    }

    // errors already shown: a trigger that fails every cooldown (a neutral building's @s) is reported once, not each time
    private static final Set<String> reported = WorldState.server(new HashSet<>());

    // a file was saved: its errors may be fixed, or new
    public static void forgetErrors() {
        reported.clear();
    }

    private static void error(MinecraftServer server, String msg) {
        if (!reported.add(msg))
            return;
        ReignOfNether.LOGGER.error(msg);
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            if (PlayerServerEvents.isOpOrHost(player)) {
                player.sendSystemMessage(SystemMessage.error(Component.literal(msg)));
                RonFileClientboundPacket.send(player, RonFileAction.LOG, "", msg);
            }
    }

    // ron/events/<event>.mcfunction, run where the event happened so ~ ~ ~ and @s work inside it
    public static void fire(MinecraftServer server, String event, CommandSourceStack source) {
        fire(server, event, source, null);
    }

    public static void fire(MinecraftServer server, String event, CommandSourceStack source, @Nullable String owner) {
        run(server, "events/" + event, source, owner);
        wake(server, event, owner);
    }

    public static void fire(MinecraftServer server, String event, Vec3 pos) {
        fire(server, event, server.createCommandSourceStack().withPosition(pos));
    }

    // for events of a player's: the player while they're online, else a source named after the owner (asOwner)
    public static CommandSourceStack ownerSource(MinecraftServer server, String ownerName) {
        ServerPlayer player = server.getPlayerList().getPlayerByName(ownerName);
        return player != null ? player.createCommandSourceStack() : asOwner(server.createCommandSourceStack(), ownerName);
    }

    // source as the owner's, keeping its position and level: the owner's player while online, else (a bot, an
    // offline player, a seat nobody plays) a source with no entity named after them, which @s and rtsapi player
    // names resolve to (PlayerNameArgument.sourceOwner). A blank owner (neutral) keeps source
    public static CommandSourceStack asOwner(CommandSourceStack source, @Nullable String ownerName) {
        if (ownerName == null || ownerName.isBlank())
            return source;
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(ownerName);
        if (player != null)
            return source.withEntity(player);
        return new CommandSourceStack(CommandSource.NULL, source.getPosition(), source.getRotation(), source.getLevel(),
                2, ownerName, Component.literal(ownerName), source.getServer(), null);
    }

    // bumped by rtsapi schedule clear, at match end and when the world closes: files scheduled before then don't run
    private static int scheduleGeneration = 0;

    static {
        WorldState.onServerReset(() -> {
            cancelScheduled();
            currentOwner = null;
        });
    }

    // runs ron/<path>.mcfunction after ticks (20 a second), as source and for owner; a file may schedule itself again
    public static void schedule(MinecraftServer server, int ticks, String path, CommandSourceStack source, @Nullable String owner) {
        int generation = scheduleGeneration;
        TaskSchedulerServerEvents.schedule(Math.max(0, ticks - 1), () -> {
            if (generation == scheduleGeneration)
                run(server, path, source, owner);
        });
    }

    public static void cancelScheduled() {
        scheduleGeneration++;
        waiting.clear();
    }

    // a real match: someone plays a faction (the Editor alone isn't one), for the events that would fire while editing
    public static boolean matchRunning() {
        return PlayerServerEvents.rtsPlayers.stream()
                .anyMatch(p -> p.faction != Factions.NONE);
    }

    // ron/lang/<locale>.json files as one {"en_us": {...}, "fr_fr": {...}}: the map's own text for say, highlight and
    // seat names, in every language it has; the client picks its own, else en_us
    public static String langJson(MinecraftServer server) {
        JsonObject all = new JsonObject();
        Path dir = dir(server).resolve("lang");
        if (!Files.isDirectory(dir))
            return "{}";
        try (var files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    all.add(file.getFileName().toString().replace(".json", "").toLowerCase(Locale.ROOT),
                            JsonParser.parseString(Files.readString(file)).getAsJsonObject());
                } catch (Exception e) {
                    error(server, "ron/lang/" + file.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to list " + dir + ": " + e.getMessage());
        }
        return all.toString();
    }

    // the hand-written start, then the event
    public static void matchStart(MinecraftServer server) {
        CommandSourceStack source = server.createCommandSourceStack();
        run(server, "main", source);
        fire(server, "match_start", source);
    }
}
