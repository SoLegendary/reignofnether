package com.solegendary.reignofnether.replay;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.config.ClientboundSyncResourceCostPacket;
import com.solegendary.reignofnether.fogofwar.FogChunksClientboundPacket;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.fogofwar.PlayerChunksClientboundPacket;
import com.solegendary.reignofnether.hud.HudClientboundPacket;
import com.solegendary.reignofnether.matchstart.MatchResumeClientboundPacket;
import com.solegendary.reignofnether.orthoview.CameraClientboundPacket;
import com.solegendary.reignofnether.orthoview.CameraFadeClientboundPacket;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.soldier.SoldierStateClientboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialClientboundPacket;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Toast;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

/**
 * Plays a .ronreplay back as a spectator, and downloads replays from the end screen.
 *
 * Playback opens a throwaway flat singleplayer world (so login, registries and the RTS camera all work as
 * usual) whose own server sends nothing but the bare connection (ReplayServerEvents.playbackWorld). The
 * recorded packets are fed straight into the client's packet listener: everything every player was sent,
 * merged, so the viewer sees the whole match like a spectator without fog. Entities stay until the last
 * player who saw them stops seeing them. The spectator top bar's "view as" picks whose recorded fog to show;
 * the client already hides entities and tints terrain outside it, so no server-side fog is needed.
 */
