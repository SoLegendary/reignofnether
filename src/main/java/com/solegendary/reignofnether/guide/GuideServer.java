package com.solegendary.reignofnether.guide;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.rtsmap.RonEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Server side of the guide: the files dialogue lines run as they're shown (say ... then <file>), and the player input
 * events (camera moved, unit selected...) clients only send while the map has a file for them.
 */
public class GuideServer {

    // players a script took control from (rtsapi player control <players> false): their orders are refused
    private static final Set<String> locked = WorldState.server(new HashSet<>());

    public static void setControl(ServerPlayer player, boolean control) {
        if (control)
            locked.remove(player.getName().getString());
        else
            locked.add(player.getName().getString());
    }

    // gives control back (player control <player> true), so a cutscene can't outlive its match or its player's side
    public static void release(ServerPlayer player) {
        if (locked.remove(player.getName().getString()))
            GuideClientboundPacket.control(List.of(player), true);
    }

    public static void releaseAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers())
            release(player);
        locked.clear();
    }

    public static boolean isLocked(ServerPlayer player) {
        return locked.contains(player.getName().getString());
    }

    // callback id -> file, per player: only ids the server handed out run, so a client can't name its own file
    private static final Map<String, Map<Integer, String>> callbacks = WorldState.server(new HashMap<>());
    private static int nextId = 0;

    static {
        WorldState.onServerReset(() -> nextId = 0);
    }

    public static int addCallback(ServerPlayer player, String file) {
        int id = nextId++;
        callbacks.computeIfAbsent(player.getName().getString(), k -> new HashMap<>()).put(id, file);
        return id;
    }

    static void lineShown(ServerPlayer player, String value) {
        Map<Integer, String> mine = callbacks.get(player.getName().getString());
        String file;
        try {
            file = mine == null ? null : mine.remove(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return;
        }
        if (file != null)
            RonFiles.run(player.server, file, player.createCommandSourceStack(), player.getName().getString());
    }

    // once per login, when their loading screen is done: scripts run before it would play behind the animation
    private static final Set<String> readied = WorldState.server(new HashSet<>());

    static void ready(ServerPlayer player) {
        if (!readied.add(player.getName().getString()))
            return;
        if (RTSMapInfoServerEvents.isEditable())
            RonFiles.fire(player.server, "player_joined", player.createCommandSourceStack(), player.getName().getString());
        StartPosServerEvents.autoStart(player);
    }

    public static void loggedOut(ServerPlayer player) {
        readied.remove(player.getName().getString());
    }

    // players sent dialogue (rtsapi say) that haven't finished reading it: only they can end it, once
    private static final Set<String> talking = WorldState.server(new HashSet<>());

    public static void said(ServerPlayer player) {
        talking.add(player.getName().getString());
    }

    static void dialogueEnded(ServerPlayer player) {
        if (talking.remove(player.getName().getString()) && PlayerServerEvents.isRTSPlayer(player.getName().getString()))
            RonFiles.fire(player.server, "dialogue_end", player.createCommandSourceStack(), player.getName().getString());
    }

    // server tick of each player's last input event: one a tick, so a modified client can't flood the scripts
    private static final Map<String, Integer> lastInput = WorldState.server(new HashMap<>());

    static void input(ServerPlayer player, String event) {
        Integer last = lastInput.put(player.getName().getString(), player.server.getTickCount());
        if (last != null && last == player.server.getTickCount())
            return;
        String base = event.contains("/") ? event.substring(0, event.indexOf('/')) : event;
        if (!RonEvents.INPUT.contains(base) || !event.matches("[a-z0-9_./-]{1,64}") || event.contains(".."))
            return;
        if (!PlayerServerEvents.isRTSPlayer(player.getName().getString()))
            return;
        RonFiles.fire(player.server, base, player.createCommandSourceStack(), player.getName().getString());
        if (!base.equals(event))
            RonFiles.fire(player.server, event, player.createCommandSourceStack(), player.getName().getString());
    }

    // the input events this map has files for or a paused file waits for, e.g. "unit_selected" for events/unit_selected/villager.mcfunction
    public static Set<String> listened(MinecraftServer server) {
        Path events = RonFiles.dir(server).resolve("events");
        Set<String> found = new HashSet<>();
        for (String event : RonEvents.INPUT)
            if (Files.isRegularFile(events.resolve(event + ".mcfunction")) || Files.isDirectory(events.resolve(event)))
                found.add(event);
        for (String event : RonFiles.waitedEvents()) // rtsapi wait for unit_selected/villager
            if (RonEvents.INPUT.contains(event.split("/")[0]))
                found.add(event.split("/")[0]);
        return found;
    }

    // to one player on join, or everyone after a file was saved
    public static void syncListened(MinecraftServer server, Iterable<ServerPlayer> players) {
        String value = listened(server).stream().sorted().collect(Collectors.joining(","));
        for (ServerPlayer player : players)
            GuideClientboundPacket.listen(player, value);
    }
}
