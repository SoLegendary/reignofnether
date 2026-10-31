package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.ReignOfNether;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.registries.RegistryManager;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * One match being written to a .ronreplay zip. Every clientbound packet is encoded on the sending thread and
 * buffered for the tick; a packet sent to several players in one tick (same instance, or same bytes) is
 * stored once with all its recipients as a bitmask. At the end of each server tick the buffer is handed to
 * a writer thread that deflates it into packets.bin, so the tick thread never touches the disk.
 *
 * packets.bin is a sequence of: int tick, long recipients, int packetId (-1 = camera), int length, bytes.
 */
public class ReplayRecorder {

    public static final String OMNI = "*"; // what a viewer with no fog and endless view distance gets: not a real name
    public static final int SNAPSHOT_BIT = 63; // the start snapshot: seen by no-one in particular
    public static final int CAMERA_ID = -1;
    private static final int MAX_PLAYER_BITS = 63;
    private static final byte[] END = new byte[0];

    // only ever about the receiving player's own client (their inventory, their camera, their chunk loading...),
    // or private: in a match normal chat goes through TeamChat, so vanilla player chat is only /msg and the like.
    // Playback drops these too, since a shared replay file can hold anything
    static final Set<Class<?>> IGNORED = Set.of(
            ClientboundPlayerChatPacket.class,
            ClientboundLoginPacket.class, ClientboundRespawnPacket.class, ClientboundKeepAlivePacket.class,
            ClientboundPingPacket.class, ClientboundDisconnectPacket.class, ClientboundPlayerPositionPacket.class,
            ClientboundPlayerAbilitiesPacket.class, ClientboundSetHealthPacket.class, ClientboundSetExperiencePacket.class,
            ClientboundSetCarriedItemPacket.class, ClientboundContainerSetContentPacket.class,
            ClientboundContainerSetSlotPacket.class, ClientboundContainerSetDataPacket.class,
            ClientboundContainerClosePacket.class, ClientboundOpenScreenPacket.class, ClientboundOpenBookPacket.class,
            ClientboundOpenSignEditorPacket.class, ClientboundHorseScreenOpenPacket.class,
            ClientboundMerchantOffersPacket.class, ClientboundSetCameraPacket.class,
            ClientboundSetChunkCacheCenterPacket.class, ClientboundSetChunkCacheRadiusPacket.class,
            ClientboundSetSimulationDistancePacket.class, ClientboundForgetLevelChunkPacket.class,
            ClientboundCommandsPacket.class, ClientboundUpdateRecipesPacket.class, ClientboundRecipePacket.class,
            ClientboundUpdateTagsPacket.class, ClientboundUpdateAdvancementsPacket.class,
            ClientboundSelectAdvancementsTabPacket.class, ClientboundCooldownPacket.class,
            ClientboundAwardStatsPacket.class, ClientboundResourcePackPacket.class, ClientboundServerDataPacket.class,
            ClientboundCommandSuggestionsPacket.class, ClientboundTabListPacket.class,
            ClientboundCustomChatCompletionsPacket.class, ClientboundPlayerCombatEnterPacket.class,
            ClientboundPlayerCombatEndPacket.class, ClientboundPlayerCombatKillPacket.class,
            ClientboundPlayerLookAtPacket.class, ClientboundBlockChangedAckPacket.class,
            ClientboundSetDefaultSpawnPositionPacket.class, ClientboundDeleteChatPacket.class,
            ClientboundDisguisedChatPacket.class, ClientboundSetTitleTextPacket.class,
            ClientboundSetSubtitleTextPacket.class, ClientboundSetTitlesAnimationPacket.class,
            ClientboundClearTitlesPacket.class, ClientboundSetActionBarTextPacket.class,
            ClientboundChangeDifficultyPacket.class, ClientboundUpdateEnabledFeaturesPacket.class,
            ClientboundPlaceGhostRecipePacket.class, ClientboundTagQueryPacket.class, ClientboundMoveVehiclePacket.class
    );
    private static final Set<ClientboundGameEventPacket.Type> WEATHER = Set.of(
            ClientboundGameEventPacket.START_RAINING, ClientboundGameEventPacket.STOP_RAINING,
            ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE);

