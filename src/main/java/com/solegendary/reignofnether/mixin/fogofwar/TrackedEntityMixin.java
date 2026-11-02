package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

// gate entity packets behind fog; target by name since TrackedEntity is package-private
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin {

    @Shadow @Final Entity entity;
    @Shadow @Final private ServerEntity serverEntity;
    @Shadow @Final private Set<ServerPlayerConnection> seenBy;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void reignofnether$gateEntityVisibility(ServerPlayer player, CallbackInfo ci) {
        if (player == entity || !FogOfWarServerEvents.isFogActiveFor(player)) return;
        // block-level gate: an enemy entity hides unless its own column is inside the viewer's circle
        BlockPos bp = entity.blockPosition();
        if (FogOfWarServerEvents.isBlockVisibleFor(player, bp.getX(), bp.getZ())) return;
        // cancelling skips vanilla's own un-track branch, so a player who already tracked this entity would keep
        // receiving its position and data; drop them here (this also sends the remove packet)
        if (seenBy.remove(player.connection))
            serverEntity.removePairing(player);
        ci.cancel();
    }
}
