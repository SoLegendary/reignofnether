package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Server-side chunk eviction for the resource index. Chunks are scanned lazily on demand (when a worker's
// findClosest touches an un-indexed in-range chunk), like the walkability grid - so we only need to drop a
// chunk's index when it unloads. @Mod.EventBusSubscriber self-registers (same style as PathfinderWorkerPool).
@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public final class ResourceIndexEvents {
    private ResourceIndexEvents() {}

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload evt) {
        if (evt.getLevel() instanceof Level lvl && !lvl.isClientSide())
            ResourceIndex.onChunkUnload(lvl, evt.getChunk().getPos());
    }
}
