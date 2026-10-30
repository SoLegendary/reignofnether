package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.gamemode.GameModeServerEvents;
import com.solegendary.reignofnether.gamerules.GameruleServerEvents;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.scenario.ScenarioServerEvents;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records every RTS match on the server (singleplayer, P2P host and dedicated alike) and sends the finished
 * replay to players who ask for it from the end screen. Also keeps the replay world's own server quiet while a
 * replay is being watched, see ReplayClientEvents.
 */
public class ReplayServerEvents {

    private static final int MAX_DOWNLOADS_PER_REPLAY = 3;
    private static final long MAX_DOWNLOAD_BYTES = 512L * 1024 * 1024;
    private static final int CHUNKS_PER_TICK = 4;

    private static volatile ReplayRecorder recorder = null;
    private static boolean matchOver = false; // finished, but rtsPlayers isn't cleared until the next reset
    private static final Set<String> lastRtsPlayers = new HashSet<>();

    // the start snapshot sends the usual join syncs, recorded once and kept from the real players
    private static volatile boolean capturing = false;
    private static Thread captureThread = null;

    private static volatile Path lastReplay = null;
    private static volatile boolean writing = false;
    private static volatile Set<String> lastRecipients = Set.of(); // only players who were in the match may download it

    private static class Transfer {
        Path file;
        long offset, size;
    }
    private static final Map<UUID, Transfer> transfers = new HashMap<>();
    private static final Map<UUID, Integer> downloadCounts = new HashMap<>();

    // the throwaway world ReplayClientEvents watches replays in: its server only sends the bare connection
    public static final String PLAYBACK_WORLD = "ronreplay-playback";
    public static volatile boolean playbackWorld = false;
    private static final Set<Class<?>> PLAYBACK_ALLOWED = Set.of(
            ClientboundLoginPacket.class, ClientboundKeepAlivePacket.class, ClientboundPingPacket.class,
            ClientboundDisconnectPacket.class, ClientboundRespawnPacket.class, ClientboundPlayerPositionPacket.class,
            ClientboundPlayerAbilitiesPacket.class, ClientboundGameEventPacket.class, ClientboundSetHealthPacket.class,
            ClientboundSetExperiencePacket.class, ClientboundPlayerInfoUpdatePacket.class,
            ClientboundCommandsPacket.class, ClientboundUpdateRecipesPacket.class, ClientboundUpdateTagsPacket.class,
            ClientboundUpdateAdvancementsPacket.class, ClientboundSetCarriedItemPacket.class,
            ClientboundContainerSetContentPacket.class, ClientboundContainerSetSlotPacket.class,
            ClientboundServerDataPacket.class, ClientboundChangeDifficultyPacket.class,
            ClientboundSetDefaultSpawnPositionPacket.class, ClientboundUpdateEnabledFeaturesPacket.class
    );

    /** From MixinNetworkManager, for every packet any server sends a player. @return true to not send it */
    public static boolean onSend(ServerPlayer player, Packet<?> packet) {
        if (playbackWorld) // other channels are Forge's own, like opening the RTS screen's container
            return packet instanceof ClientboundCustomPayloadPacket cp
                    ? cp.getIdentifier().getNamespace().equals(ReignOfNether.MOD_ID)
                    : !PLAYBACK_ALLOWED.contains(packet.getClass());
        ReplayRecorder r = recorder;
        if (r == null)
            return false;
        if (capturing && Thread.currentThread() == captureThread) {
            r.recordSnapshot(packet, 1L << ReplayRecorder.SNAPSHOT_BIT);
            return true;
        }
        // block changes come from onBlockChanged for everywhere at once, not each player's fogged copy
        if (packet instanceof ClientboundBlockUpdatePacket || packet instanceof ClientboundSectionBlocksUpdatePacket)
            return false;
        // a fogged player gets the pre-game snapshot of chunks they can't see: not what's really there
        if (packet instanceof ClientboundLevelChunkWithLightPacket c && FogOfWarServerEvents.isFogActiveFor(player)
                && !FogOfWarServerEvents.isChunkSentFor(player, new ChunkPos(c.getX(), c.getZ())))
            return false;
        r.record(player.getName().getString(), packet);
        return false;
    }

    /**
     * Something a viewer with no fog would get, though maybe no player did: every entity's spawn, movement and
     * removal, and stats that are only sent to players who can see the unit. So the replay has the whole match,
     * and its fog is only a view.
     */
    public static void recordOmni(Packet<?> packet) {
        ReplayRecorder r = recorder;
        if (r != null && !capturing)
            r.record(ReplayRecorder.OMNI, packet);
    }

