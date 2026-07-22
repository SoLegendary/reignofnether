package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Pre-game chunk packets inside the world border; served to dark players by ChunkMapInitialSendMixin.
// ~50-500 KB per chunk - fine for <=512x512 borders, not for 2k+.
public class FogChunkSnapshot {

    private static final Map<ChunkPos, ClientboundLevelChunkWithLightPacket> snapshots = new ConcurrentHashMap<>();

    public static void captureWorldBorder(ServerLevel level) {
        snapshots.clear();

        WorldBorder border = level.getWorldBorder();
        int minChunkX = (int) Math.floor(border.getMinX()) >> 4;
        int maxChunkX = (int) Math.floor(border.getMaxX() - 1) >> 4;
        int minChunkZ = (int) Math.floor(border.getMinZ()) >> 4;
        int maxChunkZ = (int) Math.floor(border.getMaxZ() - 1) >> 4;

        int captured = 0;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, true);
                if (chunk == null) continue;
                ChunkPos pos = new ChunkPos(cx, cz);
                snapshots.put(pos, new ClientboundLevelChunkWithLightPacket(
                        chunk, level.getLightEngine(), null, null));
                captured++;
            }
        }
        ReignOfNether.LOGGER.info("[FogChunkSnapshot] captured {} chunks inside world border", captured);
    }

    public static ClientboundLevelChunkWithLightPacket get(ChunkPos pos) {
        return snapshots.get(pos);
    }

    public static boolean hasAny() {
        return !snapshots.isEmpty();
    }

    public static void clear() {
        if (!snapshots.isEmpty())
            ReignOfNether.LOGGER.info("[FogChunkSnapshot] clearing {} chunks", snapshots.size());
        snapshots.clear();
    }
}
