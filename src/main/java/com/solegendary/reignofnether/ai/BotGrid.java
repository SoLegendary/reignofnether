package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.resources.ResourceIndex;
import com.solegendary.reignofnether.resources.ResourceName;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

// What the supervisor net sees: the map as 32x32 cells, 18 layers each (RoN-Bots/FORMAT.md). Terrain and resources
// are redrawn once a minute, units, buildings and fog every decision. Values are 0..1, kept as bytes the way the
// trainer reads them, so the net sees the same numbers in game and in training.
class BotGrid {

    static final int N = 32, CELLS = N * N, LAYERS = 18, STATIC = 6;
    static final int WATER = 0, HEIGHT = 1, CLIFF = 2, WOOD = 3, ORE = 4, FOOD = 5, FOG = 6, MAIN = 7, OTHER = 8,
            HEROES = 9, WORKERS = 10, BUILDINGS = 11, ALLY_UNITS = 12, ALLY_BUILDINGS = 13, ENEMY_ARMY = 14,
            ENEMY_BUILDINGS = 15, CAMPS = 16, SCOUTS = 17;
    private static final int MAX_BLOCKS = 1024, SAMPLES = 4;
    private static final long STATIC_EVERY = 60 * 20, FOG_TICKS = 3600, ENEMY_AGE = 3600;
    private static final int BUILD_RANGE = 160; // a building goes this near one of ours at most: the workers walk there

    private final BotBrain bot;
    double minX, minZ, size;
    final byte[] layers = new byte[LAYERS * CELLS];
    final boolean[] reach = new boolean[CELLS], buildable = new boolean[CELLS];
    private final long[] seenTick = new long[CELLS];
    private final int[] surface = new int[CELLS];
    private long staticTick = -1_000_000;
    boolean staticChanged = false;

    BotGrid(BotBrain bot) {
        this.bot = bot;
        java.util.Arrays.fill(seenTick, -1);
    }

    // the square the grid covers: the world border when it's map-sized, else round the starts (same as the map image)
    static double[] bounds(ServerLevel level, List<BlockPos> starts) {
        var border = level.getWorldBorder();
        double cx = starts.stream().mapToInt(BlockPos::getX).average().orElse(0), cz = starts.stream().mapToInt(BlockPos::getZ).average().orElse(0);
        double spread = starts.stream().mapToDouble(p -> Math.max(Math.abs(p.getX() - cx), Math.abs(p.getZ() - cz))).max().orElse(64);
        boolean bordered = border.getSize() <= MAX_BLOCKS;
        double size = bordered ? border.getSize() : Math.min(MAX_BLOCKS, Math.max(256, 2 * spread + 160));
        return new double[] {bordered ? border.getMinX() : cx - size / 2, bordered ? border.getMinZ() : cz - size / 2, size};
    }

    int cell(BlockPos p) {
        int cx = (int) Math.floor((p.getX() - minX) * N / size), cz = (int) Math.floor((p.getZ() - minZ) * N / size);
        return cx < 0 || cz < 0 || cx >= N || cz >= N ? -1 : cz * N + cx;
    }

    BlockPos pos(int cell) {
        int x = (int) (minX + (cell % N + 0.5) * size / N), z = (int) (minZ + (cell / N + 0.5) * size / N);
        return new BlockPos(x, surface[cell] > 0 ? surface[cell] : bot.home.getY(), z);
    }

    double cellBlocks() {
        return size / N;
    }

    float get(int layer, int cell) {
        return (layers[layer * CELLS + cell] & 255) / 255f;
    }

    // the net's input: every layer as 0..1
    float[] input() {
        float[] f = new float[layers.length];
        for (int i = 0; i < f.length; i++)
            f[i] = (layers[i] & 255) / 255f;
        return f;
    }

    // the share of the map our team has ever seen
    double explored() {
        int n = 0;
        for (long t : seenTick)
            if (t >= 0)
                n++;
        return n / (double) CELLS;
    }

    void update() {
        if (size == 0) {
            List<BlockPos> starts = new java.util.ArrayList<>(bot.memory.enemyStarts());
            starts.add(bot.home);
            for (BuildingPlacement b : BuildingServerEvents.getBuildings())
                if (b.isCapitol && AlliancesServerEvents.isAllied(bot.name, b.ownerName))
                    starts.add(b.centrePos);
            double[] b = bounds(bot.level, starts);
            minX = b[0];
            minZ = b[1];
            size = b[2];
        }
        staticChanged = bot.tick - staticTick >= STATIC_EVERY;
        if (staticChanged) {
            staticTick = bot.tick;
            terrain();
        }
        java.util.Arrays.fill(layers, STATIC * CELLS, layers.length, (byte) 0);
        float[] add = new float[(LAYERS - STATIC) * CELLS];
        for (int c = 0; c < CELLS; c++) {
            if (bot.memory.isSeen(pos(c)))
                seenTick[c] = bot.tick;
            add[(FOG - STATIC) * CELLS + c] = seenTick[c] < 0 ? 1 : Math.min(1, (bot.tick - seenTick[c]) / (float) FOG_TICKS);
        }
        List<LivingEntity> main = bot.army.main();
        for (LivingEntity e : bot.fighters)
            put(add, BotBrain.isHero(e) ? HEROES : main.contains(e) ? MAIN : OTHER, e.blockPosition(), BotBrain.isHero(e) ? 0.5f : pop(e) / 20f);
        for (LivingEntity e : bot.workers)
            put(add, WORKERS, e.blockPosition(), 0.1f);
        for (LivingEntity e : bot.scouts)
            put(add, SCOUTS, e.blockPosition(), 0.5f);
        for (LivingEntity e : bot.memory.allyArmy())
            put(add, ALLY_UNITS, e.blockPosition(), pop(e) / 20f);
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (!b.isDestroyedServerside && (b.ownerName.equals(bot.name) || AlliancesServerEvents.isAllied(bot.name, b.ownerName)))
                put(add, b.ownerName.equals(bot.name) ? BUILDINGS : ALLY_BUILDINGS, b.centrePos, 1 / 3f);
        for (LivingEntity e : bot.memory.recentEnemyArmy())
            put(add, ENEMY_ARMY, bot.memory.lastSeenPos(e), pop(e) / 20f * Math.max(0, 1 - (bot.tick - bot.memory.lastSeenTick(e)) / (float) ENEMY_AGE));
        for (BotMemory.SeenBuilding sb : bot.memory.enemyBuildings.values())
            put(add, ENEMY_BUILDINGS, sb.pos(), 1 / 3f);
        for (BotMemory.Camp camp : bot.memory.camps.values())
            put(add, CAMPS, camp.pos(), BotMath.popOf(camp.units()) / 10f);
        // a free capture point (a map's trade hub) is neutral too: an army standing by it takes it and its income
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (b.getBuilding().capturable && b.ownerName.isEmpty() && !b.isDestroyedServerside)
                put(add, CAMPS, b.centrePos, 1 / 3f);
        for (int i = 0; i < add.length; i++)
            layers[STATIC * CELLS + i] = quantise(add[i]);
        masks();
    }

