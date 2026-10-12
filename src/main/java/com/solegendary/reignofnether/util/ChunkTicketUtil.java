package com.solegendary.reignofnether.util;

import java.util.Comparator;
import java.util.Objects;

import com.solegendary.reignofnether.ReignOfNether;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * 1.20.1's Forge {@code ChunkTicketManager.forceChunk(level, modId, owner, x, z, add, ticking)}
 * kept a per-owner ticket so a chunk stayed loaded while any of its owners did. NeoForge 21.1
 * dropped that helper in favour of the vanilla region-ticket API, so the per-owner bookkeeping
 * lives here: one ticket type per ticking flag, keyed on the owner object.
 *
 * <p><b>The third argument is a radius in chunks, not a status level.</b>
 * {@code DistanceManager#addRegionTicket} computes
 * {@code ticketLevel = ChunkLevel.byStatus(FullChunkStatus.FULL) - radius}, and a ticket at level
 * {@code L} marks every position within {@code MAX_LEVEL - L} chunks as needing generation - so
 * {@code radius = 0} means "this chunk alone, at FULL" and any larger value floods outwards.
 * Passing a {@code ChunkLevel}/{@code FullChunkStatus} value is the natural misreading here
 * ({@code ChunkLevel.byStatus(FULL)} is 33) and yields {@code level = 0}, i.e. a 44-chunk radius
 * per ticket: one unit then pins roughly 8000 chunks at generation level, which is enough to keep
 * the overworld's {@code ChunkMap#hasWork()} permanently true and to make quitting the world spin
 * on "Saving worlds" with the server thread pegged at 100% CPU.
 *
 * <p>Vanilla tickets are monotone - a lower level always covers a wider area - so there is no way
 * to ask for "this chunk at BLOCK_TICKING but not its neighbours". Radius 0 is the tightest the
 * model can express for both cases.
 */
public final class ChunkTicketUtil {
	private ChunkTicketUtil() {}

	private static final Comparator<Object> OWNER_ORDER =
			(a, b) -> Integer.compare(ownerOrder(a), ownerOrder(b));

	/** Owner-keyed ticket that keeps the chunk loaded and block-ticking. */
	private static final TicketType<Object> OWNER_BLOCK_TICKING =
			TicketType.create(ReignOfNether.MOD_ID + "_owner_block_ticking", OWNER_ORDER);

	/** Owner-keyed ticket that keeps the chunk fully ticking, entities included. */
	private static final TicketType<Object> OWNER_ENTITY_TICKING =
			TicketType.create(ReignOfNether.MOD_ID + "_owner_entity_ticking", OWNER_ORDER);

	/**
	 * TicketType uses the comparator to decide whether a re-added ticket is the same ticket, so
	 * owners of the same chunk must compare equal. Identity hash is that key here.
	 */
	private static int ownerOrder(Object owner) {
		return System.identityHashCode(Objects.requireNonNull(owner, "owner"));
	}

	/**
	 * Forcibly loads (or releases) the chunk containing {@code chunkX}/{@code chunkZ} on behalf of
	 * {@code owner}.
	 *
	 * @param owner   the object holding the chunk; use the entity or placement itself
	 * @param ticking whether the chunk must keep ticking entities, not just blocks
	 */
	public static void forceChunk(ServerLevel level, Object owner, int chunkX, int chunkZ, boolean add, boolean ticking) {
		ChunkPos pos = new ChunkPos(chunkX, chunkZ);
		int radius = 0;
		TicketType<Object> type = ticking ? OWNER_ENTITY_TICKING : OWNER_BLOCK_TICKING;
		if (add) {
			level.getChunkSource().addRegionTicket(type, pos, radius, owner, true);
		} else {
			level.getChunkSource().removeRegionTicket(type, pos, radius, owner, true);
		}
	}
}
