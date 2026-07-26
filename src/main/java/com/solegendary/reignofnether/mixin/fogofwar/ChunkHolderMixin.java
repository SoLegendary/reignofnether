package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Per-packet fog gate for chunk broadcasts (block updates, multi-block section updates, block entity data,
// light). ChunkMapMixin already dropped fog-dark players from the recipient list; this refines edge chunks
// per column via FogOfWarServerEvents.shouldSendChunkPacket so the visible part of an edge chunk updates
// live while its fogged part stays frozen.
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

    @Shadow @Final ChunkPos pos;

    @Inject(
            method = "broadcast(Ljava/util/List;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void reignofnether$gateEdgeChunkPackets(List<ServerPlayer> players, Packet<?> packet, CallbackInfo ci) {
        if (!FogOfWarServerEvents.isEnabled()) return;
        ci.cancel();
        for (ServerPlayer sp : players)
            sp.connection.send(packet);
    }
}
