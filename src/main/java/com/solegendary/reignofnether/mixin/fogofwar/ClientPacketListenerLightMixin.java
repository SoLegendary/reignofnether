package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Per-column light masking, the light counterpart of the block-state merge in ClientChunkCacheMixin.
//
// Every DataLayer the server sends reaches the light engine through ClientPacketListener.readSectionList,
// which both applyLightData (light attached to a chunk packet) and handleLightUpdatePacket funnel into, so
// redirecting the queueSectionData call covers all of it. Server light for an edge chunk was computed with
// the blocks the player cannot see, so a hidden block on the ground darkens the ground it stands on. For
// fogged columns we keep the light the client already has - which came from the snapshot, and therefore
// matches the terrain the client is actually rendering - and let visible columns take the fresh values.
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerLightMixin {

    @Redirect(
            method = "readSectionList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;queueSectionData"
                            + "(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/SectionPos;"
                            + "Lnet/minecraft/world/level/chunk/DataLayer;)V"
            )
    )
    private void reignofnether$maskIncomingLight(
            LevelLightEngine engine, LightLayer layer, SectionPos pos, DataLayer incoming
    ) {
        long[] mask = FogOfWarClientEvents.getLightMask(pos.x(), pos.z());
        if (mask == null) { // fog off, or a fully visible chunk: server light is authoritative
            engine.queueSectionData(layer, pos, incoming);
            return;
        }
        // A section the client has no real light for does NOT come back null here - the engine hands out an
        // empty DataLayer instead, and merging against that freezes fogged columns at light level 0, i.e.
        // pitch black. Adopt the incoming values in that case; getLightMask covers the same situation at
        // chunk granularity via the freshly-loaded flag.
        DataLayer current = engine.getLayerListener(layer).getDataLayerData(pos);
        if (current == null || current.isEmpty()) {
            engine.queueSectionData(layer, pos, incoming);
            return;
        }
        engine.queueSectionData(layer, pos, reignofnether$merge(incoming, current, mask));
    }

    // Build a layer we own rather than editing either input: queueSectionData keeps the reference, and the
    // one passed in is reused by the caller for the empty-section case.
    @Unique
    private static DataLayer reignofnether$merge(DataLayer incoming, DataLayer current, long[] mask) {
        DataLayer merged = new DataLayer(new byte[2048]);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int i = (lx << 4) | lz;
                boolean visible = (mask[i >> 6] & (1L << (i & 63))) != 0;
                DataLayer src = visible ? incoming : current;
                if (src == null) continue; // absent = all zero, and merged is already zeroed
                for (int ly = 0; ly < 16; ly++)
                    merged.set(lx, ly, lz, src.get(lx, ly, lz));
            }
        }
        return merged;
    }
}

