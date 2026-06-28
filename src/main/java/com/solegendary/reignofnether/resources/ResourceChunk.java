package com.solegendary.reignofnether.resources;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.util.EnumMap;

// Resource block positions in one chunk, bucketed by type, as packed BlockPos.asLong world positions.
// Server-main-thread only, so no synchronisation. A LongOpenHashSet (not a list) because block changes do
// O(1) add/remove BY VALUE (a block mined/placed anywhere in the chunk) and we must never store a position
// twice (a re-scan after eviction, or a place onto an already-indexed spot, would otherwise duplicate).
// Iteration order is irrelevant - ResourceIndex.findClosest re-sorts candidates by distance.
public final class ResourceChunk {
    // Only FOOD / WOOD / ORE ever get a bucket; NONE is never indexed.
    private final EnumMap<ResourceName, LongOpenHashSet> buckets = new EnumMap<>(ResourceName.class);

    public void add(ResourceName type, long packed) {
        buckets.computeIfAbsent(type, k -> new LongOpenHashSet()).add(packed);
    }

    // Remove this position from every bucket - on a block change we don't know its previous resource type.
    public void removeAll(long packed) {
        for (LongOpenHashSet set : buckets.values())
            set.remove(packed);
    }

    // The positions of one resource type, or null if none indexed for it.
    public LongOpenHashSet positions(ResourceName type) {
        return buckets.get(type);
    }
}
