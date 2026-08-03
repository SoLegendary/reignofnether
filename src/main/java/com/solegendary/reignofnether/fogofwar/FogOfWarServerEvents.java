package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;

public class FogOfWarServerEvents {

    private static boolean enabled = false; // enforced for all clients
    private static ServerLevel serverLevel = null;

    // Per-player vision, recomputed every UPDATE_TICKS ticks. Three tiers:
    //  - liveChunks: chunks FULLY inside the block-radius circle -> receive all live updates unfiltered.
    //  - edgeChunks (= live + partially-covered "edge"): chunks the client has data for and renders. Edge
    //    chunks get per-column gated updates (shouldSendChunkPacket): visible columns update live, fogged
    //    columns stay frozen so out-of-circle changes don't leak.
    //  - edgeMasks: 16x16 covered-column bitmask (long[4]) per edge chunk. Authoritative for entity gating,
    //    update gating AND the client's fog tint (synced via FogChunksClientboundPacket).
    private static final Map<UUID, Set<ChunkPos>> playerLiveChunks = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<ChunkPos>> playerEdgeChunks = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<ChunkPos, long[]>> playerEdgeMasks = new ConcurrentHashMap<>();

    // (player, chunk) pairs needing a full-chunk resend at the next fog tick: a multi-block section update
    // straddled the vision circle in an edge chunk, so the packet was dropped for that player but part of
    // the change IS visible to them. Drained by updatePlayerBrightChunks.
    private static final Map<UUID, Set<ChunkPos>> pendingResends = new ConcurrentHashMap<>();

    // players whose units/buildings ignore fog and are visible to everyone
    private static final Set<String> revealedPlayerNames = ConcurrentHashMap.newKeySet();

    // <entityId <name of attacked player, ticks to expiry>>.
    // A unit that attacks out of the fog is revealed to the player it attacked
    // capped at REVEALED_ATTACKER_SIGHT_BLOCKS
    private static final Map<Integer, Map<String, Long>> revealedUnitExpiryTick = new ConcurrentHashMap<>();

    // snapshot of neutral unit starting positions
    public static final Map<Integer, AABB> neutralFogUnits = new HashMap<>();

