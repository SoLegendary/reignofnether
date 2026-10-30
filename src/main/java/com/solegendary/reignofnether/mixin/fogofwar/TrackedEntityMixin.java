package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.replay.ReplayServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// gate entity packets behind fog; target by name since TrackedEntity is package-private. A replay also records
// every entity as if fog were off (ReplayServerEvents.recordOmni): nothing else sees all of them, fog or no fog
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Shadow @Final Entity entity;
    @Shadow @Final ServerEntity serverEntity;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void reignofnether$recordSpawn(CallbackInfo ci) {
        if (ReplayServerEvents.isRecording())
            serverEntity.sendPairingData(null, ReplayServerEvents::recordOmni);
    }

    @Inject(method = "broadcast", at = @At("HEAD"))
    private void reignofnether$recordBroadcast(Packet<?> packet, CallbackInfo ci) {
        ReplayServerEvents.recordOmni(packet);
    }

    @Inject(method = "broadcastRemoved", at = @At("HEAD"))
    private void reignofnether$recordRemoved(CallbackInfo ci) {
        if (ReplayServerEvents.isRecording())
            ReplayServerEvents.recordOmni(new ClientboundRemoveEntitiesPacket(entity.getId()));
    }

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void reignofnether$gateEntityVisibility(ServerPlayer player, CallbackInfo ci) {
        if (!FogOfWarServerEvents.isFogActiveFor(player)) return;
        // block-level gate: an enemy entity hides unless its own column is inside the viewer's circle
        BlockPos bp = entity.blockPosition();
        if (FogOfWarServerEvents.isBlockVisibleFor(player, bp.getX(), bp.getZ())) return;
        ci.cancel();
    }
}
