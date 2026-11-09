package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingAction;
import com.solegendary.reignofnether.building.BuildingClientboundPacket;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.fogofwar.FogChunksClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.fogofwar.FogSyncClientboundPacket;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.WorldState;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gives a player who reconnects mid-match the fog they had: the blocks and buildings they last saw in what is
 * dark for them now, read back from the match's recording. Fog is stateless, so without this a reconnect shows
 * the pre-match snapshot everywhere.
 *
 * Each column's last-seen state is every block change up to the last time that column was visible to the player
 * (a column coming back into view gets the whole chunk resent, so it catches up on what changed while dark). The
 * filter runs on the closed segments, off the server thread, and only what passes is sent, so nothing hidden
 * reaches the client. Chunks the player has been sent since joining are skipped: they're newer than the recording.
 */
public class FogRestore {

    private static final ResourceLocation MOD_CHANNEL = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "main");
    private static final int WAIT_FOR_RECORDING_TICKS = 20 * 10; // the recording resumes a moment after the host joins
    private static final int BLOCKS_PER_TICK = 4096;
    private static final long TIMEOUT_SECONDS = 60;

    private record Known(BuildingClientboundPacket place, @Nullable BuildingClientboundPacket sync,
                         Map<BuildingAction, BuildingClientboundPacket> changes) { }

    private static class Result {
        final Map<Long, List<Map.Entry<BlockPos, BlockState>>> blocksByChunk = new HashMap<>();
        final List<Known> buildings = new ArrayList<>();
        @Nullable Iterator<Map.Entry<Long, List<Map.Entry<BlockPos, BlockState>>>> sending = null;
    }

    private static final Map<UUID, Integer> waiting = WorldState.server(new HashMap<>()); // ticks since joining
    private static final Map<UUID, Result> ready = WorldState.server(new ConcurrentHashMap<>());
    // chunks sent to the player since they joined: what they have is newer than the recording there
    private static final Map<UUID, Set<Long>> touched = WorldState.server(new ConcurrentHashMap<>());

    public static void onJoin(ServerPlayer sp) {
        waiting.put(sp.getUUID(), 0);
        touched.put(sp.getUUID(), ConcurrentHashMap.newKeySet());
    }

    /** FogOfWarServerEvents: the chunks this player has data for after a fog update. */
    public static void onVision(UUID uuid, Set<ChunkPos> sent) {
        Set<Long> t = touched.get(uuid);
        if (t != null)
            for (ChunkPos cp : sent)
                t.add(cp.toLong());
    }

    static void tick(MinecraftServer server, @Nullable ReplayRecorder r) {
        Iterator<Map.Entry<UUID, Integer>> it = waiting.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
            // no recording, or no fog for them (a spectator, fog off): nothing to restore
            if (sp == null || e.getValue() > WAIT_FOR_RECORDING_TICKS) {
                it.remove();
                touched.remove(e.getKey());
                continue;
            }
            e.setValue(e.getValue() + 1);
            if (r == null || !FogOfWarServerEvents.isFogActiveFor(sp))
                continue;
            it.remove();
            String name = sp.getName().getString();
            int bit = r.bitOf(name);
            if (bit < 0 || r.failed()) { // nothing recorded for them: a first join, not a reconnect
                touched.remove(e.getKey());
                continue;
            }
            start(sp, r, bit);
        }
        Iterator<Map.Entry<UUID, Result>> rit = ready.entrySet().iterator();
        while (rit.hasNext()) {
            Map.Entry<UUID, Result> e = rit.next();
            ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
            if (sp == null || send(sp, e.getValue())) {
                rit.remove();
                touched.remove(e.getKey());
                if (sp != null)
                    FogSyncClientboundPacket.send(sp, false);
            }
        }
    }

    private static void start(ServerPlayer sp, ReplayRecorder r, int bit) {
        UUID uuid = sp.getUUID();
        FogSyncClientboundPacket.send(sp, true);
        Path dir = r.dir;
        long omni = r.maskOf(ReplayRecorder.OMNI);
        long started = System.currentTimeMillis();
        r.roll(ReplayServerEvents.maxEntityId())
                .thenApplyAsync(segments -> filter(dir, segments, 1L << bit, omni))
                .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        ReignOfNether.LOGGER.error("[FogRestore] couldn't restore {}'s fog", sp.getName().getString(), ex);
                        result = new Result();
                    } else {
                        ReignOfNether.LOGGER.info("[FogRestore] {}: {} chunks, {} buildings in {} ms", sp.getName().getString(),
                                result.blocksByChunk.size(), result.buildings.size(), System.currentTimeMillis() - started);
                    }
                    ready.put(uuid, result);
                });
    }

    // @return whether everything has been sent
    private static boolean send(ServerPlayer sp, Result res) {
        Set<Long> t = touched.getOrDefault(sp.getUUID(), Set.of());
        if (res.sending == null)
            res.sending = res.blocksByChunk.entrySet().iterator();
        int budget = BLOCKS_PER_TICK;
        while (budget > 0 && res.sending.hasNext()) {
            Map.Entry<Long, List<Map.Entry<BlockPos, BlockState>>> e = res.sending.next();
            if (t.contains(e.getKey()))
                continue;
            for (Map.Entry<BlockPos, BlockState> b : e.getValue())
                sp.connection.send(new ClientboundBlockUpdatePacket(b.getKey(), b.getValue()));
            budget -= e.getValue().size();
        }
        if (res.sending.hasNext())
            return false;
        for (Known k : res.buildings) {
            if (wasSent(t, k.place.buildingPos))
                continue;
            sendTo(sp, k.place);
            if (k.sync != null)
                sendTo(sp, k.sync);
            for (BuildingClientboundPacket change : k.changes.values())
                sendTo(sp, change);
        }
        return true;
    }

    // the player has been sent a chunk of it since joining, so they have it as it is now
    private static boolean wasSent(Set<Long> touched, BlockPos pos) {
        BuildingPlacement b = BuildingUtils.findBuilding(false, pos);
        if (b == null)
            return touched.contains(ChunkPos.asLong(pos));
        int x0 = b.minCorner.getX() >> 4, x1 = b.maxCorner.getX() >> 4, z0 = b.minCorner.getZ() >> 4, z1 = b.maxCorner.getZ() >> 4;
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++)
                if (touched.contains(ChunkPos.asLong(x, z)))
                    return true;
        return false;
    }

    private static void sendTo(ServerPlayer sp, Object modMessage) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sp), modMessage);
    }

    // ------------------------------------------------------------------ filter (off the server thread)

    private static Result filter(Path dir, int segments, long me, long omni) {
        int blockId = ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND,
                new ClientboundBlockUpdatePacket(BlockPos.ZERO, Blocks.AIR.defaultBlockState()));
        int payloadId = ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND,
                new ClientboundCustomPayloadPacket(MOD_CHANNEL, new FriendlyByteBuf(Unpooled.buffer())));
        int fogDisc = -1, buildingDisc = -1;
        for (Map.Entry<Integer, String> e : ReplayHeader.currentPacketIds().entrySet()) {
            if (e.getValue().equals(FogChunksClientboundPacket.class.getName())) fogDisc = e.getKey();
            if (e.getValue().equals(BuildingClientboundPacket.class.getName())) buildingDisc = e.getKey();
        }
        long snapshot = 1L << ReplayRecorder.SNAPSHOT_BIT;

        Set<ChunkPos> bright = Set.of();
        Map<ChunkPos, long[]> edges = Map.of();
        Map<Long, Map<BlockPos, BlockState>> latest = new HashMap<>();  // chunk -> what the player saw last
        Map<Long, Map<BlockPos, BlockState>> pending = new HashMap<>(); // chunk -> changed while dark there
        Map<BlockPos, BuildingClientboundPacket> places = new HashMap<>();
        Map<BlockPos, BuildingClientboundPacket> syncs = new HashMap<>();
        Map<BlockPos, Map<BuildingAction, BuildingClientboundPacket>> changes = new HashMap<>();
        Map<BlockPos, BuildingClientboundPacket> startPlaces = new HashMap<>(); // the start snapshot's, seen by nobody in particular

        for (int i = 0; i < segments; i++) {
            try (DataInputStream in = new DataInputStream(ReplayRecorder.openSegmentForRead(dir, i))) {
                while (true) {
                    long mask;
                    int id, len;
                    try {
                        in.readInt(); // tick
                        mask = in.readLong();
                        id = in.readInt();
                        len = in.readInt();
                    } catch (EOFException end) {
                        break;
                    }
                    boolean block = id == blockId && (mask & omni) != 0;
                    boolean mod = id == payloadId && (mask & (me | snapshot)) != 0;
                    if (!block && !mod) {
                        in.skipNBytes(len);
                        continue;
                    }
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(in.readNBytes(len)));
                    if (block) {
                        ClientboundBlockUpdatePacket p = new ClientboundBlockUpdatePacket(buf);
                        BlockPos pos = p.getPos();
                        long chunk = ChunkPos.asLong(pos);
                        if (isVisible(bright, edges, pos)) {
                            latest.computeIfAbsent(chunk, k -> new HashMap<>()).put(pos, p.getBlockState());
                            Map<BlockPos, BlockState> pend = pending.get(chunk);
                            if (pend != null)
                                pend.remove(pos);
                        } else {
                            pending.computeIfAbsent(chunk, k -> new HashMap<>()).put(pos, p.getBlockState());
                        }
                        continue;
                    }
                    if (!buf.readResourceLocation().equals(MOD_CHANNEL))
                        continue;
                    int disc = buf.readUnsignedByte();
                    boolean mine = (mask & me) != 0;
                    if (disc == fogDisc && mine) {
                        FogChunksClientboundPacket f = new FogChunksClientboundPacket(buf);
                        bright = f.bright;
                        edges = f.edgeMasks;
                        reveal(bright, edges, pending, latest);
                    } else if (disc == buildingDisc) {
                        BuildingClientboundPacket b = new BuildingClientboundPacket(buf);
                        boolean place = b.action == BuildingAction.PLACE || b.action == BuildingAction.PLACE_CUSTOM;
                        if (!mine) {
                            if (place)
                                startPlaces.putIfAbsent(b.buildingPos, b);
                        } else if (place) {
                            places.put(b.buildingPos, b);
                            syncs.remove(b.buildingPos);
                            changes.remove(b.buildingPos);
                        } else if (b.action == BuildingAction.REMOVE) {
                            places.remove(b.buildingPos);
                            syncs.remove(b.buildingPos);
                            changes.remove(b.buildingPos);
                        } else if (b.action == BuildingAction.SYNC_BLOCKS_AND_OWNER) {
                            syncs.put(b.buildingPos, b);
                        } else {
                            changes.computeIfAbsent(b.buildingPos, k -> new LinkedHashMap<>()).put(b.action, b);
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Result res = new Result();
        latest.forEach((chunk, blocks) -> res.blocksByChunk.put(chunk, new ArrayList<>(blocks.entrySet())));
        // known from a sync while it was in view, placed before the recording began: the start snapshot's PLACE
        // is older than what they saw, never newer
        Set<BlockPos> known = new HashSet<>(places.keySet());
        known.addAll(syncs.keySet());
        known.addAll(changes.keySet());
        for (BlockPos pos : known) {
            BuildingClientboundPacket place = places.getOrDefault(pos, startPlaces.get(pos));
            if (place != null)
                res.buildings.add(new Known(place, syncs.get(pos), changes.getOrDefault(pos, Map.of())));
        }
        return res;
    }

    // columns that came into view: the chunk was resent, so what changed there while dark is now seen
    private static void reveal(Set<ChunkPos> bright, Map<ChunkPos, long[]> edges,
                               Map<Long, Map<BlockPos, BlockState>> pending, Map<Long, Map<BlockPos, BlockState>> latest) {
        Iterator<Map.Entry<Long, Map<BlockPos, BlockState>>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Map<BlockPos, BlockState>> e = it.next();
            if (!bright.contains(new ChunkPos(e.getKey())))
                continue;
            Iterator<Map.Entry<BlockPos, BlockState>> bit = e.getValue().entrySet().iterator();
            while (bit.hasNext()) {
                Map.Entry<BlockPos, BlockState> b = bit.next();
                if (isVisible(bright, edges, b.getKey())) {
                    latest.computeIfAbsent(e.getKey(), k -> new HashMap<>()).put(b.getKey(), b.getValue());
                    bit.remove();
                }
            }
            if (e.getValue().isEmpty())
                it.remove();
        }
    }

    // same rule as FogOfWarServerEvents.isBlockVisibleFor: a sent chunk, and its column if it's an edge chunk
    private static boolean isVisible(Set<ChunkPos> bright, Map<ChunkPos, long[]> edges, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        if (!bright.contains(cp))
            return false;
        long[] mask = edges.get(cp);
        if (mask == null)
            return true;
        int i = ((pos.getX() & 15) << 4) | (pos.getZ() & 15);
        return (mask[i >> 6] & (1L << (i & 63))) != 0;
    }
}
