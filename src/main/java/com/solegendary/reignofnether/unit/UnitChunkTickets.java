package com.solegendary.reignofnether.unit;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-thread-only ownership of each unit's runtime loading area and persistent center ticket. */
public final class UnitChunkTickets {
    private static final int RADIUS = 2;
    private static final int RESTORED_TICKET_GRACE_TICKS = 200;
    // Separate ownership from FORCED tickets used by other systems; no timeout is needed.
    private static final TicketType<ChunkPos> UNIT_TICKET = TicketType.create(
            "reignofnether_unit", (first, second) -> Long.compare(first.toLong(), second.toLong()));
    private static final Map<ServerLevel, LevelTickets> LEVELS = new IdentityHashMap<>();
    private static final Map<ServerLevel, RestoredTickets> RESTORED_TICKETS = new IdentityHashMap<>();

    private record RestoredTickets(int checkTick, Map<UUID, Pair<LongSet, LongSet>> owners) {}

    private UnitChunkTickets() {}

    public static void registerLoadingCallback() {
        ForgeChunkManager.setForcedChunkLoadingCallback(ReignOfNether.MOD_ID, (level, helper) -> {
            if (helper.getEntityTickets().isEmpty())
                return;
            Map<UUID, Pair<LongSet, LongSet>> owners = new HashMap<>();
            helper.getEntityTickets().forEach((uuid, chunks) -> owners.put(uuid, Pair.of(
                    new LongOpenHashSet(chunks.getFirst()), new LongOpenHashSet(chunks.getSecond()))));
            // Keep every ticket during restoration: distant units need their chunks to load first.
            // Startup levels start at tick zero; later-loaded levels receive the same grace period.
            RESTORED_TICKETS.put(level, new RestoredTickets(
                    level.getServer().getTickCount() + RESTORED_TICKET_GRACE_TICKS, owners));
        });
    }

    public static void cleanupRestoredTickets(MinecraftServer server) {
        if (RESTORED_TICKETS.isEmpty())
            return;
        Iterator<Map.Entry<ServerLevel, RestoredTickets>> pending = RESTORED_TICKETS.entrySet().iterator();
        while (pending.hasNext()) {
            var entry = pending.next();
            ServerLevel level = entry.getKey();
            RestoredTickets restored = entry.getValue();
            if (level.getServer() != server || server.getTickCount() - restored.checkTick() < 0)
                continue;
            pending.remove(); // One pass per restored level, rather than a permanent entity scan.
            for (var owner : restored.owners().entrySet()) {
                if (level.getEntity(owner.getKey()) != null)
                    continue;
                releaseRestoredTickets(level, owner.getKey(), owner.getValue().getFirst(), false);
                releaseRestoredTickets(level, owner.getKey(), owner.getValue().getSecond(), true);
            }
        }
    }

    private static void releaseRestoredTickets(ServerLevel level, UUID owner, LongSet chunks, boolean ticking) {
        for (long chunk : chunks) {
            ChunkPos pos = new ChunkPos(chunk);
            // This removes both the live ticket and its chunks.dat entry, marking the save data dirty.
            ForgeChunkManager.forceChunk(level, ReignOfNether.MOD_ID, owner, pos.x, pos.z, false, ticking);
        }
    }

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
        RESTORED_TICKETS.remove(level);
    }

    public static void clear() {
        LEVELS.clear();
        RESTORED_TICKETS.clear();
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
