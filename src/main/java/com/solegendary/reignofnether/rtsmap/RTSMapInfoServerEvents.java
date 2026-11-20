package com.solegendary.reignofnether.rtsmap;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import com.google.gson.JsonPrimitive;
import net.minecraftforge.server.ServerLifecycleHooks;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamemode.GameModeClientboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.guide.GuideServer;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayerSaveData;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.util.WorldState;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraft.world.level.storage.LevelResource;

import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RTSMapInfoServerEvents {

    // mod gamerules that are facts about the map rather than lobby options, for the Editor's Map tab
    // (map.json "rules" can still hold any gamerule, vanilla or mod)
    public static final List<String> MAP_RULES = List.of("groundYLevel", "flyingMaxYLevel",
            "allowBeacons", "slantedBuilding", "buildingsOutsideBorder", "animalSpawnYDiff",
            "doNetherConversion", "doUnitGriefing", "doPlayerGriefing");

    public static RTSMapInfo rtsMapInfo = null;

    // the map.json seat at pos's start (RTSMapInfo.Seat.start); "capitol" for start blocks and seatless matches
    public static String seatStart(BlockPos pos) {
        RTSMapInfo.Seat seat = rtsMapInfo == null ? null : rtsMapInfo.getSeat(pos);
        return seat == null ? "capitol" : seat.getStart();
    }
    private static JsonObject savedJson = null; // rtsMapInfo as last read from or written to map.json, what setAndSave diffs against
    private static boolean lockedByTeamMode = false;

    // Map formats (see RTSMapInfoLoader.mapFormat), only SUPPORTED_FORMAT is ever loaded, played or written:
    // 1: rtsmap.json, start blocks, custom buildings and scenario roles in data/. Only MapConverter reads it, when the
    //    mapmaker converts the map; until then the world opens with none of it and can't be played.
    // 2: ron/map.json with "format": 2, plus ron/buildings and ron/*.mcfunction. A world with no old data is a new
    //    format 2 map.
    // Newer: from a later mod version: like 1, but with no Convert
    public static final int SUPPORTED_FORMAT = 2;
    static int format = 0; // read when the world opens, 0 = not yet; MapConverter sets it
    static {
        WorldState.onServerReset(() -> {
            lockedByTeamMode = false;
            rtsMapInfo = null;
            savedJson = null;
            format = 0;
        });
    }

    public static int mapFormat() {
        if (format == 0) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null)
                return 1;
            format = RTSMapInfoLoader.mapFormat(server.getWorldPath(LevelResource.ROOT));
        }
        return format;
    }

    // ron/ and map.json may be written
    public static boolean isEditable() {
        return mapFormat() == SUPPORTED_FORMAT;
    }

    // why ron/ can't be written here, null when it can
    @Nullable
    public static Component writeRefusal() {
        int format = mapFormat();
        if (format == SUPPORTED_FORMAT)
            return null;
        return Component.translatable(format < SUPPORTED_FORMAT ? "map.reignofnether.format.convert_first" : "map.reignofnether.format.newer", format);
    }

    // why no match can start here, null when one can: an old map is Editor-only until converted, a newer one needs the update
    @Nullable
    public static Component playRefusal() {
        int format = mapFormat();
        if (format == SUPPORTED_FORMAT)
            return null;
        return Component.translatable(format < SUPPORTED_FORMAT ? "map.reignofnether.format.play_old" : "map.reignofnether.format.play_newer");
    }

    // a new map without map.json gets one at its first ron/ write, so it's never taken for an old map when it reopens
    public static void claimFormat(MinecraftServer server) {
        if (isEditable() && !Files.exists(RTSMapInfoLoader.path(server)))
            setAndSave(server, rtsMapInfo != null ? rtsMapInfo : new RTSMapInfo());
    }

    public static boolean usingMapInfoStartPositions() {
        return rtsMapInfo != null && (rtsMapInfo.getModes().isEmpty() || rtsMapInfo.supportsMode(rtsMapInfo.getSelectedMode()));
    }

    // the map locks this match rule (map.json lockedRules), only ops can change it
    public static boolean isLocked(String rule) {
        return rtsMapInfo != null && rtsMapInfo.getLockedRules().contains(rule);
    }

    public static boolean isTeamMode() {
        return usingMapInfoStartPositions() && rtsMapInfo.isTeamMode();
    }

    // a team mode keeps its teams, so alliances stay locked, then unlock when it ends (the host can relock them);
    // tells clients so they can show it
    public static void syncTeamMode() {
        boolean teamMode = isTeamMode();
        RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_TEAM_MODE, String.valueOf(teamMode));
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && (teamMode || lockedByTeamMode)) {
            server.getGameRules().getRule(GameRuleRegistrar.LOCK_ALLIANCES).set(teamMode, server);
            GameruleClientboundPacket.setLockAlliances(teamMode);
        }
        lockedByTeamMode = teamMode;
    }

    // the Editor (or anything else) changed the map: what changed is merged into ron/map.json on disk, and the result
    // (with any hand edits made since) is used from now on
    // Only on a format 2 map, false on any other (see writeRefusal)
    public static boolean setAndSave(MinecraftServer server, RTSMapInfo info) {
        if (!isEditable())
            return false;
        info.setFormat(SUPPORTED_FORMAT);
        info.sanitize();
        JsonObject before = savedJson != null ? savedJson : RTSMapInfoLoader.toTree(new RTSMapInfo());
        RTSMapInfo saved = RTSMapInfoLoader.save(server, info, before);
        if (saved == null)
            return false;
        // the lobby's pick stays, unless the map's default mode itself was changed
        if (rtsMapInfo != null && new JsonPrimitive(info.getDefaultMode()).equals(before.get("defaultMode")))
            saved.setSelectedMode(rtsMapInfo.selectedMode);
        // only rules that changed are set again, so a map edit doesn't undo the host's lobby settings
        Map<String, JsonPrimitive> changedRules = new LinkedHashMap<>(saved.getRules());
        changedRules.entrySet().removeIf(rule -> rule.getValue().equals(before.getAsJsonObject("rules").get(rule.getKey())));
        rtsMapInfo = saved;
        savedJson = RTSMapInfoLoader.toTree(saved);
        StartPosServerEvents.loadPositionsFromMapInfo();
        applyRules(server, changedRules);
        syncTeamMode();
        syncJson(server);
        return true;
    }

    // the whole map.json for the Editor's map tab, plus the current value of each MAP_RULES gamerule (shown, not saved,
    // for the rules map.json doesn't list), then the lobby's selected mode
    public static void syncJson(MinecraftServer server) {
        syncJson(server, null);
    }

    // to one player (who just joined), or everyone when null
    public static void syncJson(MinecraftServer server, @Nullable ServerPlayer to) {
        JsonObject json = RTSMapInfoLoader.toTree(rtsMapInfo != null ? rtsMapInfo : new RTSMapInfo());
        JsonObject current = new JsonObject();
        Map<String, GameRules.Key<?>> keys = new HashMap<>();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                keys.put(key.getId(), key);
            }
        });
        for (String rule : MAP_RULES) {
            if (!keys.containsKey(rule))
                continue;
            GameRules.Value<?> value = server.getGameRules().getRule(keys.get(rule));
            current.add(rule, value instanceof GameRules.BooleanValue b ? new JsonPrimitive(b.get())
                    : value instanceof GameRules.IntegerValue i ? new JsonPrimitive(i.get()) : new JsonPrimitive(value.serialize()));
        }
        json.add(RTSMapInfoLoader.CURRENT_RULES, current);
        send(to, RTSMapInfoAction.SET_JSON, RTSMapInfoLoader.toJson(json));
        if (rtsMapInfo != null)
            send(to, RTSMapInfoAction.SET_MODE, rtsMapInfo.getSelectedMode());
    }

    private static void send(@Nullable ServerPlayer to, RTSMapInfoAction action, String value) {
        if (to != null)
            RTSMapInfoClientboundPacket.sendTo(to, action, value);
        else
            RTSMapInfoClientboundPacket.sendValue(action, value);
    }

    // sets each of these map.json gamerules, through /gamerule so clients and cached values follow as if typed
    public static void applyRules(MinecraftServer server, Map<String, JsonPrimitive> rules) {
        Map<String, GameRules.Key<?>> keys = new HashMap<>();
        GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                keys.put(key.getId(), key);
            }
        });
        for (Map.Entry<String, JsonPrimitive> rule : rules.entrySet()) {
            if (rule.getKey().equals("fogOfWar")) {
                // on once the level ticks (an RTS map only)
                if (rule.getValue().getAsBoolean())
                    FogOfWarServerEvents.restoreOnLoad(server.overworld(), true);
                else if (!rule.getValue().getAsBoolean() && FogOfWarServerEvents.isEnabled())
                    FogOfWarServerEvents.setEnabled(false);
                continue;
            }
            GameRules.Key<?> key = keys.get(rule.getKey());
            if (key == null) {
                ReignOfNether.LOGGER.warn("map.json rules: unknown gamerule " + rule.getKey());
                continue;
            }
            String value = rule.getValue().getAsString();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                    "gamerule " + key.getId() + " " + value);
            String now = server.getGameRules().getRule(key).serialize();
            if (!now.equals(value))
                ReignOfNether.LOGGER.warn("map.json rules: " + key.getId() + " " + value + " is not a valid value, it stays " + now);
        }
    }

    @SubscribeEvent
    public static void loadInfo(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        // ron/map.json; an old or newer format map loads nothing (see playRefusal)
        if (level != null) {
            ReignOfNether.LOGGER.info("Map format: " + mapFormat());
            rtsMapInfo = isEditable() ? RTSMapInfoLoader.load(level) : null;
            savedJson = rtsMapInfo != null ? RTSMapInfoLoader.toTree(rtsMapInfo) : null;
            if (rtsMapInfo != null) {
                ReignOfNether.LOGGER.info("Loaded rtsMapInfo: " +
                        rtsMapInfo.getName() + '|' +
                        rtsMapInfo.getDescription() + '|' +
                        rtsMapInfo.getVersion() + '|' +
                        rtsMapInfo.getAuthor());
                // a saved match keeps the rules it was started with
                Map<String, JsonPrimitive> rules = new LinkedHashMap<>(rtsMapInfo.getRules());
                if (RTSPlayerSaveData.getInstance(level).rtsPlayers.stream().anyMatch(p -> p.faction != Factions.NONE))
                    rules.keySet().removeAll(RTSMapInfo.MATCH_RULES);
                applyRules(evt.getServer(), rules);
                // ron/events/map_loaded: once when the world opens, before anyone has joined (see player_joined)
                RonFiles.fire(evt.getServer(), "map_loaded", evt.getServer().createCommandSourceStack());
                // v1 scenario units that joined before the map was read
                for (ServerLevel l : evt.getServer().getAllLevels())
                    for (Entity entity : l.getAllEntities())
                        MapConverter.claimLegacyUnit(entity);
            }
        }
    }

    // a v1 scenario unit loaded after its map was converted
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (!evt.getLevel().isClientSide())
            MapConverter.claimLegacyUnit(evt.getEntity());
    }

    // only to the player who joined: everyone else has it already
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!(evt.getEntity() instanceof ServerPlayer player) || player.getServer() == null)
            return;
        send(player, RTSMapInfoAction.SET_FORMAT, String.valueOf(mapFormat()));
        // an old format map can only be converted: its mapmaker lands in the Editor, and nobody can switch to a game
        if (mapFormat() < SUPPORTED_FORMAT) {
            GameModeClientboundPacket.setAndLockAllClientGameModes(
                    GameMode.SANDBOX);
            if (PlayerServerEvents.isOpOrHost(player))
                player.server.execute(() -> { // once PlayerServerEvents has them in its player list
                    if (!PlayerServerEvents.isRTSPlayer(player.getName().getString()))
                        PlayerServerEvents.startRTS(player.getId(), player.position(),
                                Factions.NONE);
                });
        }
        syncJson(player.getServer(), player);
        RonFilesServer.sendList(player);
        if (isEditable()) {
            send(player, RTSMapInfoAction.SET_LANG, RonFiles.langJson(player.server));
            GuideServer.syncListened(player.server, List.of(player));
            // ron/events/player_joined waits for their loading screen to finish (GuideServer.ready)
        }
        if (rtsMapInfo != null) {
            // the loading screen shows the name, authors, version and description, so chat stays clear
            send(player, RTSMapInfoAction.SET_MAP_NAME, rtsMapInfo.getName());
            for (String author : rtsMapInfo.getAuthor())
                send(player, RTSMapInfoAction.ADD_AUTHOR, author);
            send(player, RTSMapInfoAction.SET_DESCRIPTION, rtsMapInfo.getDescription());
            send(player, RTSMapInfoAction.SET_MODE, rtsMapInfo.getSelectedMode());
            send(player, RTSMapInfoAction.SET_VERSION, rtsMapInfo.getVersion());
            syncTeamMode();
            for (String mode : rtsMapInfo.getModeNames())
                send(player, RTSMapInfoAction.ADD_MODE, mode);
        }
    }
}