    private record Key(int id, ByteBuffer data) { }

    private static class Entry {
        final int id;
        final byte[] bytes;
        final int index;
        long mask;

        Entry(int id, byte[] bytes, long mask, int index) {
            this.id = id;
            this.bytes = bytes;
            this.mask = mask;
            this.index = index;
        }
    }

    public final ReplayHeader header = new ReplayHeader();
    public final Path target;
    private final Path part;
    private final ZipOutputStream zip;
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private volatile String headerJson = null;
    private volatile boolean failed = false;

    private int tick = 0;
    private final List<Entry> entries = new ArrayList<>();
    private final IdentityHashMap<Packet<?>, Entry> byInstance = new IdentityHashMap<>();
    private final Map<Key, Entry> byContent = new HashMap<>();
    private final int[] lastIndex = new int[64]; // per recipient: their last entry this tick, so merging never reorders
    private final int[] cameraTick = new int[64]; // per recipient: last tick a camera was kept, one per tick at most
    private final Map<String, Integer> bits = new HashMap<>();
    private final Map<Long, byte[]> lastChunks = new HashMap<>(); // fog of war resends chunks a lot, mostly unchanged

    /** onWritten gets the saved file, or null if it couldn't be saved; called on the writer thread. */
    public ReplayRecorder(Path target, Consumer<Path> onWritten) throws IOException {
        this.target = target;
        this.part = target.resolveSibling(target.getFileName() + ".part");
        Files.createDirectories(target.getParent());
        this.zip = new ZipOutputStream(Files.newOutputStream(part));
        zip.putNextEntry(new ZipEntry(ReplayHeader.PACKETS_ENTRY));
        header.date = System.currentTimeMillis();
        Arrays.fill(lastIndex, -1);

        byte[] registries = registrySnapshot();
        Thread writer = new Thread(() -> {
            try (OutputStream out = zip) {
                while (true) {
                    byte[] block = queue.take();
                    if (block == END)
                        break;
                    out.write(block);
                }
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry(ReplayHeader.HEADER_ENTRY));
                zip.write(headerJson.getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                zip.putNextEntry(new ZipEntry(ReplayHeader.REGISTRIES_ENTRY));
                zip.write(registries);
                zip.closeEntry();
                zip.finish();
            } catch (Exception e) {
                failed = true;
                queue.clear();
                ReignOfNether.LOGGER.error("[Replay] failed to write {}", part, e);
                onWritten.accept(null);
                return;
            }
            try {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
                ReignOfNether.LOGGER.info("[Replay] saved {}", target);
                onWritten.accept(target);
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("[Replay] failed to save {}", target, e);
                onWritten.accept(null);
            }
        }, "RoN replay writer");
        writer.setDaemon(false); // a dedicated server shutting down waits for the replay to be saved
        writer.start();
    }

    public int tick() {
        return tick;
    }

