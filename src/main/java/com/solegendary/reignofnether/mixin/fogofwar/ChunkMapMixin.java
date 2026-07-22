package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// drop fog-dark players from chunk broadcasts so their client view freezes
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

    @Inject(
            method = "getPlayers(Lnet/minecraft/world/level/ChunkPos;Z)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void reignofnether$filterPlayersByFog(
            ChunkPos pos, boolean boundaryOnly,
            CallbackInfoReturnable<List<ServerPlayer>> cir
    ) {
        if (!FogOfWarServerEvents.isEnabled()) return;
        List<ServerPlayer> base = cir.getReturnValue();
        if (base.isEmpty()) return;
        List<ServerPlayer> filtered = null;
        for (int i = 0; i < base.size(); i++) {
            ServerPlayer sp = base.get(i);
            boolean visible = !FogOfWarServerEvents.isFogActiveFor(sp)
                    || FogOfWarServerEvents.isChunkBrightFor(sp, pos);
            if (!visible) {
                if (filtered == null) filtered = new ArrayList<>(base.subList(0, i));
            } else if (filtered != null) {
                filtered.add(sp);
            }
        }
        if (filtered != null) cir.setReturnValue(filtered);
    }
}