    public static void captureNeutralFogUnits() {
        neutralFogUnits.clear();
        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (le instanceof Unit unit && unit.getOwnerName().isEmpty() && Unit.hasAnchor(unit)) {
                neutralFogUnits.put(le.getId(), le.getBoundingBox());
                FogNeutralUnitClientboundPacket.sendNeutralFogUnitToAll(le.getId(), le.getBoundingBox());
            }
        }
    }

    public static void setPlayerRevealed(String playerName, boolean reveal) {
        if (reveal) revealedPlayerNames.add(playerName);
        else revealedPlayerNames.remove(playerName);
    }

    public static void revealRangedUnit(int unitId, String targetOwnerName, int durationTicks) {
        if (serverLevel == null || targetOwnerName == null) return;
        long expireAt = serverLevel.getGameTime() + durationTicks;
        revealedUnitExpiryTick
                .computeIfAbsent(unitId, k -> new ConcurrentHashMap<>())
                .merge(targetOwnerName, expireAt, Math::max);
    }

    public static final int PLAYER_SIGHT_BLOCKS = 16;
    public static final int REVEALED_ATTACKER_SIGHT_BLOCKS = 8;
    public static final int MAX_SIGHT_BLOCKS = 64;

    private static final int UPDATE_TICKS = 10;
    private static int ticksUntilUpdate = UPDATE_TICKS;

    private static final Map<UUID, Boolean> isRtsPlayerCache = new ConcurrentHashMap<>();

    public static void invalidateRtsCache() {
        isRtsPlayerCache.clear();
    }

    // match by name; entity ids change on reconnect
    public static boolean isFogActiveFor(ServerPlayer player) {
        if (!enabled) return false;
        Boolean cached = isRtsPlayerCache.get(player.getUUID());
        if (cached != null) return cached;
        boolean v = PlayerServerEvents.isRTSPlayer(player.getName().getString());
        isRtsPlayerCache.put(player.getUUID(), v);
        return v;
    }

    // A chunk is "live" (receives all updates unfiltered) only when fully inside the vision circle.
    public static boolean isChunkLiveFor(ServerPlayer player, ChunkPos pos) {
        if (!isFogActiveFor(player)) return true;
        Set<ChunkPos> set = playerLiveChunks.get(player.getUUID());
        return set != null && set.contains(pos);
    }

    // A chunk is "sent" (client has its data / renders it) when live or edge. The initial chunk send
    // (ChunkMapInitialSendMixin) serves live data for these and the snapshot for everything else.
    public static boolean isChunkSentFor(ServerPlayer player, ChunkPos pos) {
        if (!isFogActiveFor(player)) return true;
        Set<ChunkPos> set = playerEdgeChunks.get(player.getUUID());
        return set != null && set.contains(pos);
    }

    public static boolean isBlockVisibleFor(String playerName, int x, int z) {
        if (serverLevel == null)
            return false;
        for (ServerPlayer sp : serverLevel.players())
            if (sp.getName().getString().equals(playerName))
                return isBlockVisibleFor(sp, x, z);
        return false;
    }

    // Block-level visibility for entity gating: live chunk -> visible; edge chunk -> covered-column bit; else
    // hidden. Lets enemy units hide exactly at the circle instead of one whole chunk out.
    public static boolean isBlockVisibleFor(ServerPlayer player, int x, int z) {
        if (!isFogActiveFor(player)) return true;
        UUID uuid = player.getUUID();
        ChunkPos cp = new ChunkPos(x >> 4, z >> 4);
        Set<ChunkPos> live = playerLiveChunks.get(uuid);
        if (live != null && live.contains(cp)) return true;
        Map<ChunkPos, long[]> masks = playerEdgeMasks.get(uuid);
        if (masks == null) return false;
        long[] mask = masks.get(cp);
        if (mask == null) return false;
        return isColumnSet(mask, x, z);
    }

    private static boolean isColumnSet(long[] mask, int x, int z) {
        int i = ((x & 15) << 4) | (z & 15);
        return (mask[i >> 6] & (1L << (i & 63))) != 0;
    }

    // Per-packet gate for chunk broadcasts (ChunkHolderMixin). Live chunk -> everything passes; edge chunk
    // -> per-column filtering so the visible part updates live while the fogged part stays frozen:
    //  - single block / block entity update: pass iff its column is inside the circle
    //  - multi-block section update: pass iff ALL its columns are visible; if only SOME are, drop it but
    //    queue a full-chunk resend at the next fog tick so the visible columns still catch up (<=0.5s)
    //  - anything else (light updates etc.): frozen
    public static boolean shouldSendChunkPacket(ServerPlayer sp, ChunkPos pos, Packet<?> packet) {
        if (!isFogActiveFor(sp)) return true;
        UUID uuid = sp.getUUID();
        Set<ChunkPos> live = playerLiveChunks.get(uuid);
        if (live != null && live.contains(pos)) return true;
        Set<ChunkPos> sent = playerEdgeChunks.get(uuid);
        if (sent == null || !sent.contains(pos)) return false; // dark: fully frozen

        if (packet instanceof ClientboundBlockUpdatePacket p) {
            BlockPos bp = p.getPos();
            return isBlockVisibleFor(sp, bp.getX(), bp.getZ());
        }
        if (packet instanceof ClientboundBlockEntityDataPacket p) {
            BlockPos bp = p.getPos();
            return isBlockVisibleFor(sp, bp.getX(), bp.getZ());
        }
        if (packet instanceof ClientboundSectionBlocksUpdatePacket p) {
            // Hoist the mask lookup out of the per-block callback: with small sight ranges almost every
            // visible chunk is an edge chunk, so this path runs for nearly all block updates.
            Map<ChunkPos, long[]> masks = playerEdgeMasks.get(uuid);
            long[] mask = masks == null ? null : masks.get(pos);
            if (mask == null) return false;
            int[] visibleTotal = new int[2];
            p.runUpdates((bp, state) -> {
                visibleTotal[1]++;
                if (isColumnSet(mask, bp.getX(), bp.getZ())) visibleTotal[0]++;
            });
            if (visibleTotal[0] == visibleTotal[1]) return true;
            if (visibleTotal[0] > 0)
                queueResend(uuid, pos);
            return false;
        }
        return false;
    }

    // schedule a full-chunk resend to this player at the next fog tick (client-side merge keeps its fogged
    // columns frozen, so this only refreshes the visible part)
    public static void queueResend(UUID playerId, ChunkPos pos) {
        pendingResends.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(pos);
    }

    public static Set<ChunkPos> getBuildingChunks(BuildingPlacement b) {
        int minCx = b.minCorner.getX() >> 4;
        int maxCx = b.maxCorner.getX() >> 4;
        int minCz = b.minCorner.getZ() >> 4;
        int maxCz = b.maxCorner.getZ() >> 4;
        Set<ChunkPos> chunks = new HashSet<>();
        for (int cx = minCx; cx <= maxCx; cx++)
            for (int cz = minCz; cz <= maxCz; cz++)
                chunks.add(new ChunkPos(cx, cz));
        return chunks;
    }

    public static boolean canPlayerSeeBuilding(ServerPlayer sp, BuildingPlacement b) {
        if (!isFogActiveFor(sp)) return true;
        return isBlockVisibleFor(sp, b.maxCorner.getX(), b.maxCorner.getZ()) ||
                isBlockVisibleFor(sp, b.minCorner.getX(), b.minCorner.getZ()) ||
                isBlockVisibleFor(sp, b.maxCorner.getX(), b.minCorner.getZ()) ||
                isBlockVisibleFor(sp, b.minCorner.getX(), b.maxCorner.getZ());
    }

    private static void clearPlayerVision(UUID playerId) {
        playerLiveChunks.remove(playerId);
        playerEdgeChunks.remove(playerId);
        playerEdgeMasks.remove(playerId);
        pendingResends.remove(playerId);
    }

    public static void onPlayerDisconnect(UUID playerId) {
        clearPlayerVision(playerId);
        isRtsPlayerCache.remove(playerId);
    }

    // drop bookkeeping so re-entering orthoview later resends a full vision set
    public static void onPlayerExitOrthoview(ServerPlayer player) {
        clearPlayerVision(player.getUUID());
    }

    public static boolean isForceFog() {
        return serverLevel != null && serverLevel.getGameRules().getBoolean(GameRuleRegistrar.FORCE_FOG);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        syncClientFog();
        if (evt.getEntity() instanceof ServerPlayer sp)
            for (int id : neutralFogUnits.keySet())
                FogNeutralUnitClientboundPacket.sendNeutralFogUnit(sp, id, neutralFogUnits.get(id));
    }

    // On an RTS map, adopt any snapshot left on disk by a prior session so a mid-match server restart keeps
    // serving it instead of recapturing (or briefly leaking the live world before the next capture).
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null && WorldBorderServerEvents.isRtsOptimisedMap(level))
            FogChunkSnapshot.rebuildIndex(level);
    }

    public static void setEnabled(boolean value) {
        if (!value && isForceFog()) {
            ReignOfNether.LOGGER.info("[FogOfWar] Disable refused: gamerule reignofnetherForceFog is true");
            return;
        }
        // Fog only works on an RTS-optimised map: the small world border bounds the play area, so the
        // pre-match snapshot (FogChunkSnapshot) can cover it in full and dark chunks can never fall back
        // to the live world on reload/relog. On a vanilla-sized border the snapshot can't be captured, so
        // refuse rather than silently leaking. A null serverLevel can't be verified, so it's refused too.
        if (value && !WorldBorderServerEvents.isRtsOptimisedMap(serverLevel)) {
            ReignOfNether.LOGGER.info("[FogOfWar] Enable refused: not an RTS-optimised map (world border > {})",
                    WorldBorderServerEvents.RTS_OPTIMIZED_BORDER);
            sendMessageToAllPlayers("server.reignofnether.fog_requires_rts_map", true);
            return;
        }
        boolean wasEnabled = enabled;
        enabled = value;
        if (enabled)
            sendMessageToAllPlayers("server.reignofnether.enabled_fog_of_war", true);
        else
            sendMessageToAllPlayers("server.reignofnether.disabled_fog_of_war", true);

        // force a full resync on the next tick (clients clear their own set on disable)
        playerLiveChunks.clear();
        playerEdgeChunks.clear();
        playerEdgeMasks.clear();
        pendingResends.clear();

        syncClientFog();

        // vanilla only diffs tracking on movement; force a resync for stale chunks
        if (wasEnabled && !enabled && serverLevel != null) {
            resendAllTrackedChunks();
        }
    }

    private static void resendAllTrackedChunks() {
        int viewDist = serverLevel.getServer().getPlayerList().getViewDistance();
        for (ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
            ChunkPos center = sp.chunkPosition();
            ServerLevel level = sp.serverLevel();
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    LevelChunk chunk = level.getChunkSource().getChunk(center.x + dx, center.z + dz, false);
                    if (chunk == null) continue;
                    sp.connection.send(new ClientboundLevelChunkWithLightPacket(
                            chunk, level.getLightEngine(), null, null));
                }
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        serverLevel = (ServerLevel) evt.level;

        // Only attempt the forced auto-enable on an RTS-optimised map; otherwise setEnabled would refuse
        // and spam the log/chat every tick. On a vanilla-sized map force-fog simply stays inert.
        if (!enabled && isForceFog() && WorldBorderServerEvents.isRtsOptimisedMap(serverLevel))
            setEnabled(true);

        if (!enabled) return;

        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }

        ticksUntilUpdate = UPDATE_TICKS;
        updatePlayerBrightChunks();
        for (ServerPlayer p : PlayerServerEvents.orthoviewPlayers) {
            // TODO: on server restart (or singleplayer map relog) unit ids change so fog units always get removed as soon as they're explored
            PlayerChunksClientboundPacket.send(p, playerLiveChunks, playerEdgeChunks);
            for (int id : neutralFogUnits.keySet()) {
                Vec3 center = neutralFogUnits.get(id).getCenter();
                if (isBlockVisibleFor(p, (int) center.x, (int) center.z) && serverLevel.getEntity(id) == null) {
                    FogNeutralUnitClientboundPacket.removeNeutralFogUnit(p, id);
                }
            }
        }
    }

    private static void updatePlayerBrightChunks() {
        if (serverLevel == null) return;

        long now = serverLevel.getGameTime();
        revealedUnitExpiryTick.values().forEach(m -> m.values().removeIf(t -> t < now));
        revealedUnitExpiryTick.entrySet().removeIf(e -> e.getValue().isEmpty());

        // Precompute orthoview UUIDs (O(1) lookup instead of stream.anyMatch per player)
        Set<UUID> orthoviewUuids = new HashSet<>();
        for (ServerPlayer p : PlayerServerEvents.orthoviewPlayers)
            orthoviewUuids.add(p.getUUID());

        // Collect viewers per owner as blockPos -> LARGEST sight range on that column. Deduping by column
        // (rather than rasterising per entity) is what keeps a clumped army cheap; taking the max preserves
        // the coverage of the longest-sighted occupant.
        Map<String, Long2IntOpenHashMap> viewersByOwner = new HashMap<>();
        // Temporarily revealed units, bucketed by the owner name of the player they attacked. Units whose
        // owner is wholly revealed are skipped (that owner's mask is reused from coveredByOwner instead,
        // so it's never rasterised twice).
        Map<String, Long2IntOpenHashMap> revealedViewersByTarget = new HashMap<>();

        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (!(le instanceof Unit unit)) continue;
            String unitOwner = unit.getOwnerName();
            int bx = le.blockPosition().getX(), bz = le.blockPosition().getZ();
            int sight = unit.getSightRange();
            addViewer(viewersByOwner.computeIfAbsent(unitOwner, k -> new Long2IntOpenHashMap()), bx, bz, sight);

            if (!revealedPlayerNames.contains(unitOwner)) {
                Map<String, Long> targets = revealedUnitExpiryTick.get(le.getId());
                if (targets != null) {
                    int revealSight = Math.min(sight, REVEALED_ATTACKER_SIGHT_BLOCKS);
                    for (String target : targets.keySet())
                        addViewer(revealedViewersByTarget.computeIfAbsent(target, k -> new Long2IntOpenHashMap()),
                                bx, bz, revealSight);
                }
            }
        }

        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            addViewer(viewersByOwner.computeIfAbsent(b.ownerName, k -> new Long2IntOpenHashMap()),
                    b.centrePos.getX(), b.centrePos.getZ(), b.getSightRange());
        }

        // Rasterise each owner's viewer circles ONCE into covered-column masks (chunkLong -> 16x16 bitmask),
        // reused across that owner, all its allies, and (if revealed) every other player.
        Map<String, Long2ObjectOpenHashMap<long[]>> coveredByOwner = new HashMap<>();
        for (Map.Entry<String, Long2IntOpenHashMap> e : viewersByOwner.entrySet()) {
            Long2ObjectOpenHashMap<long[]> masks = new Long2ObjectOpenHashMap<>();
            rasterizeViewers(masks, e.getValue());
            coveredByOwner.put(e.getKey(), masks);
        }

        // Revealed owners are treated exactly like allies of everyone: their full block-granular vision is
        // merged into every player's covered mask.
        List<String> revealedOwners = new ArrayList<>();
        for (String owner : viewersByOwner.keySet())
            if (revealedPlayerNames.contains(owner)) revealedOwners.add(owner);

        // One mask set per attacked player: merged only into that player and their allies.
        Map<String, Long2ObjectOpenHashMap<long[]>> revealedMasksByTarget = new HashMap<>();
        for (Map.Entry<String, Long2IntOpenHashMap> e : revealedViewersByTarget.entrySet()) {
            Long2ObjectOpenHashMap<long[]> masks = new Long2ObjectOpenHashMap<>();
            rasterizeViewers(masks, e.getValue());
            revealedMasksByTarget.put(e.getKey(), masks);
        }

        for (ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
            UUID uuid = sp.getUUID();
            if (!isFogActiveFor(sp)) {
                clearPlayerVision(uuid);
                continue;
            }
            String ownerName = sp.getName().getString();

            // Union this player's own + allied + revealed owners' covered masks (+ FPS self-bubble).
            Long2ObjectOpenHashMap<long[]> covered = new Long2ObjectOpenHashMap<>();
            if (!orthoviewUuids.contains(uuid) && sp.gameMode.isSurvival())
                rasterizeCircle(covered, sp.blockPosition().getX(), sp.blockPosition().getZ(), PLAYER_SIGHT_BLOCKS);
            mergeMasks(covered, coveredByOwner.get(ownerName));
            Set<String> alliedOwners = collectAlliedOwners(ownerName, viewersByOwner.keySet());
            for (String ally : alliedOwners)
                mergeMasks(covered, coveredByOwner.get(ally));
            for (String revealed : revealedOwners)
                mergeMasks(covered, coveredByOwner.get(revealed)); // no-op if already merged as self/ally
            // enemy units that attacked this player (or an ally) out of the fog
            mergeMasks(covered, revealedMasksByTarget.get(ownerName));
            for (String ally : alliedOwners)
                mergeMasks(covered, revealedMasksByTarget.get(ally));

            // Classify: fully-covered chunk -> live; partially-covered -> edge (keep its column mask).
            Set<ChunkPos> live = new HashSet<>();
            Map<ChunkPos, long[]> edge = new HashMap<>();
            for (Long2ObjectMap.Entry<long[]> e : covered.long2ObjectEntrySet()) {
                ChunkPos cp = new ChunkPos(e.getLongKey());
                if (maskFull(e.getValue())) live.add(cp);
                else edge.put(cp, e.getValue());
            }
            Set<ChunkPos> sent = new HashSet<>(live);
            sent.addAll(edge.keySet());

            Set<ChunkPos> prevLive = playerLiveChunks.get(uuid);
            Set<ChunkPos> prevSent = playerEdgeChunks.get(uuid);
            Map<ChunkPos, long[]> prevEdge = playerEdgeMasks.get(uuid);

            // Full-chunk resends needed this tick:
            //  - newly sent chunk
            //  - edge -> live promotion (fogged columns were gated while edge, so the client copy is stale)
            //  - still-edge chunk whose mask GREW (the newly covered columns were fogged, hence stale)
            //  - queued by shouldSendChunkPacket (mixed multi-block update straddling the circle)
            Set<ChunkPos> toResend = new HashSet<>();
            for (ChunkPos cp : sent) {
                if (prevSent == null || !prevSent.contains(cp)) { toResend.add(cp); continue; }
                boolean nowLive = live.contains(cp);
                boolean wasLive = prevLive != null && prevLive.contains(cp);
                if (nowLive && !wasLive) { toResend.add(cp); continue; }
                if (!nowLive && prevEdge != null) {
                    long[] pm = prevEdge.get(cp);
                    long[] nm = edge.get(cp);
                    if (pm != null && nm != null && maskGrew(pm, nm)) toResend.add(cp);
                }
            }
            Set<ChunkPos> pending = pendingResends.remove(uuid);
            if (pending != null)
                for (ChunkPos cp : pending)
                    if (sent.contains(cp)) toResend.add(cp);

            // Resolve resend chunks first: an unloaded chunk is dropped from this tick's vision (never tell
            // the client a chunk is sent when it got no data for it) and retries on a later recompute.
            ServerLevel level = sp.serverLevel();
            List<LevelChunk> resendChunks = new ArrayList<>(toResend.size());
            for (ChunkPos cp : toResend) {
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
                if (chunk == null) {
                    sent.remove(cp);
                    live.remove(cp);
                    edge.remove(cp);
                    continue;
                }
                resendChunks.add(chunk);
            }

            playerLiveChunks.put(uuid, live);
            playerEdgeChunks.put(uuid, sent);
            playerEdgeMasks.put(uuid, edge);

            // The mask packet must go out BEFORE the chunk resends: the client merges each incoming chunk
            // against its CURRENT mask (restoring fogged columns, see ClientChunkCacheMixin), so it needs
            // the new mask first or newly-visible columns would stay frozen at their stale state.
            if (prevSent == null || !prevSent.equals(sent) || !masksEqual(prevEdge, edge))
                FogChunksClientboundPacket.send(sp, sent, edge);

            // noLight == client retains its cached lighting
            // this does mean there might be lighting bugs for corners of edge chunks out of view
            BitSet noLight = new BitSet();
            for (LevelChunk chunk : resendChunks) {
                boolean updateLight = live.contains(chunk.getPos()) || sent.contains(chunk.getPos());
                sp.connection.send(new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(),
                        updateLight ? null : noLight, updateLight ? null : noLight));
            }
        }
    }

    private static long blockLong(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    // Record a viewer at (x,z), keeping only the largest radius seen on that column. Clamped to
    // MAX_SIGHT_BLOCKS so an unbounded attribute modifier can't blow up rasterisation cost.
    private static void addViewer(Long2IntOpenHashMap viewers, int x, int z, int sight) {
        if (sight < 0) return;
        int r = Math.min(sight, MAX_SIGHT_BLOCKS);
        long key = blockLong(x, z);
        if (viewers.get(key) < r || !viewers.containsKey(key)) // default return value is 0
            viewers.put(key, r);
    }

    private static void rasterizeViewers(Long2ObjectOpenHashMap<long[]> masks, Long2IntOpenHashMap viewers) {
        if (viewers == null) return;
        for (Long2IntMap.Entry e : viewers.long2IntEntrySet()) {
            long p = e.getLongKey();
            rasterizeCircle(masks, (int) (p >> 32), (int) p, e.getIntValue());
        }
    }

    // Stamp a filled block-radius circle into per-chunk 16x16 column masks. Iterates chunk-by-chunk so each
    // chunk does at most one map lookup regardless of how many of its columns the circle covers. Two fast
    // paths matter now that radii vary: an already-saturated chunk is skipped outright, and a chunk whose
    // farthest corner is inside the circle is filled without testing its 256 columns.
    private static void rasterizeCircle(Long2ObjectOpenHashMap<long[]> masks, int cx, int cz, int r) {
        if (r < 0) return;
        int r2 = r * r;
        int minCX = (cx - r) >> 4, maxCX = (cx + r) >> 4;
        int minCZ = (cz - r) >> 4, maxCZ = (cz + r) >> 4;
        for (int chX = minCX; chX <= maxCX; chX++) {
            int baseX = chX << 4;
            int farX = Math.max(Math.abs(baseX - cx), Math.abs(baseX + 15 - cx));
            int farX2 = farX * farX;
            for (int chZ = minCZ; chZ <= maxCZ; chZ++) {
                int baseZ = chZ << 4;
                long cl = ChunkPos.asLong(chX, chZ);
                long[] mask = masks.get(cl);
                if (mask != null && maskFull(mask)) continue;

                int farZ = Math.max(Math.abs(baseZ - cz), Math.abs(baseZ + 15 - cz));
                if (farX2 + farZ * farZ <= r2) { // whole chunk inside the circle
                    if (mask == null) { mask = new long[4]; masks.put(cl, mask); }
                    Arrays.fill(mask, -1L);
                    continue;
                }

                for (int lx = 0; lx < 16; lx++) {
                    int dx = baseX + lx - cx;
                    int dx2 = dx * dx;
                    if (dx2 > r2) continue;
                    for (int lz = 0; lz < 16; lz++) {
                        int dz = baseZ + lz - cz;
                        if (dx2 + dz * dz > r2) continue;
                        if (mask == null) { mask = new long[4]; masks.put(cl, mask); }
                        int i = (lx << 4) | lz;
                        mask[i >> 6] |= (1L << (i & 63));
                    }
                }
            }
        }
    }

    // OR src masks into dest, cloning on first insert so the shared owner-cache mask is never mutated.
    private static void mergeMasks(Long2ObjectOpenHashMap<long[]> dest, Long2ObjectOpenHashMap<long[]> src) {
        if (src == null) return;
        for (Long2ObjectMap.Entry<long[]> e : src.long2ObjectEntrySet()) {
            long[] sm = e.getValue();
            long[] dm = dest.get(e.getLongKey());
            if (dm == null) dest.put(e.getLongKey(), sm.clone());
            else { dm[0] |= sm[0]; dm[1] |= sm[1]; dm[2] |= sm[2]; dm[3] |= sm[3]; }
        }
    }

    private static boolean maskFull(long[] mask) {
        return mask[0] == -1L && mask[1] == -1L && mask[2] == -1L && mask[3] == -1L;
    }

    // any column covered now that wasn't before (shrinking alone is not growth)
    private static boolean maskGrew(long[] prev, long[] now) {
        return (now[0] & ~prev[0]) != 0 || (now[1] & ~prev[1]) != 0
                || (now[2] & ~prev[2]) != 0 || (now[3] & ~prev[3]) != 0;
    }

    private static boolean masksEqual(Map<ChunkPos, long[]> a, Map<ChunkPos, long[]> b) {
        if (a == null) return b == null || b.isEmpty();
        if (a.size() != b.size()) return false;
        for (Map.Entry<ChunkPos, long[]> e : a.entrySet()) {
            long[] other = b.get(e.getKey());
            if (other == null || !Arrays.equals(e.getValue(), other)) return false;
        }
        return true;
    }

    private static Set<String> collectAlliedOwners(String me, Set<String> owners) {
        Set<String> out = new HashSet<>();
        for (String o : owners)
            if (!o.equals(me) && AlliancesServerEvents.isAllied(me, o)) out.add(o);
        return out;
    }

    // register here too for command blocks
    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("rts-fog").then(Commands.literal("enable")
                .executes((command) -> {
                    setEnabled(true);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("rts-fog").then(Commands.literal("disable")
                .executes((command) -> {
                    setEnabled(false);
                    return 1;
                })));
    }

    private static void syncClientFog() {
        FogOfWarClientboundPacket.setEnabled(enabled);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        onPlayerDisconnect(evt.getEntity().getUUID());
    }
}