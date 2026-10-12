package com.solegendary.reignofnether.resources;

import net.neoforged.fml.common.EventBusSubscriber;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

// Server-side chunk eviction for the resource index. Chunks are scanned lazily on demand (when a worker's
// findClosest touches an un-indexed in-range chunk), like the walkability grid - so we only need to drop a
// chunk's index when it unloads. @EventBusSubscriber self-registers (same style as PathfinderWorkerPool).
@EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public final class ResourceIndexEvents {
    private ResourceIndexEvents() {}

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload evt) {
        if (evt.getLevel() instanceof Level lvl && !lvl.isClientSide())
            ResourceIndex.onChunkUnload(lvl, evt.getChunk().getPos());
    }
}
