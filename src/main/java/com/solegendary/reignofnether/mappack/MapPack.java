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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads the official map pack and adds its worlds to the singleplayer list.
 * The website redirects this address to the current pack, so a new pack needs no mod update.
 */
public final class MapPack {

    private MapPack() { }

    public static final String URL = "https://reignofnether.net/download/mappack";
    private static final String MARKER = ".ron-mappack";
    // the pack is ~250 MB zipped; these only stop a broken or hostile download from filling the disk
    static final long MAX_DOWNLOAD = 1L << 30, MAX_EXTRACTED = 4L << 30;
    static final int MAX_ENTRIES = 100_000;

    /** The world folders the last map pack download installed (empty when it was never downloaded here). */
    public static java.util.Set<String> installedWorlds() {
        try {
            return new java.util.HashSet<>(Files.readAllLines(saves().resolve(MARKER)));
        } catch (Exception e) {
            return java.util.Set.of();
        }
    }

    public enum State { MISSING, DOWNLOADING, INSTALLED, FAILED }

    private static volatile State state;
    private static volatile float progress; // 0..1 while downloading, -1 while installing
    private static volatile String error = "";

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

    private static Path saves() {
        return Minecraft.getInstance().getLevelSource().getBaseDir();
    }

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
                List<String> worlds = extract(part, saves);
                Files.write(saves.resolve(MARKER), worlds);
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
     * A world whose name is already taken is skipped, so a player's own world is never overwritten.
     * Returns the names of the worlds it installed.
     */
    static List<String> extract(Path zip, Path saves) throws IOException {
        Path base = saves.toAbsolutePath().normalize();
        List<String> installed = new ArrayList<>();
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
                Path dest = base.resolve(world).normalize();
                if (!base.equals(dest.getParent()))
                    throw new IOException("Bad world name in map pack: " + world);
                if (Files.exists(dest))
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
                    Files.move(tmp, dest);
                } finally {
                    deleteTree(tmp);
                }
                installed.add(world);
            }
        }
        return installed;
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
