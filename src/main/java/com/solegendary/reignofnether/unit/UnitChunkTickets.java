package com.solegendary.reignofnether.unit;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;

import java.util.IdentityHashMap;
import java.util.Map;

/** Server-thread-only ownership of the temporary loading area around each unit. */
public final class UnitChunkTickets {
    private static final int RADIUS = 2;
    // Separate ownership from FORCED tickets used by other systems; no timeout is needed.
    private static final TicketType<ChunkPos> UNIT_TICKET = TicketType.create(
            "reignofnether_unit", (first, second) -> Long.compare(first.toLong(), second.toLong()));
    private static final Map<ServerLevel, LevelTickets> LEVELS = new IdentityHashMap<>();

    private UnitChunkTickets() {}

    public static void update(Mob unit) {
        if (!(unit.level() instanceof ServerLevel level) || unit.isRemoved())
            return;

        LevelTickets tickets = LEVELS.computeIfAbsent(level, ignored -> new LevelTickets());
        ChunkPos previous = tickets.units.get(unit);
        int chunkX = unit.blockPosition().getX() >> 4;
        int chunkZ = unit.blockPosition().getZ() >> 4;
        if (previous != null && previous.x == chunkX && previous.z == chunkZ)
            return;

        ChunkPos current = new ChunkPos(chunkX, chunkZ);
        // Acquire the new area before releasing the old one, preserving their overlap.
        tickets.changeArea(level, current, previous, true);
        if (previous != null)
            tickets.changeArea(level, previous, current, false);
        tickets.units.put(unit, current);
    }

    public static void remove(ServerLevel level, Entity unit) {
        LevelTickets tickets = LEVELS.get(level);
        if (tickets == null)
            return;
        ChunkPos previous = tickets.units.remove(unit);
        if (previous != null)
            tickets.changeArea(level, previous, null, false);
        if (tickets.units.isEmpty())
            LEVELS.remove(level);
    }

    // The level owns the actual tickets and discards them when it shuts down.
    public static void clear(ServerLevel level) {
        LEVELS.remove(level);
    }

    public static void clear() {
        LEVELS.clear();
    }

    private static final class LevelTickets {
        private final Map<Entity, ChunkPos> units = new IdentityHashMap<>();
        private final Long2IntOpenHashMap references = new Long2IntOpenHashMap();

        private void changeArea(ServerLevel level, ChunkPos center, ChunkPos retained, boolean add) {
            for (int x = center.x - RADIUS; x <= center.x + RADIUS; x++) {
                for (int z = center.z - RADIUS; z <= center.z + RADIUS; z++) {
                    if (retained != null && Math.abs(x - retained.x) <= RADIUS &&
                            Math.abs(z - retained.z) <= RADIUS)
                        continue;

                    long key = ChunkPos.asLong(x, z);
                    if (add) {
                        if (references.addTo(key, 1) == 0) {
                            ChunkPos pos = new ChunkPos(x, z);
                            level.getChunkSource().addRegionTicket(UNIT_TICKET, pos, RADIUS, pos);
                        }
                    } else {
                        int count = references.get(key);
                        if (count == 1) {
                            references.remove(key);
                            ChunkPos pos = new ChunkPos(x, z);
                            level.getChunkSource().removeRegionTicket(UNIT_TICKET, pos, RADIUS, pos);
                        } else {
                            references.put(key, count - 1);
                        }
                    }
                }
            }
        }
    }
}
