package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.addon.GarrisonableBuildingAddon;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.piglins.GhastUnit;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;

public class FogOfWarServerEvents {

    private static boolean enabled = false; // enforced for all clients
    private static ServerLevel serverLevel = null;

    // per-player bright (visible) chunks, recomputed every UPDATE_TICKS ticks
    private static final Map<UUID, Set<ChunkPos>> playerBrightChunks = new ConcurrentHashMap<>();

    // players whose units/buildings ignore fog and are visible to everyone
    private static final Set<String> revealedPlayerNames = ConcurrentHashMap.newKeySet();

    // entityId -> server tick when temporary "attacked-from-fog" reveal expires
    private static final Map<Integer, Long> revealedUnitExpiryTick = new ConcurrentHashMap<>();

    public static void setPlayerRevealed(String playerName, boolean reveal) {
        if (reveal) revealedPlayerNames.add(playerName);
        else revealedPlayerNames.remove(playerName);
    }

    public static void revealRangedUnit(int unitId, int durationTicks) {
        if (serverLevel == null) return;
        long expireAt = serverLevel.getGameTime() + durationTicks;
        revealedUnitExpiryTick.merge(unitId, expireAt, Math::max);
    }

    public static final int CHUNK_VIEW_DIST = 1;
    public static final int CHUNK_FAR_VIEW_DIST = 2;
    private static final int UPDATE_TICKS = 10;
    private static int ticksUntilUpdate = UPDATE_TICKS;

    // Cache "is RTS player" per UUID — ChunkMapMixin.getPlayers calls this for every player on
    // every chunk broadcast, and PlayerServerEvents.isRTSPlayer iterates a synchronized list with
    // string compares. Invalidated by PlayerServerEvents.invalidateFogRtsCache() on every mutation.
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

    public static boolean isChunkBrightFor(ServerPlayer player, ChunkPos pos) {
        if (!isFogActiveFor(player)) return true;
        Set<ChunkPos> set = playerBrightChunks.get(player.getUUID());
        return set != null && set.contains(pos);
    }

    public static boolean isChunkBrightFor(UUID playerId, ChunkPos pos) {
        if (!enabled) return true;
        Set<ChunkPos> set = playerBrightChunks.get(playerId);
        return set != null && set.contains(pos);
    }

    public static Set<ChunkPos> getBrightChunks(UUID playerId) {
        return playerBrightChunks.getOrDefault(playerId, Set.of());
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

    public static boolean canPlayerSeeChunks(ServerPlayer sp, Set<ChunkPos> chunks) {
        if (!isFogActiveFor(sp)) return true;
        Set<ChunkPos> bright = playerBrightChunks.get(sp.getUUID());
        if (bright == null) return false;
        for (ChunkPos cp : chunks)
            if (bright.contains(cp)) return true;
        return false;
    }

    public static void onPlayerDisconnect(UUID playerId) {
        playerBrightChunks.remove(playerId);
        isRtsPlayerCache.remove(playerId);
    }

    // drop bookkeeping so re-entering orthoview later resends a full bright set
    public static void onPlayerExitOrthoview(ServerPlayer player) {
        playerBrightChunks.remove(player.getUUID());
    }

    public static boolean isForceFog() {
        return serverLevel != null && serverLevel.getGameRules().getBoolean(GameRuleRegistrar.FORCE_FOG);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        syncClientFog();
    }

    public static void setEnabled(boolean value) {
        if (!value && isForceFog()) {
            ReignOfNether.LOGGER.info("[FogOfWar] Disable refused: gamerule reignofnetherForceFog is true");
            return;
        }
        boolean wasEnabled = enabled;
        enabled = value;
        if (enabled)
            sendMessageToAllPlayers("server.reignofnether.enabled_fog_of_war", true);
        else
            sendMessageToAllPlayers("server.reignofnether.disabled_fog_of_war", true);

        // force a full resync on the next tick (clients clear their own set on disable)
        playerBrightChunks.clear();

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

        if (!enabled && isForceFog())
            setEnabled(true);

        if (!enabled) return;

        if (ticksUntilUpdate > 0) {
            ticksUntilUpdate--;
            return;
        }
        ticksUntilUpdate = UPDATE_TICKS;
        updatePlayerBrightChunks();
    }

    private static void updatePlayerBrightChunks() {
        if (serverLevel == null) return;

        long now = serverLevel.getGameTime();
        revealedUnitExpiryTick.entrySet().removeIf(e -> e.getValue() < now);

        // Precompute orthoview UUIDs (O(1) lookup instead of stream.anyMatch per player)
        Set<UUID> orthoviewUuids = new HashSet<>();
        for (ServerPlayer p : PlayerServerEvents.orthoviewPlayers)
            orthoviewUuids.add(p.getUUID());

        // Bucket each unit/building by owner once; previously re-iterated per player
        Map<String, LongOpenHashSet> nearByOwner = new HashMap<>();
        Map<String, LongOpenHashSet> farByOwner = new HashMap<>();
        LongOpenHashSet forceRevealed = new LongOpenHashSet();

        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (!(le instanceof Unit unit)) continue;
            String unitOwner = unit.getOwnerName();
            long packed = chunkLong(le.blockPosition().getX(), le.blockPosition().getZ());
            if (le instanceof GhastUnit) {
                farByOwner.computeIfAbsent(unitOwner, k -> new LongOpenHashSet()).add(packed);
            } else {
                nearByOwner.computeIfAbsent(unitOwner, k -> new LongOpenHashSet()).add(packed);
            }
            if (revealedPlayerNames.contains(unitOwner) || revealedUnitExpiryTick.containsKey(le.getId()))
                forceRevealed.add(packed);
        }

        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            long packed = chunkLong(b.centrePos.getX(), b.centrePos.getZ());
            boolean farSight = b.isCapitol ||
                    (b.getBuilding().hasActiveAddon(GarrisonableBuildingAddon.class) &&
                            GarrisonableBuildingAddon.getNumOccupants(b) > 0 && b.isBuilt);
            if (farSight) {
                farByOwner.computeIfAbsent(b.ownerName, k -> new LongOpenHashSet()).add(packed);
            } else {
                nearByOwner.computeIfAbsent(b.ownerName, k -> new LongOpenHashSet()).add(packed);
            }
            if (revealedPlayerNames.contains(b.ownerName))
                forceRevealed.add(packed);
        }

