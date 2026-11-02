package com.solegendary.reignofnether.mappack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MapPackTest {

    private static Path zip(Path dir, String... entries) throws IOException {
        Path zip = dir.resolve("pack.zip");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String e : entries) {
                out.putNextEntry(new ZipEntry(e));
                out.write(e.getBytes());
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

        assertEquals(List.of("[2] Oceanborn"), MapPack.extract(zip, saves));
        assertTrue(Files.exists(saves.resolve("[2] Oceanborn/region/r.0.0.mca")));
        assertTrue(Files.exists(saves.resolve("[2] Oceanborn/backup/level.dat")));
        assertEquals("mine", Files.readString(saves.resolve("Mine/level.dat")));
        assertFalse(Files.exists(saves.resolve("readme.txt")));
    }

    @Test
    void rejectsPathsLeavingTheWorld(@TempDir Path dir) throws IOException {
        Path saves = Files.createDirectories(dir.resolve("saves"));
        Path zip = zip(dir, "Evil/level.dat", "Evil/../../escaped.txt");

        assertThrows(IOException.class, () -> MapPack.extract(zip, saves));
        assertFalse(Files.exists(dir.resolve("escaped.txt")));
        // a failed install leaves no half world to be skipped next time
        try (var files = Files.list(saves)) {
            assertEquals(0, files.count());
        }
    }
}
