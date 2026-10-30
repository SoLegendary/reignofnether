package com.solegendary.reignofnether.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.simple.IndexedMessageCodec;
import net.minecraftforge.network.simple.SimpleChannel;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * header.json of a .ronreplay zip (the other entry is packets.bin, see ReplayRecorder). Written when the match
 * ends, read by the replay browser and by playback.
 */
public class ReplayHeader {

    public static final int FORMAT = 1;
    public static final String EXTENSION = ".ronreplay";
    public static final String HEADER_ENTRY = "header.json";
    public static final String PACKETS_ENTRY = "packets.bin";
    public static final String REGISTRIES_ENTRY = "registries.nbt"; // the recorded world's ids, as Forge saves them in level.dat
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class PlayerRow {
        public String name;
        public String faction; // Faction.key
        public int team;       // startPosColorId, like the end screen
        public boolean winner;
        public boolean bot;
        public int[] scores;   // RTSPlayerScoresEnum order
    }

    public static class Marker {
        public int tick;
        public String type; // "defeat" or "end"
        public String name;

        public Marker(int tick, String type, String name) {
            this.tick = tick;
            this.type = type;
            this.name = name;
        }
    }

    public int format = FORMAT;
    public String modVersion = modVersion();
    public String map = "";
    public long date;
    public int durationTicks;
    public List<PlayerRow> players = new ArrayList<>();
    public List<Marker> markers = new ArrayList<>();
    // recipient bit -> player name (bit 63 is the start snapshot)
    public List<String> recipients = new ArrayList<>();
    // the mod channel's discriminator -> packet class name, so playback can remap if packets were added/removed
    public Map<Integer, String> packetIds = new HashMap<>();
    public int minBuildHeight, buildHeight;
    public int maxEntityId;
    public int minChunkX, minChunkZ, maxChunkX, maxChunkZ;

    public transient Path file;
    public transient long fileSize;

    public static String modVersion() {
        return ModList.get().getModContainerById(ReignOfNether.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString()).orElse("dev");
    }

    /** The mod channel's discriminator -> packet class name, read from Forge's codec (it keeps no public view of it). */
    @SuppressWarnings("unchecked")
    public static Map<Integer, String> currentPacketIds() {
        Map<Integer, String> ids = new HashMap<>();
        try {
            Field codecField = SimpleChannel.class.getDeclaredField("indexedCodec");
            codecField.setAccessible(true);
            Object codec = codecField.get(PacketHandler.INSTANCE);
            Field typesField = IndexedMessageCodec.class.getDeclaredField("types");
            typesField.setAccessible(true);
            for (Map.Entry<Class<?>, Object> e : ((Map<Class<?>, Object>) typesField.get(codec)).entrySet()) {
                Field index = e.getValue().getClass().getDeclaredField("index");
                index.setAccessible(true);
                ids.put(index.getInt(e.getValue()) & 0xff, e.getKey().getName());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ReignOfNether.LOGGER.warn("[Replay] could not read the packet ids, playback will assume they match", e);
        }
        return ids;
    }

    public boolean sameVersion() {
        return modVersion().equals(modVersion);
    }

    public static Path dir() {
        return FMLPaths.GAMEDIR.get().resolve("reignofnether").resolve("replays");
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ReplayHeader read(Path file) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry(HEADER_ENTRY);
            if (entry == null || zip.getEntry(PACKETS_ENTRY) == null)
                throw new IOException("not a replay: " + file.getFileName());
            try (Reader r = new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                ReplayHeader h = GSON.fromJson(r, ReplayHeader.class);
                if (h == null)
                    throw new IOException("empty header: " + file.getFileName());
                // a shared file can hold anything
                if (h.players == null) h.players = new ArrayList<>();
                if (h.markers == null) h.markers = new ArrayList<>();
                if (h.recipients == null) h.recipients = new ArrayList<>();
                if (h.packetIds == null) h.packetIds = new HashMap<>();
                h.file = file;
                h.fileSize = Files.size(file);
                return h;
            }
        }
    }
}
