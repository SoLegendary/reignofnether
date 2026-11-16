package com.solegendary.reignofnether.worldselect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SavedGamesTest {

    @Test
    void clonesTheMapAndLeavesItUntouched(@TempDir Path saves) throws IOException {
        Path map = Files.createDirectories(saves.resolve("[2p] Oceanborn/region"));
        Files.writeString(saves.resolve("[2p] Oceanborn/level.dat"), "level");
        Files.writeString(map.resolve("r.0.0.mca"), "chunks");
        Files.writeString(saves.resolve("[2p] Oceanborn/session.lock"), "lock");

        long time = 1_700_000_000_000L;
        String first = SavedGames.cloneMap(saves, "[2p] Oceanborn", "Oceanborn", time);
        String second = SavedGames.cloneMap(saves, "[2p] Oceanborn", "Oceanborn", time);

        assertNotEquals(first, second);
        assertFalse(first.contains(":"));
        Path save = saves.resolve(first);
        assertEquals("chunks", Files.readString(save.resolve("region/r.0.0.mca")));
        assertFalse(Files.exists(save.resolve("session.lock")));
        SavedGames.Info info = SavedGames.read(save);
        assertNotNull(info);
        assertEquals("[2p] Oceanborn", info.map());
        assertEquals("Oceanborn", info.mapName());
        assertNull(SavedGames.read(saves.resolve("[2p] Oceanborn")));
        assertTrue(Files.exists(saves.resolve("[2p] Oceanborn/session.lock")));
    }
}