        for (ServerPlayer sp : serverLevel.getServer().getPlayerList().getPlayers()) {
            if (!isFogActiveFor(sp)) {
                playerBrightChunks.remove(sp.getUUID());
                continue;
            }
            UUID uuid = sp.getUUID();
            String ownerName = sp.getName().getString();

            LongOpenHashSet nearViewers = new LongOpenHashSet();
            LongOpenHashSet farViewers = new LongOpenHashSet();

            // FPS players carry a personal bubble; orthoview sees only via units/buildings + allies
            if (!orthoviewUuids.contains(uuid))
                nearViewers.add(chunkLong(sp.blockPosition().getX(), sp.blockPosition().getZ()));

            // Self
            LongOpenHashSet ownNear = nearByOwner.get(ownerName);
            if (ownNear != null) nearViewers.addAll(ownNear);
            LongOpenHashSet ownFar = farByOwner.get(ownerName);
            if (ownFar != null) farViewers.addAll(ownFar);

            // Allies: precompute allied owner set once for this player, then union their buckets
            Set<String> alliedOwners = collectAlliedOwners(ownerName, nearByOwner.keySet(), farByOwner.keySet());
            for (String ally : alliedOwners) {
                LongOpenHashSet n = nearByOwner.get(ally);
                if (n != null) nearViewers.addAll(n);
                LongOpenHashSet f = farByOwner.get(ally);
                if (f != null) farViewers.addAll(f);
            }

            LongOpenHashSet brightLong = new LongOpenHashSet();
            expandInto(brightLong, nearViewers, CHUNK_VIEW_DIST);
            expandInto(brightLong, farViewers, CHUNK_FAR_VIEW_DIST);
            brightLong.addAll(forceRevealed);

            // Materialise to Set<ChunkPos> only at the boundary (public API + network packet)
            Set<ChunkPos> bright = new HashSet<>(brightLong.size() * 2);
            for (LongIterator it = brightLong.iterator(); it.hasNext(); )
                bright.add(new ChunkPos(it.nextLong()));

            Set<ChunkPos> previous = playerBrightChunks.put(uuid, bright);

            // resend full chunk data for newly-bright chunks; ChunkMapMixin dropped block updates
            // while dark, so the client's copy is stale
            ServerLevel level = sp.serverLevel();
            for (ChunkPos cp : bright) {
                if (previous != null && previous.contains(cp)) continue;
                LevelChunk chunk = level.getChunkSource().getChunk(cp.x, cp.z, false);
                if (chunk == null) continue;
                sp.connection.send(new ClientboundLevelChunkWithLightPacket(
                        chunk, level.getLightEngine(), null, null));
            }

            if (previous == null || !previous.equals(bright))
                FogChunksClientboundPacket.send(sp, bright);
        }
    }

    private static long chunkLong(int blockX, int blockZ) {
        return ChunkPos.asLong(blockX >> 4, blockZ >> 4);
    }

    private static void expandInto(LongOpenHashSet out, LongOpenHashSet viewers, int dist) {
        for (LongIterator it = viewers.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            int cx = ChunkPos.getX(packed);
            int cz = ChunkPos.getZ(packed);
            for (int dx = -dist; dx <= dist; dx++)
                for (int dz = -dist; dz <= dist; dz++)
                    out.add(ChunkPos.asLong(cx + dx, cz + dz));
        }
    }

    private static Set<String> collectAlliedOwners(String me, Set<String> nearOwners, Set<String> farOwners) {
        Set<String> out = new HashSet<>();
        for (String o : nearOwners)
            if (!o.equals(me) && AlliancesServerEvents.isAllied(me, o)) out.add(o);
        for (String o : farOwners)
            if (!o.equals(me) && !out.contains(o) && AlliancesServerEvents.isAllied(me, o)) out.add(o);
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
