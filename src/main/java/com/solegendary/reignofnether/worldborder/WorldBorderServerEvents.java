package com.solegendary.reignofnether.worldborder;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.pathfinding.PathfinderConfig;
import com.solegendary.reignofnether.unit.pathfinding.WalkabilityGrid;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldBorderServerEvents {

    // A world border this small (blocks) means the map was purpose-built for RoN RTS: it's "RTS-optimised".
    // 1024 = 64x64 chunks. We never change the border - we only read it as the signal to switch the map into
    // RTS-optimised mode (improved pathfinding + navmesh precompute). A vanilla map's border is ~60M wide, so
    // it stays on vanilla pathfinding untouched. The whole bounded play area fits in the walkability cache
    // (see PathfinderConfig.MAX_CACHED_CHUNKS), which is what makes the full-map precompute worthwhile.
    public static final int RTS_OPTIMIZED_BORDER = 1024;

    public static boolean prewarmedNavmesh = false;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        MinecraftServer server = evt.getServer();
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null)
            return;

        WorldBorder border = level.getWorldBorder();
        // A small world border marks an RTS-optimised map; a vanilla-sized border is left fully untouched.
        if (border.getSize() > RTS_OPTIMIZED_BORDER)
            return;

        ReignOfNether.LOGGER.info(
                "RTS-optimised map detected (world border = {} blocks) - enabling improved pathfinding + navmesh precompute",
                (int) border.getSize());

        // Turn the rtsPathfinding gamerule on for this map (the gamerule's own default stays off, so
        // vanilla maps are unaffected) and mirror it to the server flag + any clients, reusing the existing
        // gamerule plumbing. At server-start there are no clients yet; player-join sync handles late joiners.
        server.getGameRules().getRule(GameRuleRegistrar.RTS_PATHFINDING).set(true, server);
        UnitServerEvents.rtsPathfinding = true;
        GameruleClientboundPacket.setRtsPathfinding(true);

        prewarmNavmesh(level, border);
    }

    // Force-load and classify every chunk inside the world border so the navmesh is fully warm before play.
    // Synchronous by design: this is the load-time "prepare the world" phase, paid up front and bounded by the
    // (small) RTS-optimised border. The cache is sized to hold all of it, so the prewarm never evicts itself.
    private static void prewarmNavmesh(ServerLevel level, WorldBorder border) {
        WalkabilityGrid grid = WalkabilityGrid.get(level);

        int cxMin = (int) Math.floor((border.getCenterX() - border.getSize() / 2.0) / 16.0);
        int cxMax = (int) Math.floor((border.getCenterX() + border.getSize() / 2.0) / 16.0);
        int czMin = (int) Math.floor((border.getCenterZ() - border.getSize() / 2.0) / 16.0);
        int czMax = (int) Math.floor((border.getCenterZ() + border.getSize() / 2.0) / 16.0);

        int total = (cxMax - cxMin + 1) * (czMax - czMin + 1);
        ReignOfNether.LOGGER.info("Prewarming RTS navmesh: {} chunks within world border ({}..{}, {}..{})...",
                total, cxMin, cxMax, czMin, czMax);

        long startMs = System.currentTimeMillis();
        int done = 0;
        int nextProgress = 10; // log at ~every 10%

        for (int cx = cxMin; cx <= cxMax; cx++) {
            for (int cz = czMin; cz <= czMax; cz++) {
                try {
                    // Force-load/generate the chunk so its block states are readable, then classify a
                    // surface-aligned Y band around its terrain height.
                    ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, true);
                    int[] band = surfaceBand(chunk);
                    grid.getOrBuild(level, cx, cz, band[0], band[1]);
                } catch (Throwable t) {
                    ReignOfNether.LOGGER.error("Navmesh prewarm failed for chunk ({}, {})", cx, cz, t);
                }
                done++;
                int pct = (int) (100L * done / total);
                if (pct >= nextProgress) {
                    ReignOfNether.LOGGER.info("Prewarming RTS navmesh: {}% ({}/{} chunks)", pct, done, total);
                    nextProgress = pct - (pct % 10) + 10;
                }
            }
        }

        ReignOfNether.LOGGER.info("Prewarmed RTS navmesh: {} chunks in {} ms",
                total, System.currentTimeMillis() - startMs);

        prewarmedNavmesh = true;
    }

    // Y band to classify for a chunk: span the chunk's surface heights (sampled at its corners + centre) and
    // pad to the pathfinder's vertical window so a unit pathing across this chunk has headroom above/below.
    private static int[] surfaceBand(ChunkAccess chunk) {
        int[] xs = {0, 15, 0, 15, 8};
        int[] zs = {0, 0, 15, 15, 8};
        int minSurf = Integer.MAX_VALUE;
        int maxSurf = Integer.MIN_VALUE;
        for (int i = 0; i < xs.length; i++) {
            int h = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, xs[i], zs[i]);
            if (h < minSurf) minSurf = h;
            if (h > maxSurf) maxSurf = h;
        }
        int minY = minSurf - PathfinderConfig.VERTICAL_WINDOW_SLACK;
        int maxY = maxSurf + PathfinderConfig.VERTICAL_RADIUS + PathfinderConfig.VERTICAL_WINDOW_SLACK;
        return new int[]{minY, maxY};
    }
}
