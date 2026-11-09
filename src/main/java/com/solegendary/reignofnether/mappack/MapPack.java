package com.solegendary.reignofnether.mappack;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads the official map pack and adds its worlds to the singleplayer list.
 * The website redirects this address to the current pack, so a new pack needs no mod update.
 * Downloading again is the update check: maps whose files changed since they were installed are replaced.
 */
public final class MapPack {

    private MapPack() { }

    public static final String URL = "https://reignofnether.net/download/mappack";
    private static final String MARKER = ".ron-mappack";
    // the pack is ~250 MB zipped; these only stop a broken or hostile download from filling the disk
    static final long MAX_DOWNLOAD = 1L << 30, MAX_EXTRACTED = 4L << 30;
    static final int MAX_ENTRIES = 100_000;

    /** The world folders map pack downloads installed (empty when it was never downloaded here). */
    public static java.util.Set<String> installedWorlds() {
        return readMarker(saves()).keySet();
    }

    /** What the last download changed: maps added and maps replaced by a newer version. */
    record Result(int added, int updated) { }

    /** World folder -> version of the pack map in it, one "name<tab>version" line each (older installs have no version). */
    static Map<String, String> readMarker(Path saves) {
        Map<String, String> worlds = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(saves.resolve(MARKER))) {
                int tab = line.indexOf('\t');
                if (!line.isBlank())
                    worlds.put(tab < 0 ? line : line.substring(0, tab), tab < 0 ? "" : line.substring(tab + 1));
            }
        } catch (Exception ignored) { }
        return worlds;
    }

    static void writeMarker(Path saves, Map<String, String> worlds) throws IOException {
        Files.write(saves.resolve(MARKER), worlds.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue()).toList());
    }

    public enum State { MISSING, DOWNLOADING, INSTALLED, FAILED }

    private static volatile State state;
    private static volatile float progress; // 0..1 while downloading, -1 while installing
    private static volatile String error = "";
    private static volatile Result result;

    public static State state() {
        if (state == null) {
            // ponytail: only an in-game install is detected, worlds copied in by hand show as missing;
            // match save folder names against the pack's if that confuses players
            Path marker = saves().resolve(MARKER);
            state = Files.exists(marker) ? State.INSTALLED : State.MISSING;
        }
        return state;
    }

    public static float progress() { return progress; }
    public static String error() { return error; }
    /** Added and replaced map counts from this launch's last download, or null. */
    public static int[] result() { return result == null ? null : new int[] { result.added(), result.updated() }; }

    private static Path saves() {
        return Minecraft.getInstance().getLevelSource().getBaseDir();
    }

    // ponytail: checking downloads the whole pack (~250 MB); compare the response's ETag first if that annoys players
    public static void download() {
        if (state() == State.DOWNLOADING)
            return;
        state = State.DOWNLOADING;
        progress = 0;
        Path saves = saves();
        Path part = Minecraft.getInstance().gameDirectory.toPath().resolve("ron-mappack.zip.part");
        Util.backgroundExecutor().execute(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(15)).build();
                HttpResponse<InputStream> res = client.send(HttpRequest.newBuilder(URI.create(URL)).build(),
                        HttpResponse.BodyHandlers.ofInputStream());
                if (res.statusCode() != 200)
                    throw new IOException("Server answered " + res.statusCode());
                long total = res.headers().firstValueAsLong("Content-Length").orElse(-1);
                try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(part)) {
                    byte[] buf = new byte[64 * 1024];
                    long done = 0;
                    for (int n; (n = in.read(buf)) > 0; ) {
                        out.write(buf, 0, n);
                        done += n;
                        if (done > MAX_DOWNLOAD)
                            throw new IOException("Map pack download is too big");
                        if (total > 0)
                            progress = (float) done / total;
                    }
                }
                progress = -1;
                Map<String, String> worlds = readMarker(saves);
                Result r = extract(part, saves, worlds);
                writeMarker(saves, worlds);
                result = r;
                state = State.INSTALLED;
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                state = State.FAILED;
            } finally {
                try {
                    Files.deleteIfExists(part);
                } catch (IOException ignored) { }
            }
        });
    }

    /**
     * Copies every world in the zip (a folder holding a level.dat) into saves, under its folder name.
     * A pack map installed before (in {@code installed}) is replaced when its files changed. A player's own world
     * is never overwritten: a pack map with its name goes next to it as "name (map pack)".
     * Updates {@code installed} with the version of every world it wrote.
     */
    static Result extract(Path zip, Path saves, Map<String, String> installed) throws IOException {
        Path base = saves.toAbsolutePath().normalize();
        int added = 0, updated = 0;
        try (ZipFile file = new ZipFile(zip.toFile())) {
            if (file.size() > MAX_ENTRIES)
                throw new IOException("Too many files in map pack");
            long[] budget = { MAX_EXTRACTED };
            List<String> roots = new ArrayList<>();
            for (ZipEntry e : Collections.list(file.entries())) {
                String name = e.getName();
                if (name.equals("level.dat") || name.endsWith("/level.dat"))
                    roots.add(name.substring(0, name.length() - "level.dat".length()));
            }
            // a level.dat further down a world (a backup folder, say) is part of that world
            roots.removeIf(r -> roots.stream().anyMatch(o -> !o.equals(r) && r.startsWith(o)));

            for (String root : roots) {
                String world = root.isEmpty() ? "Map pack" : Path.of(root).getFileName().toString();
                if (Files.exists(base.resolve(world)) && !installed.containsKey(world))
                    world += " (map pack)";
                Path dest = base.resolve(world).normalize();
                if (!base.equals(dest.getParent()))
                    throw new IOException("Bad world name in map pack: " + world);
                String version = version(file, root);
                boolean exists = Files.exists(dest);
                if (exists && (!installed.containsKey(world) || version.equals(installed.get(world))))
                    continue;
                // unpacked next to it and moved in at the end, so a failed install leaves no half world behind
                Path tmp = base.resolve(".ron-mappack-tmp");
                deleteTree(tmp);
                try {
                    for (ZipEntry e : Collections.list(file.entries())) {
                        if (!e.getName().startsWith(root))
                            continue;
                        Path target = tmp.resolve(e.getName().substring(root.length())).normalize();
                        if (!target.startsWith(tmp))
                            throw new IOException("Bad path in map pack: " + e.getName());
                        if (e.isDirectory()) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            try (InputStream in = file.getInputStream(e); OutputStream out = Files.newOutputStream(target)) {
                                copy(in, out, budget);
                            }
                        }
                    }
                    if (exists)
                        replace(base, dest, tmp);
                    else
                        Files.move(tmp, dest);
                } finally {
                    deleteTree(tmp);
                }
                installed.put(world, version);
                if (exists)
                    updated++;
                else
                    added++;
            }
        }
        return new Result(added, updated);
    }

    /** Swaps the new world in, putting the old one back if that fails. */
    private static void replace(Path base, Path dest, Path tmp) throws IOException {
        Path old = base.resolve(".ron-mappack-old");
        deleteTree(old);
        Files.move(dest, old);
        try {
            Files.move(tmp, dest);
        } catch (IOException e) {
            Files.move(old, dest);
            throw e;
        }
        deleteTree(old);
    }

    /** A checksum of a world's file names, sizes and CRCs: changes whenever the pack ships a different version of it. */
    private static String version(ZipFile file, String root) {
        CRC32 crc = new CRC32();
        file.stream().filter(e -> !e.isDirectory() && e.getName().startsWith(root))
                .sorted(Comparator.comparing(ZipEntry::getName))
                .forEach(e -> crc.update((e.getName().substring(root.length()) + ":" + e.getSize() + ":" + e.getCrc() + "\n").getBytes()));
        return Long.toHexString(crc.getValue());
    }

    private static void copy(InputStream in, OutputStream out, long[] budget) throws IOException {
        byte[] buf = new byte[64 * 1024];
        for (int n; (n = in.read(buf)) > 0; ) {
            if ((budget[0] -= n) < 0)
                throw new IOException("Map pack is too big");
            out.write(buf, 0, n);
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir))
            return;
        try (var walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList())
                Files.delete(p);
        }
    }
}
