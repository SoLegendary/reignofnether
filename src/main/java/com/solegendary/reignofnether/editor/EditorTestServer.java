package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ai.BotDifficulty;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The server half of the editor's Test mode (see EditorTest): saves the world, copies its folder aside and starts a
 * real match in place. The copy is the whole state: Stop (on the host's client) puts it back and reopens the world.
 * Whether a world is being tested is just whether its copy exists, so a crash mid-test still ends in a Stop.
 */
public final class EditorTestServer {

    private EditorTestServer() { }

    public static Path world(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
    }

    /** <game dir>/ron-test/<world folder>: outside saves/, so it never shows up as a world of its own. */
    public static Path snapshot(Path world) {
        return world.getParent().resolveSibling("ron-test").resolve(world.getFileName());
    }

    /** Puts the world folder back as it was before the test. Only while its server is stopped. */
    public static void restore(Path world, Path snapshot) throws IOException {
        FileUtils.cleanDirectory(world.toFile()); // also drops files the test added
        FileUtils.copyDirectory(snapshot.toFile(), world.toFile());
        FileUtils.deleteDirectory(snapshot.toFile());
    }

    // only the player hosting the world: Stop has to close and reopen it on their client
    public static void start(ServerPlayer player, Faction faction) {
        MinecraftServer server = player.server;
        String name = player.getName().getString();
        if (server.isDedicatedServer() || !server.isSingleplayerOwner(player.getGameProfile())) {
            error(player, "editor.reignofnether.test.host_only");
            return;
        }
        Component refusal = RTSMapInfoServerEvents.playRefusal();
        if (refusal != null) {
            player.sendSystemMessage(SystemMessage.error(refusal));
            return;
        }
        Path world = world(server);
        Path snapshot = snapshot(world);
        if (Files.exists(snapshot)) {
            error(player, "editor.reignofnether.test.already");
            return;
        }
        if (PlayerServerEvents.rtsPlayers.stream().anyMatch(p -> p.faction != Factions.NONE)) {
            error(player, "editor.reignofnether.test.match_running");
            return;
        }
        // the seats as map.json has them now: the first free player seat is the editor's
        StartPosServerEvents.loadPositionsFromMapInfo();
        StartPos seat = StartPosServerEvents.startPoses.stream()
                .filter(sp -> sp.enabled && !sp.mapBot && (sp.playerName.isBlank() || sp.playerName.equals(name)))
                .findFirst().orElse(null);
        if (seat == null) {
            error(player, "editor.reignofnether.test.no_seat");
            return;
        }

        ServerLevel level = server.overworld();
        // what the mod keeps in memory goes to its save data first, as when the world closes
        PlayerServerEvents.saveRTSPlayers();
        BuildingServerEvents.saveBuildings(level);
        BuildingServerEvents.saveNetherZones(level);
        UnitServerEvents.saveGatherTargets(level);
        UnitServerEvents.saveFallenHeroUnits(level);
        ResourcesServerEvents.saveResources(level);
        server.saveEverything(true, true, true);
        // ponytail: copied on the server thread, a short freeze on big worlds; RTS maps are small
        try {
            FileUtils.copyDirectory(world.toFile(), snapshot.toFile(), f -> !f.getName().equals("session.lock"));
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[EditorTest] could not copy {} to {}", world, snapshot, e);
            FileUtils.deleteQuietly(snapshot.toFile());
            error(player, "editor.reignofnether.test.snapshot_failed", e.getMessage());
            return;
        }
        ReignOfNether.LOGGER.info("[EditorTest] test match started, world saved to {}", snapshot);
        EditorTestClientboundPacket.testing(player);

        // the lobby's start: the editor in their seat, map.json bots in theirs, other seats stay empty
        PlayerServerEvents.resetRTS(false);
        seat.playerName = name;
        seat.faction = seat.fixedFaction != Factions.NONE ? seat.fixedFaction
                : faction == Factions.NONE || faction == Factions.NEUTRAL ? Factions.RANDOM : faction;
        for (StartPos sp : StartPosServerEvents.startPoses) {
            if (!sp.mapBot)
                continue;
            sp.enabled = true;
            StartPosServerEvents.reserveBotPos(sp.pos, Factions.RANDOM, BotDifficulty.NORMAL.ordinal());
            if (!sp.isBot()) // bots can't play here
                sp.enabled = false;
        }
        StartPosServerEvents.syncAll();
        StartPosServerEvents.startGameCountdown(); // ends in StartPosServerEvents' start, RonFiles.matchStart included
        PlayerServerEvents.sendMessageToPlayer(name, "editor.reignofnether.test.started", true);
    }

    private static void error(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(SystemMessage.error(Component.translatable(key, args)));
    }
}