    private static int pop(LivingEntity e) {
        return BotMath.popOf(List.of(e));
    }

    private void put(float[] add, int layer, BlockPos p, float v) {
        int c = cell(p);
        if (c >= 0)
            add[(layer - STATIC) * CELLS + c] += v;
    }

    private static byte quantise(float v) {
        return (byte) Math.round(Math.max(0, Math.min(1, v)) * 255);
    }

    // ponytail: 4x4 samples per cell, enough to tell a lake from a pond; sample every block if rivers get missed
    private void terrain() {
        ServerLevel level = bot.level;
        ResourceIndex index = ResourceIndex.get(level);
        int radius = (int) Math.ceil(cellBlocks() / 2);
        for (int c = 0; c < CELLS; c++) {
            BlockPos centre = pos(c);
            if (!level.hasChunkAt(centre))
                continue; // keeps what it saw last
            int water = 0, height = 0;
            for (int sx = 0; sx < SAMPLES; sx++)
                for (int sz = 0; sz < SAMPLES; sz++) {
                    int x = (int) (minX + (c % N + (sx + 0.5) / SAMPLES) * size / N), z = (int) (minZ + (c / N + (sz + 0.5) / SAMPLES) * size / N);
                    int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    height += top;
                    if (BotScout.wet(level, x, z))
                        water++;
                }
            surface[c] = height / (SAMPLES * SAMPLES) + 1;
            layers[WATER * CELLS + c] = quantise(water / (float) (SAMPLES * SAMPLES));
            layers[HEIGHT * CELLS + c] = quantise((surface[c] - 40) / 100f);
            layers[WOOD * CELLS + c] = quantise(index.count(level, centre, radius, ResourceName.WOOD) / 60f);
            layers[ORE * CELLS + c] = quantise(index.count(level, centre, radius, ResourceName.ORE) / 30f);
            layers[FOOD * CELLS + c] = quantise(index.count(level, centre, radius, ResourceName.FOOD) / 10f);
        }
        for (int c = 0; c < CELLS; c++) {
            boolean cliff = false;
            for (int n : neighbours(c))
                cliff |= surface[c] > 0 && surface[n] > 0 && Math.abs(surface[c] - surface[n]) > 4;
            layers[CLIFF * CELLS + c] = (byte) (cliff ? 255 : 0);
        }
    }

    private static int[] neighbours(int c) {
        int x = c % N, z = c / N;
        return java.util.stream.IntStream.of(c - 1, c + 1, c - N, c + N)
                .filter(n -> n >= 0 && n < CELLS && (n / N == z || n % N == x)).toArray();
    }

    // where an order may point: land. Where a building may go: land we've seen, away from enemy buildings, and a
    // walk our workers would make
    private void masks() {
        for (int c = 0; c < CELLS; c++) {
            reach[c] = get(WATER, c) < 0.9f;
            BlockPos p = pos(c);
            boolean nearOurs = bot.buildings.stream().anyMatch(b -> b.centrePos.distSqr(p) < BUILD_RANGE * BUILD_RANGE);
            // a neutral camp turns every spot round it down (BotPlacer): the same as an enemy base for building
            boolean enemyNear = get(ENEMY_BUILDINGS, c) > 0 || get(CAMPS, c) > 0;
            for (int n : neighbours(c))
                enemyNear |= get(ENEMY_BUILDINGS, n) > 0 || get(CAMPS, n) > 0;
            buildable[c] = seenTick[c] >= 0 && get(WATER, c) < 0.5f && !enemyNear && nearOurs;
        }
    }

    static String bits(boolean[] mask) {
        byte[] b = new byte[(mask.length + 7) / 8];
        for (int i = 0; i < mask.length; i++)
            if (mask[i])
                b[i / 8] |= (byte) (1 << (i % 8));
        return java.util.Base64.getEncoder().encodeToString(b);
    }

    String encode(int from, int to) {
        return java.util.Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(layers, from * CELLS, to * CELLS));
    }
}