public class ReplayClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final String VIEWER_NAME = "Replay Viewer"; // can't be a real player's name
    private static final ResourceLocation MOD_CHANNEL = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "main");
    private static final ResourceLocation FORGE_PLAY = ResourceLocation.fromNamespaceAndPath("fml", "play");
    private static final int FORGE_OPEN_CONTAINER = 1;
    public static final float[] SPEEDS = { 0.5f, 1, 2, 4, 8 };
    private static final int MAX_FAILURES = 500;
    private static final long TICK_BUDGET_NS = 40_000_000L; // fast-forwarding never freezes the game for long
    private static final long SEEK_BUDGET_NS = 150_000_000L; // jumping shows a loading screen, so it can take more
    private static final int CAMERA_SEND_TICKS = 10;
    private static final long MAX_DOWNLOAD_BYTES = 512L * 1024 * 1024;

    // the recipients' own clients: per-player screens, camera moves, debug and config
    private static final Set<String> BLOCKED = Set.of(
            CameraClientboundPacket.class.getName(), CameraFadeClientboundPacket.class.getName(),
            MatchResumeClientboundPacket.class.getName(), PlayerChunksClientboundPacket.class.getName(),
            HudClientboundPacket.class.getName(), SoldierStateClientboundPacket.class.getName(),
            ClientboundSyncResourceCostPacket.class.getName(), TutorialClientboundPacket.class.getName(),
            ReplayChunkClientboundPacket.class.getName());

    private record Rec(int tick, long mask, int id, byte[] bytes) { }

    @Nullable private static ReplayHeader header = null;
    private static boolean started = false;
    @Nullable private static ZipFile zip = null;
    @Nullable private static DataInputStream in = null;
    @Nullable private static Rec next = null;
    private static final int[] remap = new int[256];
    private static final String[] currentNames = new String[256];

    private static double tick = 0;
    private static int seekTo = -1;
    private static int seekFrom = 0;
    private static final Map<Long, Packet<?>> heldChunks = new LinkedHashMap<>();
    private static boolean paused = false;
    private static int speedIndex = 1;
    private static int handled = 0, failures = 0;

    private static final Map<Integer, Long> trackers = new HashMap<>(); // entity id -> recipients who see it
    private static final Map<String, FogChunksClientboundPacket> fog = new HashMap<>();
    private static Set<String> fogViews = Set.of();
    private static boolean fogDirty = false;
    private static final Map<String, float[]> cameraTargets = new HashMap<>();
    private static final Map<String, float[]> cameras = new HashMap<>(); // smoothed towards the targets

    public static boolean isActive() {
        return header != null && started;
    }

    @Nullable
    public static ReplayHeader header() {
        return header;
    }

    public static int tick() {
        return seekTo >= 0 ? seekTo : (int) tick;
    }

    public static boolean isSeeking() {
        return seekTo >= 0;
    }

    /** 0 to 1, how far a jump has got. */
    public static float seekProgress() {
        return seekTo <= seekFrom ? 1 : Mth.clamp((float) (tick - seekFrom) / (seekTo - seekFrom), 0, 1);
    }

    public static boolean isPaused() {
        return paused;
    }

    public static void togglePause() {
        if (paused && next == null)
            seek(0); // at the end: play again
        paused = !paused;
    }

    public static int speedIndex() {
        return speedIndex;
    }

    public static void setSpeedIndex(int i) {
        speedIndex = Mth.clamp(i, 0, SPEEDS.length - 1);
    }

    /** x, z, zoom and rotation of this player's RTS camera, or null if they never sent one. */
    @Nullable
    public static float[] camera(String name) {
        return isActive() ? cameras.get(name) : null;
    }

    // ------------------------------------------------------------------ starting and stopping

    public static void watch(ReplayHeader h) {
        header = h;
        started = false;
        paused = false;
        speedIndex = 1;
        // the replay's entity ids must never land on our own player
        try {
            AtomicInteger counter = ObfuscationReflectionHelper.getPrivateValue(Entity.class, null, "f_19843_");
            if (counter != null)
                counter.updateAndGet(c -> Math.max(c, h.maxEntityId + 1000));
        } catch (RuntimeException e) {
            ReignOfNether.LOGGER.warn("[Replay] could not move the entity id counter", e);
        }
        LevelStorageSource source = MC.getLevelSource();
        if (source.levelExists(ReplayServerEvents.PLAYBACK_WORLD)) {
            try (LevelStorageSource.LevelStorageAccess access = source.createAccess(ReplayServerEvents.PLAYBACK_WORLD)) {
                access.deleteLevel();
            } catch (IOException e) {
                ReignOfNether.LOGGER.warn("[Replay] could not delete the old replay world", e);
            }
        }
        useRecordedIds(h);
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        MC.createWorldOpenFlows().createFreshLevel(ReplayServerEvents.PLAYBACK_WORLD,
                new LevelSettings(ReplayServerEvents.PLAYBACK_WORLD, GameType.SPECTATOR, false, Difficulty.NORMAL, false, rules, WorldDataConfiguration.DEFAULT),
                new WorldOptions(0, false, false),
                reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    // the replay world takes the recorded world's ids, as Forge does when opening a saved world (or a worker
    // replays as some other mob). Forge puts its own back when that world's server stops
    private static void useRecordedIds(ReplayHeader h) {
        try (ZipFile zip = new ZipFile(h.file.toFile())) {
            ZipEntry entry = zip.getEntry(ReplayHeader.REGISTRIES_ENTRY);
            if (entry == null) { // recorded before ids were saved: right only if that world numbered things as we do
                ReignOfNether.LOGGER.warn("[Replay] {} has no saved ids, units may show as the wrong mob", h.file.getFileName());
                return;
            }
            CompoundTag tag = NbtIo.read(new DataInputStream(new BufferedInputStream(new GZIPInputStream(zip.getInputStream(entry)))),
                    new NbtAccounter(64L << 20)); // a shared file: bounded
            Map<ResourceLocation, ForgeRegistry.Snapshot> snapshot = new HashMap<>();
            for (String key : tag.getAllKeys()) {
                ResourceLocation rl = ResourceLocation.tryParse(key);
                if (rl != null)
                    snapshot.put(rl, ForgeRegistry.Snapshot.read(tag.getCompound(key)));
            }
            Multimap<ResourceLocation, ResourceLocation> missing = GameData.injectSnapshot(snapshot, true, true);
            if (!missing.isEmpty())
                ReignOfNether.LOGGER.warn("[Replay] recorded with things this game doesn't have: {}", missing);
        } catch (IOException | RuntimeException e) {
            ReignOfNether.LOGGER.warn("[Replay] could not load the recorded ids", e);
            GameData.revertToFrozen(); // never leave half-loaded ids behind
        }
    }

    private static void begin() {
        started = true;
        ReplayHeader h = header;
        if (MC.level.getMinBuildHeight() != h.minBuildHeight || MC.level.getHeight() != h.buildHeight) {
            stop("replay.reignofnether.error.world");
            return;
        }
        // we're not any of the recorded players, even when watching our own match
        try {
            ObfuscationReflectionHelper.findField(Player.class, "f_36084_")
                    .set(MC.player, new GameProfile(MC.player.getUUID(), VIEWER_NAME));
        } catch (ReflectiveOperationException | RuntimeException e) {
            ReignOfNether.LOGGER.warn("[Replay] could not rename the viewer", e);
        }
        // hold every chunk of the map at once: the recorded chunk packets are never sent again
        int cx = (h.minChunkX + h.maxChunkX) / 2, cz = (h.minChunkZ + h.maxChunkZ) / 2;
        int radius = Mth.clamp(Math.max(h.maxChunkX - h.minChunkX, h.maxChunkZ - h.minChunkZ) / 2 + 2, 2, 64);
        MC.level.getChunkSource().updateViewRadius(radius);
        MC.level.getChunkSource().updateViewCenter(cx, cz);

        Arrays.fill(remap, -1);
        Arrays.fill(currentNames, null);
        Map<String, Integer> byName = new HashMap<>();
        ReplayHeader.currentPacketIds().forEach((i, n) -> {
            currentNames[i] = n;
            byName.put(n, i);
        });
        if (h.packetIds.isEmpty())
            for (int i = 0; i < 256; i++)
                remap[i] = i;
        else
            h.packetIds.forEach((i, n) -> remap[i & 0xff] = byName.getOrDefault(n, -1));

        if (!open())
            return;
        MC.player.setPos(cx * 16 + 8, MC.player.getY(), cz * 16 + 8);
        if (!OrthoviewClientEvents.isEnabled())
            OrthoviewClientEvents.toggleEnable();
        if (!h.sameVersion())
            Toast.show(I18n.get("replay.reignofnether.version_warning", h.modVersion), Toast.DEFAULT_TICKS, Theme.ORANGE);
    }

    private static boolean open() {
        closeStream();
        try {
            zip = new ZipFile(header.file.toFile());
            in = new DataInputStream(new BufferedInputStream(zip.getInputStream(zip.getEntry(ReplayHeader.PACKETS_ENTRY)), 1 << 16));
            next = read();
        } catch (IOException | RuntimeException e) {
            ReignOfNether.LOGGER.error("[Replay] can't open {}", header.file, e);
            stop("replay.reignofnether.error.unreadable");
            return false;
        }
        tick = 0;
        heldChunks.clear();
        handled = failures = 0;
        trackers.clear();
        fog.clear();
        fogViews = Set.of();
        cameraTargets.clear();
        cameras.clear();
        return true;
    }

    // back to the start of the match, keeping the terrain-less replay world and our own player
    private static void restart() {
        List<Entity> entities = new ArrayList<>();
        MC.level.entitiesForRendering().forEach(entities::add);
        for (Entity e : entities)
            if (e != MC.player)
                MC.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
        PlayerClientEvents.resetAll();
        open();
    }

    private static void closeStream() {
        try {
            if (zip != null)
                zip.close();
        } catch (IOException ignored) { }
        zip = null;
        in = null;
        next = null;
    }

    /** Leaves the replay with an error screen. */
    private static void stop(String errorKey) {
        ReignOfNether.LOGGER.error("[Replay] stopped playback: {}", I18n.get(errorKey));
        closeStream();
        header = null;
        started = false;
        if (MC.level != null) {
            MC.level.disconnect();
            MC.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        }
        MC.setScreen(new AlertScreen(() -> MC.setScreen(new TitleScreen()),
                Component.translatable("replay.reignofnether.error.title"), Component.translatable(errorKey)));
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        resetDownload();
        if (!started) // opening the replay world logs out of the title screen first: keep the replay to watch
            return;
        closeStream();
        header = null;
        started = false;
        seekTo = -1;
    }

    // ------------------------------------------------------------------ playback

    public static void seek(int target) {
        if (!isActive())
            return;
        target = Mth.clamp(target, 0, header.durationTicks);
        if (target < tick)
            restart();
        seekFrom = (int) tick;
        seekTo = target;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        sendCamera();
        if (header == null || MC.player == null || MC.level == null || MC.getConnection() == null)
            return;
        if (!started) {
            if (ReplayServerEvents.playbackWorld && MC.getSingleplayerServer() != null)
                begin();
            return;
        }
        if (MC.screen instanceof ReceivingLevelScreen) // its own server never sends the chunk under us
            MC.setScreen(null);

        long deadline = System.nanoTime() + (seekTo >= 0 ? SEEK_BUDGET_NS : TICK_BUDGET_NS);
        if (seekTo >= 0) {
            if (deliverUntil(seekTo, deadline, true)) {
                heldChunks.values().forEach(ReplayClientEvents::handle);
                heldChunks.clear();
                tick = seekTo;
                seekTo = -1;
            }
        } else if (!paused && !MC.isPaused()) {
            tick = Math.min(tick + SPEEDS[speedIndex], header.durationTicks);
            deliverUntil((int) tick, deadline, false);
            if (next == null && tick >= header.durationTicks)
                paused = true;
        }
        if (header == null) // stopped on an error
            return;
        updateFog();
        updateCameras();
    }

    // @return true once everything up to this tick has been delivered
    private static boolean deliverUntil(int until, long deadline, boolean seeking) {
        while (next != null && next.tick <= until) {
            if (System.nanoTime() > deadline)
                return false;
            if (seeking)
                tick = next.tick;
            deliver(next);
            if (header == null)
                return true;
            try {
                next = read();
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("[Replay] {} is damaged", header.file, e);
                next = null; // play what we have
            }
        }
        return true;
    }

    @Nullable
    private static Rec read() throws IOException {
        int t;
        try {
            t = in.readInt();
        } catch (EOFException e) {
            return null;
        }
        long mask = in.readLong();
        int id = in.readInt();
        int len = in.readInt();
        if (len < 0 || len > 1 << 24)
            throw new IOException("bad packet length " + len);
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new Rec(t, mask, id, bytes);
    }

    @SuppressWarnings("unchecked")
    private static void deliver(Rec r) {
        handled++;
        try {
            if (r.id == ReplayRecorder.CAMERA_ID) {
                if (r.bytes.length != 16)
                    throw new IOException("bad camera record");
                ByteBuffer b = ByteBuffer.wrap(r.bytes);
                float[] cam = { b.getFloat(), b.getFloat(), b.getFloat(), b.getFloat() };
                for (String name : names(r.mask))
                    cameraTargets.put(name, cam);
                return;
            }
            Packet<?> p = ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, r.id, new FriendlyByteBuf(Unpooled.wrappedBuffer(r.bytes)));
            if (p == null)
                throw new IOException("unknown packet id " + r.id);
            p = filter(p, r.mask);
            if (p != null && !(seekTo >= 0 && hold(p)))
                handle(p);
        } catch (Exception e) {
            failures++;
            if (failures == 1 || failures % 100 == 0)
                ReignOfNether.LOGGER.warn("[Replay] skipped {} packets that failed to play, latest (id {}):", failures, r.id, e);
            if (failures > MAX_FAILURES && failures * 5 > handled)
                stop("replay.reignofnether.error.unreadable");
        }
    }

    @SuppressWarnings("unchecked")
    private static void handle(Packet<?> p) {
        ((Packet<ClientGamePacketListener>) p).handle(MC.getConnection());
    }

    // while jumping only a chunk's last state is drawn, not every resend on the way: thousands of chunk rebuilds.
    // Anything that edits a held chunk's blocks or light puts the chunk in first
    private static boolean hold(Packet<?> p) {
        if (p instanceof ClientboundLevelChunkWithLightPacket c) {
            heldChunks.put(ChunkPos.asLong(c.getX(), c.getZ()), c);
            return true;
        }
        long chunk;
        if (p instanceof ClientboundBlockUpdatePacket b)
            chunk = ChunkPos.asLong(b.getPos());
        else if (p instanceof ClientboundBlockEntityDataPacket b)
            chunk = ChunkPos.asLong(b.getPos());
        else if (p instanceof ClientboundLightUpdatePacket l)
            chunk = ChunkPos.asLong(l.getX(), l.getZ());
        else if (p instanceof ClientboundSectionBlocksUpdatePacket s) {
            long[] first = { Long.MIN_VALUE };
            s.runUpdates((pos, state) -> first[0] = ChunkPos.asLong(pos));
            chunk = first[0];
        } else
            return false;
        Packet<?> held = heldChunks.remove(chunk);
        if (held != null)
            handle(held);
        return false;
    }

    @Nullable
    private static Packet<?> filter(Packet<?> p, long mask) {
        if (ReplayRecorder.IGNORED.contains(p.getClass()))
            return null;
        if (p instanceof ClientboundCustomPayloadPacket cp)
            return modPacket(cp, mask);
        if (p instanceof ClientboundAddEntityPacket a)
            return added(a.getId(), mask) ? p : null;
        if (p instanceof ClientboundAddPlayerPacket a)
            return added(a.getEntityId(), mask) ? p : null;
        if (p instanceof ClientboundAddExperienceOrbPacket a)
            return added(a.getId(), mask) ? p : null;
        if (p instanceof ClientboundRemoveEntitiesPacket rm) {
            IntArrayList gone = new IntArrayList();
            for (int id : rm.getEntityIds()) {
                // stays while anyone else still sees it; the snapshot never "unsees", so drop it on any removal
                long left = trackers.getOrDefault(id, 0L) & ~mask & ~(1L << ReplayRecorder.SNAPSHOT_BIT);
                if (left == 0) {
                    trackers.remove(id);
                    if (id != MC.player.getId())
                        gone.add(id);
                } else {
                    trackers.put(id, left);
                }
            }
            return gone.isEmpty() ? null : new ClientboundRemoveEntitiesPacket(gone);
        }
        if (p instanceof ClientboundSystemChatPacket chat) { // plain text: no click events from a shared file
            long everyone = 0;
            for (int bit = 0; bit < Math.min(header.recipients.size(), ReplayRecorder.SNAPSHOT_BIT); bit++)
                if (!header.recipients.get(bit).equals(ReplayRecorder.OMNI))
                    everyone |= 1L << bit;
            if (!chat.overlay() && (mask & everyone) == everyone) // messages to one player stay private
                MC.gui.getChat().addMessage(Component.literal(chat.content().getString()));
            return null;
        }
        return p;
    }

    // @return whether this add should reach the level (the entity isn't there yet)
    private static boolean added(int id, long mask) {
        if (id == MC.player.getId())
            return false;
        Long had = trackers.put(id, trackers.getOrDefault(id, 0L) | mask);
        return had == null || MC.level.getEntity(id) == null;
    }

    @Nullable
    private static Packet<?> modPacket(ClientboundCustomPayloadPacket cp, long mask) {
        if (cp.getIdentifier().equals(FORGE_PLAY)) // spawn data yes, the players' own screens (opening containers) no
            return cp.getData().readUnsignedByte() == FORGE_OPEN_CONTAINER ? null : cp;
        if (!cp.getIdentifier().equals(MOD_CHANNEL))
            return null;
        FriendlyByteBuf data = cp.getData();
        int current = remap[data.readUnsignedByte()];
        String name = current >= 0 ? currentNames[current] : null;
        if (name == null) {
            failures++; // a packet this version doesn't have
            return null;
        }
        if (BLOCKED.contains(name))
            return null;
        if (name.equals(FogChunksClientboundPacket.class.getName())) {
            FogChunksClientboundPacket f = new FogChunksClientboundPacket(data);
            for (String n : names(mask))
                fog.put(n, f);
            fogDirty = true;
            return null;
        }
        FriendlyByteBuf out = new FriendlyByteBuf(Unpooled.buffer());
        out.writeByte(current);
        out.writeBytes(data);
        return new ClientboundCustomPayloadPacket(MOD_CHANNEL, out);
    }

    private static List<String> names(long mask) {
        List<String> names = new ArrayList<>();
        for (int bit = 0; bit < ReplayRecorder.SNAPSHOT_BIT; bit++)
            if ((mask & (1L << bit)) != 0 && bit < header.recipients.size())
                names.add(header.recipients.get(bit));
        return names;
    }

    // the spectator top bar's "view as": the union of the recorded fog of whoever is selected
    private static void updateFog() {
        Set<String> views = new HashSet<>();
        for (String name : header.recipients)
            if (FogOfWarClientEvents.isViewing(name))
                views.add(name);
        if (views.isEmpty() || (!fogDirty && views.equals(fogViews))) {
            fogViews = views;
            return;
        }
        fogViews = views;
        fogDirty = false;
        Set<ChunkPos> bright = new HashSet<>();
        Set<ChunkPos> live = new HashSet<>();
        Map<ChunkPos, long[]> masks = new HashMap<>();
        for (String name : views) {
            FogChunksClientboundPacket f = fog.get(name);
            if (f == null)
                continue;
            bright.addAll(f.bright);
            for (ChunkPos cp : f.bright)
                if (!f.edgeMasks.containsKey(cp))
                    live.add(cp);
            f.edgeMasks.forEach((cp, m) -> masks.merge(cp, m.clone(), (a, b) -> {
                for (int i = 0; i < a.length; i++)
                    a[i] |= b[i];
                return a;
            }));
        }
        masks.keySet().removeAll(live);
        FogOfWarClientEvents.applyServerFogState(bright, masks);
    }

    // cameras arrive every few ticks: glide between them
    private static void updateCameras() {
        cameraTargets.forEach((name, target) -> {
            float[] cam = cameras.computeIfAbsent(name, n -> target.clone());
            for (int i = 0; i < 3; i++)
                cam[i] += (target[i] - cam[i]) * 0.3f;
            cam[3] += Mth.wrapDegrees(target[3] - cam[3]) * 0.3f;
        });
    }

    // ------------------------------------------------------------------ recording (live matches)

    private static int cameraTicks = 0;

    private static void sendCamera() {
        if (header != null || MC.player == null || MC.getConnection() == null || !OrthoviewClientEvents.isEnabled()
                || PlayerClientEvents.rtsPlayers.isEmpty() || ++cameraTicks % CAMERA_SEND_TICKS != 0)
            return;
        ReplayCameraServerboundPacket.send((float) MC.player.getX(), (float) MC.player.getZ(),
                OrthoviewClientEvents.getZoom(), OrthoviewClientEvents.getCamRotX());
    }

    // ------------------------------------------------------------------ downloads (end screen)

    public enum Download { NONE, WAITING, DOWNLOADING, DONE, FAILED }

    private static Download download = Download.NONE;
    private static float downloadProgress = 0;
    @Nullable private static Path downloadPart = null;
    private static long downloadReceived = 0;

    public static Download download() {
        return download;
    }

    public static float downloadProgress() {
        return downloadProgress;
    }

    public static void resetDownload() {
        download = Download.NONE;
        downloadProgress = 0;
        downloadPart = null;
        downloadReceived = 0;
    }

    public static void openReplaysFolder() {
        try {
            Files.createDirectories(ReplayHeader.dir());
        } catch (IOException ignored) { }
        Util.getPlatform().openFile(ReplayHeader.dir().toFile());
    }

    public static void requestDownload() {
        // our own server (singleplayer or hosting) already saved it into this game's replays folder
        if (MC.hasSingleplayerServer()) {
            download = Download.DONE;
            openReplaysFolder();
            return;
        }
        if (download == Download.WAITING || download == Download.DOWNLOADING)
            return;
        download = Download.WAITING;
        ReplayDownloadServerboundPacket.request();
    }

    public static void onDownloadChunk(String fileName, long size, long offset, byte[] data) {
        if (download != Download.WAITING && download != Download.DOWNLOADING)
            return;
        String clean = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        if (size <= 0 || size > MAX_DOWNLOAD_BYTES || !clean.endsWith(ReplayHeader.EXTENSION) || clean.startsWith(".")) {
            download = Download.FAILED;
            return;
        }
        try {
            if (offset == 0 && download == Download.WAITING) { // only one download per request
                Files.createDirectories(ReplayHeader.dir());
                downloadPart = ReplayHeader.dir().resolve(clean + ".part");
                Files.deleteIfExists(downloadPart);
                downloadReceived = 0;
                download = Download.DOWNLOADING;
            }
            if (downloadPart == null || offset != downloadReceived)
                throw new IOException("replay download out of order");
            Files.write(downloadPart, data, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            downloadReceived += data.length;
            downloadProgress = (float) downloadReceived / size;
            if (downloadReceived >= size) {
                // the name comes from the server: never let it replace a replay we already have
                String stem = clean.substring(0, clean.length() - ReplayHeader.EXTENSION.length());
                Path target = ReplayHeader.dir().resolve(clean);
                for (int i = 2; Files.exists(target); i++)
                    target = ReplayHeader.dir().resolve(stem + "_" + i + ReplayHeader.EXTENSION);
                Files.move(downloadPart, target);
                download = Download.DONE;
            }
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("[Replay] download failed", e);
            download = Download.FAILED;
        }
    }
}
