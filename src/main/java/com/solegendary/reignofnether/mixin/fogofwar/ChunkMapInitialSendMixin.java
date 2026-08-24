package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
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

        if (FogOfWarServerEvents.isChunkLiveFor(sp, pos)) return;
        ClientboundLevelChunkWithLightPacket snap = FogChunkSnapshot.get(pos);
        if (snap != null) {
            sp.connection.send(snap);
        }
        if (FogOfWarServerEvents.isChunkSentFor(sp, pos))
            FogOfWarServerEvents.queueResend(sp.getUUID(), pos);

        ci.cancel();

    }
}