    /** A packet sent to this player. */
    public synchronized void record(String playerName, Packet<?> packet) {
        if (failed)
            return;
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> p : bundle.subPackets())
                record(playerName, p);
            return;
        }
        if (IGNORED.contains(packet.getClass()))
            return;
        // other mods' channels can carry per-player secrets (voice chat keys...): only ours and Forge's entity spawns
        if (packet instanceof ClientboundCustomPayloadPacket cp && !cp.getIdentifier().getNamespace().equals(ReignOfNether.MOD_ID)
                && !cp.getIdentifier().toString().equals("fml:play"))
            return;
        if (packet instanceof ClientboundGameEventPacket ge && !WEATHER.contains(ge.getEvent()))
            return;
        int bit = bit(playerName);
        Entry e = byInstance.get(packet);
        if (e != null && canMerge(e, bit)) {
            merge(e, bit);
            return;
        }
        byte[] bytes = encode(packet);
        if (bytes == null || unchangedChunk(packet, bytes))
            return;
        int id = ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet);
        Key key = new Key(id, ByteBuffer.wrap(bytes));
        e = byContent.get(key);
        if (e != null && canMerge(e, bit)) {
            merge(e, bit);
            byInstance.put(packet, e);
            return;
        }
        e = add(id, bytes, 1L << bit);
        lastIndex[bit] = e.index;
        byInstance.put(packet, e);
        byContent.put(key, e);
    }

    /** Part of the start snapshot, as if seen by the players in mask (SNAPSHOT_BIT for nobody in particular). */
    public synchronized void recordSnapshot(Packet<?> packet, long mask) {
        if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> p : bundle.subPackets())
                recordSnapshot(p, mask);
            return;
        }
        if (byInstance.containsKey(packet)) // a capture broadcast reaches every connection
            return;
        byte[] bytes = encode(packet);
        if (bytes != null && !unchangedChunk(packet, bytes))
            byInstance.put(packet, add(ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet), bytes, mask));
    }

    // playback keeps every chunk it was given, so a chunk identical to the last one recorded adds nothing
    private boolean unchangedChunk(Packet<?> packet, byte[] bytes) {
        return packet instanceof ClientboundLevelChunkWithLightPacket c
                && Arrays.equals(lastChunks.put(ChunkPos.asLong(c.getX(), c.getZ()), bytes), bytes);
    }

    // packets name blocks, entity types and items by number, and each world numbers modded ones its own way
    private static byte[] registrySnapshot() {
        CompoundTag tag = new CompoundTag();
        RegistryManager.ACTIVE.takeSnapshot(true).forEach((key, snapshot) -> tag.put(key.toString(), snapshot.write()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            NbtIo.writeCompressed(tag, out);
        } catch (IOException ignored) { } // in memory
        return out.toByteArray();
    }

    public synchronized void recordCamera(String playerName, float x, float z, float zoom, float rot) {
        ByteBuffer b = ByteBuffer.allocate(16).putFloat(x).putFloat(z).putFloat(zoom).putFloat(rot);
        int bit = bit(playerName);
        if (cameraTick[bit] == tick + 1)
            return;
        cameraTick[bit] = tick + 1;
        lastIndex[bit] = add(CAMERA_ID, b.array(), 1L << bit).index;
    }

    public synchronized long maskOf(String playerName) {
        return 1L << bit(playerName);
    }

    /** Hands this tick's packets to the writer. */
    public synchronized void endTick() {
        flush();
        tick++;
    }

    /** Hands what's buffered so far to the writer, still in this tick (the start snapshot is big). */
    public synchronized void flush() {
        if (!entries.isEmpty() && !failed) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                for (Entry e : entries) {
                    out.writeInt(tick);
                    out.writeLong(e.mask);
                    out.writeInt(e.id);
                    out.writeInt(e.bytes.length);
                    out.write(e.bytes);
                }
            } catch (IOException ignored) { } // in memory
            queue.add(bytes.toByteArray());
        }
        entries.clear();
        byInstance.clear();
        byContent.clear();
        Arrays.fill(lastIndex, -1);
    }

    /** Writes the header and closes the file on the writer thread. */
    public synchronized void finish() {
        endTick();
        header.durationTicks = tick;
        headerJson = header.toJson();
        queue.add(END);
    }

    private int bit(String name) {
        return bits.computeIfAbsent(name, n -> {
            // ponytail: past 63 players everyone else shares the last bit; add a wider mask if that ever matters
            if (header.recipients.size() < MAX_PLAYER_BITS)
                header.recipients.add(n);
            return header.recipients.size() - 1;
        });
    }

    private boolean canMerge(Entry e, int bit) {
        return (e.mask & (1L << bit)) == 0 && e.index > lastIndex[bit];
    }

    private void merge(Entry e, int bit) {
        e.mask |= 1L << bit;
        lastIndex[bit] = e.index;
    }

    private Entry add(int id, byte[] bytes, long mask) {
        Entry e = new Entry(id, bytes, mask, entries.size());
        entries.add(e);
        return e;
    }

    private static boolean warnedEncode = false;

    private static byte[] encode(Packet<?> packet) {
        if (ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet) < 0)
            return null;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } catch (Exception e) {
            if (!warnedEncode) {
                warnedEncode = true;
                ReignOfNether.LOGGER.warn("[Replay] could not encode {}, skipping packets like it", packet.getClass().getName(), e);
            }
            return null;
        } finally {
            buf.release();
        }
    }
}