    public static void recordOmniMessage(Object modMessage) {
        if (recorder != null && !capturing)
            recordOmni(PacketHandler.INSTANCE.toVanillaPacket(modMessage, NetworkDirection.PLAY_TO_CLIENT));
    }

    public static boolean isRecording() {
        return recorder != null && !capturing;
    }

    /** ServerLevel.sendBlockUpdated: any block change a client would hear about. */
    public static void onBlockChanged(BlockPos pos, BlockState state) {
        if (recorder != null && !capturing)
            recordOmni(new ClientboundBlockUpdatePacket(pos, state));
    }

    public static void onCamera(ServerPlayer player, float x, float z, float zoom, float rot) {
        ReplayRecorder r = recorder;
        if (r != null)
            r.recordCamera(player.getName().getString(), x, z, zoom, rot);
    }

    /** PlayerServerEvents.broadcastMatchStats: someone won. */
    public static void onMatchEnd(Set<String> winners) {
        if (recorder != null && PlayerServerEvents.serverLevel != null)
            finish(PlayerServerEvents.serverLevel.getServer(), winners);
        matchOver = true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || playbackWorld)
            return;
        MinecraftServer server = evt.getServer();
        boolean active = PlayerServerEvents.isGameActive() && !TutorialServerEvents.isEnabled();
        if (!active)
            matchOver = false;
        // a saved match being loaded waits for someone to join, so there's someone to snapshot it for
        if (recorder == null && active && !matchOver && !server.getPlayerList().getPlayers().isEmpty())
            start(server);
        if (recorder != null) {
            markDefeats(recorder);
            recorder.endTick();
            if (!active)
                finish(server, Set.of()); // reset without a winner
        }
        tickTransfers(server);
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent evt) {
        MinecraftServer server = evt.getServer();
        playbackWorld = !server.isDedicatedServer() && PLAYBACK_WORLD.equals(server.getWorldData().getLevelName())
                && server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals(PLAYBACK_WORLD);
    }

    // stopping, not stopped: the match's players are still there to go in the header
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        if (recorder != null)
            finish(evt.getServer(), Set.of());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent evt) {
        recorder = null;
        matchOver = false;
        capturing = false;
        lastRtsPlayers.clear();
        lastReplay = null;
        writing = false;
        lastRecipients = Set.of();
        transfers.clear();
        downloadCounts.clear();
        if (playbackWorld) {
            playbackWorld = false;
            FileUtils.deleteQuietly(evt.getServer().getWorldPath(LevelResource.ROOT).normalize().toFile());
        }
    }

    // ------------------------------------------------------------------ recording

    private static void start(MinecraftServer server) {
        ServerLevel level = server.overworld();
        String map = RTSMapInfoServerEvents.rtsMapInfo != null ? RTSMapInfoServerEvents.rtsMapInfo.getName()
                : server.getWorldData().getLevelName();
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        // kept out of the replays folder: it's only saved there if a player clicks download on the end screen
        Path pending = FMLPaths.GAMEDIR.get().resolve("reignofnether").resolve("replays-pending");
        FileUtils.deleteQuietly(pending.toFile()); // the last match's, nobody can download it anymore
        Path file = pending.resolve(stamp + "_" + map.replaceAll("[^A-Za-z0-9._-]", "_") + ReplayHeader.EXTENSION);
        try {
            recorder = new ReplayRecorder(file, p -> {
                if (p != null)
                    lastReplay = p;
                writing = false;
            });
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[Replay] can't record this match to {}", file, e);
            matchOver = true; // don't retry every tick
            return;
        }
        ReignOfNether.LOGGER.info("[Replay] recording to {}", file);
        ReplayHeader h = recorder.header;
        h.map = map;
        h.minBuildHeight = level.getMinBuildHeight();
        h.buildHeight = level.getHeight();
        lastRtsPlayers.clear();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                lastRtsPlayers.add(p.name);
        }
        snapshot(server, level, recorder);
    }

    // what a spectator joining right now would be sent, so playback starts from the match as it is
    private static void snapshot(MinecraftServer server, ServerLevel level, ReplayRecorder r) {
        long all = 1L << ReplayRecorder.SNAPSHOT_BIT;
        captureThread = Thread.currentThread();
        capturing = true;
        try {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            r.recordSnapshot(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(players), all);
            r.recordSnapshot(new ClientboundInitializeBorderPacket(level.getWorldBorder()), all);
            r.recordSnapshot(new ClientboundSetTimePacket(level.getGameTime(), level.getDayTime(),
                    level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)), all);
            if (level.isRaining()) {
                r.recordSnapshot(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0), all);
                r.recordSnapshot(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1)), all);
                r.recordSnapshot(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1)), all);
            }
            snapshotChunks(level, r, players);
            snapshotEntities(level, r);

            // the mod's own join syncs; broadcasts and sends to this one player are both captured
            if (!players.isEmpty()) {
                ServerPlayer sp = players.get(0);
                PlayerEvent.PlayerLoggedInEvent evt = new PlayerEvent.PlayerLoggedInEvent(sp);
                PlayerRoles.syncTo(sp);
                GameruleServerEvents.onPlayerJoin(evt);
                GameModeServerEvents.onPlayerJoin(evt);
                AlliancesServerEvents.onPlayerJoin(evt);
                RTSMapInfoServerEvents.onPlayerJoin(evt);
                FogOfWarServerEvents.onPlayerJoin(evt);
                ResourcesServerEvents.onPlayerJoin(evt);
                StartPosServerEvents.onPlayerJoin(evt);
            }
            ScenarioServerEvents.syncScenarioRoles();
            PlayerServerEvents.syncUnits();
            BuildingServerEvents.placeBuildingsClientside();
            synchronized (PlayerServerEvents.rtsPlayers) {
                for (RTSPlayer p : PlayerServerEvents.rtsPlayers) {
                    PlayerClientboundPacket.addRTSPlayer(p.name, p.faction, (long) p.id, p.startPosColorId, p.isDogPerson, true);
                    ResearchServerEvents.syncResearch(p.name);
                }
            }
            PlayerClientboundPacket.syncRtsGameTime(PlayerServerEvents.rtsGameTicks);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("[Replay] start snapshot is incomplete", e);
        } finally {
            capturing = false;
        }
        // after the capture, so it's recorded as each player's own
        for (ServerPlayer sp : server.getPlayerList().getPlayers())
            FogOfWarServerEvents.resendFogState(sp);
    }

    private static void snapshotChunks(ServerLevel level, ReplayRecorder r, List<ServerPlayer> players) {
        WorldBorder border = level.getWorldBorder();
        boolean rtsMap = WorldBorderServerEvents.isRtsOptimisedMap(level);
        int minX, maxX, minZ, maxZ;
        if (rtsMap) {
            minX = (int) Math.floor(border.getMinX()) >> 4;
            maxX = (int) Math.floor(border.getMaxX() - 1) >> 4;
            minZ = (int) Math.floor(border.getMinZ()) >> 4;
            maxZ = (int) Math.floor(border.getMaxZ() - 1) >> 4;
        } else { // a vanilla-sized world: whatever is loaded around the players
            int range = 16;
            minX = minZ = Integer.MAX_VALUE;
            maxX = maxZ = Integer.MIN_VALUE;
            for (ServerPlayer sp : players) {
                minX = Math.min(minX, sp.chunkPosition().x - range);
                maxX = Math.max(maxX, sp.chunkPosition().x + range);
                minZ = Math.min(minZ, sp.chunkPosition().z - range);
                maxZ = Math.max(maxZ, sp.chunkPosition().z + range);
            }
        }
        int count = 0;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                LevelChunk chunk = rtsMap ? level.getChunkSource().getChunk(cx, cz, true) : level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null)
                    continue;
                r.recordSnapshot(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null),
                        1L << ReplayRecorder.SNAPSHOT_BIT);
                if (++count % 64 == 0)
                    r.flush(); // don't hold the whole map in memory
            }
        }
        ReplayHeader h = r.header;
        h.minChunkX = minX;
        h.maxChunkX = maxX;
        h.minChunkZ = minZ;
        h.maxChunkZ = maxZ;
        ReignOfNether.LOGGER.info("[Replay] snapshot of {} chunks", count);
    }

    private static Field entityMapField, serverEntityField, seenByField;

    // every tracked entity as its trackers see it, so playback knows who still sees what when one stops
    @SuppressWarnings("unchecked")
    private static void snapshotEntities(ServerLevel level, ReplayRecorder r) throws ReflectiveOperationException {
        if (entityMapField == null) {
            Class<?> tracked = Class.forName("net.minecraft.server.level.ChunkMap$TrackedEntity");
            entityMapField = ObfuscationReflectionHelper.findField(ChunkMap.class, "f_140150_");
            serverEntityField = ObfuscationReflectionHelper.findField(tracked, "f_140471_");
            seenByField = ObfuscationReflectionHelper.findField(tracked, "f_140475_");
        }
        Int2ObjectMap<?> map = (Int2ObjectMap<?>) entityMapField.get(level.getChunkSource().chunkMap);
        for (Object te : map.values()) {
            ServerEntity serverEntity = (ServerEntity) serverEntityField.get(te);
            long mask = 0;
            for (ServerPlayerConnection conn : (Set<ServerPlayerConnection>) seenByField.get(te))
                mask |= r.maskOf(conn.getPlayer().getName().getString());
            long m = mask | r.maskOf(ReplayRecorder.OMNI);
            serverEntity.sendPairingData(null, p -> r.recordSnapshot(p, m));
        }
    }

    private static void markDefeats(ReplayRecorder r) {
        Set<String> now = new HashSet<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                now.add(p.name);
        }
        for (String name : lastRtsPlayers)
            if (!now.contains(name))
                r.header.markers.add(new ReplayHeader.Marker(r.tick(), "defeat", name));
        lastRtsPlayers.clear();
        lastRtsPlayers.addAll(now);
    }

    private static void finish(MinecraftServer server, Set<String> winners) {
        ReplayRecorder r = recorder;
        recorder = null;
        if (r == null)
            return;
        markDefeats(r);
        ReplayHeader h = r.header;
        h.markers.add(new ReplayHeader.Marker(r.tick(), "end", String.join(", ", winners)));
        // everyone who played, like the end screen (broadcastMatchStats)
        LinkedHashMap<String, RTSPlayer> byName = new LinkedHashMap<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                if (p != null) byName.putIfAbsent(p.name, p);
        }
        synchronized (PlayerServerEvents.postGameRtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.postGameRtsPlayers)
                if (p != null) byName.putIfAbsent(p.name, p);
        }
        for (RTSPlayer p : byName.values()) {
            ReplayHeader.PlayerRow row = new ReplayHeader.PlayerRow();
            row.name = p.name;
            row.faction = p.faction != null ? p.faction.key.toString() : "";
            row.team = p.startPosColorId;
            row.winner = winners.contains(p.name);
            row.bot = p.isBot();
            row.scores = p.scores.getScoreListAsArray();
            h.players.add(row);
        }
        h.packetIds = ReplayHeader.currentPacketIds();
        h.maxEntityId = entityCounter();
        lastRecipients = Set.copyOf(h.recipients);
        lastReplay = null; // a failed save must not hand out the previous match's file
        writing = true;
        r.finish();
        downloadCounts.clear(); // a new replay to ask for
    }

    private static int entityCounter() {
        try {
            AtomicInteger counter = ObfuscationReflectionHelper.getPrivateValue(Entity.class, null, "f_19843_");
            return counter != null ? counter.get() : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------ downloads

    /** A player asked for the finished match's replay from the end screen. */
    public static void requestDownload(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (transfers.containsKey(uuid))
            return;
        if (downloadCounts.merge(uuid, 1, Integer::sum) > MAX_DOWNLOADS_PER_REPLAY
                || (lastReplay == null && !writing) || !lastRecipients.contains(player.getName().getString())) {
            ReplayChunkClientboundPacket.fail(player);
            return;
        }
        transfers.put(uuid, new Transfer());
    }

    private static void tickTransfers(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Transfer>> it = transfers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Transfer> e = it.next();
            ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
            Transfer t = e.getValue();
            if (sp == null) {
                it.remove();
                continue;
            }
            try {
                if (t.file == null) {
                    if (writing)
                        continue; // still being saved
                    t.file = lastReplay;
                    t.size = t.file != null ? Files.size(t.file) : -1;
                    if (t.file == null || t.size > MAX_DOWNLOAD_BYTES) {
                        ReplayChunkClientboundPacket.fail(sp);
                        it.remove();
                        continue;
                    }
                }
                try (SeekableByteChannel ch = Files.newByteChannel(t.file)) {
                    ch.position(t.offset);
                    for (int i = 0; i < CHUNKS_PER_TICK && t.offset < t.size; i++) {
                        ByteBuffer buf = ByteBuffer.allocate((int) Math.min(ReplayChunkClientboundPacket.CHUNK, t.size - t.offset));
                        while (buf.hasRemaining() && ch.read(buf) > 0) { }
                        ReplayChunkClientboundPacket.send(sp, t.file.getFileName().toString(), t.size, t.offset, buf.array());
                        t.offset += buf.capacity();
                    }
                }
                if (t.offset >= t.size)
                    it.remove();
            } catch (IOException ex) {
                ReignOfNether.LOGGER.error("[Replay] download of {} failed", t.file, ex);
                ReplayChunkClientboundPacket.fail(sp);
                it.remove();
            }
        }
    }
}
