package com.solegendary.reignofnether.mappack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MapPackTest {

    private static Path zip(Path dir, String... entries) throws IOException {
        return zipWith(dir, "", entries);
    }

    /** Each entry holds its own name plus {@code content}, so a different content is a new version of the pack. */
    private static Path zipWith(Path dir, String content, String... entries) throws IOException {
        Path zip = dir.resolve("pack.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String e : entries) {
                out.putNextEntry(new ZipEntry(e));
                out.write((e + content).getBytes());
                out.closeEntry();
            }
        }
        return zip;
    }

    @Test
    void installsNewWorldsAndKeepsExistingOnes(@TempDir Path dir) throws IOException {
        Path saves = Files.createDirectories(dir.resolve("saves"));
        Files.createDirectories(saves.resolve("Mine"));
        Files.writeString(saves.resolve("Mine/level.dat"), "mine");
        Path zip = zip(dir,
                "Pack/[2] Oceanborn/level.dat", "Pack/[2] Oceanborn/region/r.0.0.mca",
                "Pack/[2] Oceanborn/backup/level.dat",
                "Pack/Mine/level.dat", "Pack/readme.txt");

        Map<String, String> installed = new HashMap<>();
        assertEquals(new MapPack.Result(2, 0), MapPack.extract(zip, saves, installed));
        assertEquals(java.util.Set.of("[2] Oceanborn", "Mine (map pack)"), installed.keySet());
        assertTrue(Files.exists(saves.resolve("[2] Oceanborn/region/r.0.0.mca")));
        assertTrue(Files.exists(saves.resolve("[2] Oceanborn/backup/level.dat")));
        assertEquals("mine", Files.readString(saves.resolve("Mine/level.dat")));
        assertTrue(Files.exists(saves.resolve("Mine (map pack)/level.dat")));
        assertFalse(Files.exists(saves.resolve("readme.txt")));
    }

    @Test
    void updatesOnlyChangedPackMaps(@TempDir Path dir) throws IOException {
        Path saves = Files.createDirectories(dir.resolve("saves"));
        Map<String, String> installed = new HashMap<>();
        MapPack.extract(zip(dir, "A/level.dat", "B/level.dat"), saves, installed);
        Files.writeString(saves.resolve("B/played.txt"), "x");

        assertEquals(new MapPack.Result(0, 0), MapPack.extract(zip(dir, "A/level.dat", "B/level.dat"), saves, installed));
        assertTrue(Files.exists(saves.resolve("B/played.txt")));

        // new pack: A is the same, B changed, C is new
        Path v2 = zipWith(dir, "v2", "B/level.dat", "C/level.dat");
        assertEquals(new MapPack.Result(1, 1), MapPack.extract(v2, saves, installed));
        assertEquals("B/level.datv2", Files.readString(saves.resolve("B/level.dat")));
        assertFalse(Files.exists(saves.resolve("B/played.txt")));
        assertTrue(Files.exists(saves.resolve("C/level.dat")));
        assertFalse(Files.exists(saves.resolve(".ron-mappack-old")));
        Path a = zip(dir, "A/level.dat");
        assertEquals(new MapPack.Result(0, 0), MapPack.extract(a, saves, installed));

        // installs from before versions were stored count as outdated
        installed.put("A", "");
        assertEquals(new MapPack.Result(0, 1), MapPack.extract(a, saves, installed));
    }

    @Test
    void markerRoundTripsAndReadsOldFormat(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve(".ron-mappack"), "Old map\n");
        assertEquals(Map.of("Old map", ""), MapPack.readMarker(dir));
        MapPack.writeMarker(dir, Map.of("New map", "abc"));
        assertEquals(Map.of("New map", "abc"), MapPack.readMarker(dir));
    }

    @Test
    void rejectsPathsLeavingTheWorld(@TempDir Path dir) throws IOException {
        Path saves = Files.createDirectories(dir.resolve("saves"));
        Path zip = zip(dir, "Evil/level.dat", "Evil/../../escaped.txt");

        assertThrows(IOException.class, () -> MapPack.extract(zip, saves, new HashMap<>()));
        assertFalse(Files.exists(dir.resolve("escaped.txt")));
        // a failed install leaves no half world to be skipped next time
        try (var files = Files.list(saves)) {
            assertEquals(0, files.count());
        }
    }
}
