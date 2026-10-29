package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.ReignOfNether;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.IdentityHashMap;
import java.util.Map;

/** Server-thread-only ownership of each unit's runtime loading area and persistent center ticket. */
public final class UnitChunkTickets {
    private static final int RADIUS = 2;
    // Separate ownership from FORCED tickets used by other systems; no timeout is needed.
    private static final TicketType<ChunkPos> UNIT_TICKET = TicketType.create(
            "reignofnether_unit", (first, second) -> Long.compare(first.toLong(), second.toLong()));
    private static final Map<ServerLevel, LevelTickets> LEVELS = new IdentityHashMap<>();

    private UnitChunkTickets() {}

    public static void update(Entity unit) {
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
        // Forge saves this entity-owned center ticket and restores it before units load on world reopen.
        ForgeChunkManager.forceChunk(level, ReignOfNether.MOD_ID, unit, current.x, current.z, true, true);
        if (previous != null) {
            ForgeChunkManager.forceChunk(level, ReignOfNether.MOD_ID, unit, previous.x, previous.z, false, true);
            tickets.changeArea(level, previous, current, false);
        }
        tickets.units.put(unit, current);
    }

    public static void remove(ServerLevel level, Entity unit) {
        LevelTickets tickets = LEVELS.get(level);
        if (tickets == null)
            return;
        ChunkPos previous = tickets.units.remove(unit);
        if (previous != null) {
            ForgeChunkManager.forceChunk(level, ReignOfNether.MOD_ID, unit, previous.x, previous.z, false, true);
            tickets.changeArea(level, previous, null, false);
        }
        if (tickets.units.isEmpty())
            LEVELS.remove(level);
    }

    // Runtime tickets belong to the closing level; retain persistent centers so Forge restores them on reopen.
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
