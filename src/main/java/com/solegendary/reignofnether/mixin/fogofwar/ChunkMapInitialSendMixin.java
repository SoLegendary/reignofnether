package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogChunkSnapshot;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// serve the pre-match snapshot when a dark player first loads a chunk
@Mixin(ChunkMap.class)
public abstract class ChunkMapInitialSendMixin {

    @Inject(
            method = "playerLoadedChunk(Lnet/minecraft/server/level/ServerPlayer;Lorg/apache/commons/lang3/mutable/MutableObject;Lnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$serveFogSnapshot(
            ServerPlayer sp,
            MutableObject<ClientboundLevelChunkWithLightPacket> packet,
            LevelChunk chunk,
            CallbackInfo ci
    ) {
        if (!FogOfWarServerEvents.isEnabled()) return;
        if (!FogOfWarServerEvents.isFogActiveFor(sp)) return;
        ChunkPos pos = chunk.getPos();
        // Only fully-live chunks get live data on load. An EDGE chunk is served the snapshot too (its
        // fogged columns must never show current state), then a queued fog-tick resend refreshes it; the
        // client-side merge (ClientChunkCacheMixin) applies that resend only to the visible columns.
        if (FogOfWarServerEvents.isChunkLiveFor(sp, pos)) return;
        if (FogOfWarServerEvents.isChunkSentFor(sp, pos))
            FogOfWarServerEvents.queueResend(sp.getUUID(), pos);
        // Serve the pre-match snapshot instead of the live chunk. Fail closed - if no snapshot exists
        // (capture IO error, or a chunk outside the captured border) still cancel the vanilla send so
        // a fog-dark player can never receive live block data on (re)load. Fog is gated to RTS-optimised
        // maps so every in-border chunk is snapshotted; this is defence-in-depth for the edge cases.
        ClientboundLevelChunkWithLightPacket snap = FogChunkSnapshot.get(pos);
        if (snap != null)
            sp.connection.send(snap);
        ci.cancel();
    }
}
