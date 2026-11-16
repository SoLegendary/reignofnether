package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.matchstart.MatchResumeServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    // a published server (LAN/P2P) never pauses, and no Forge event can skip a tick: while the match waits on the
    // resume screen, only keep connections, logins and chunk loading going, so nothing moves and nobody times out
    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void reignofnether$freezeWhileResuming(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        if (!MatchResumeServerEvents.resumePending || server.isDedicatedServer())
            return;
        for (ServerLevel level : server.getAllLevels())
            level.getChunkSource().tick(hasTimeLeft, false);
        if (server.getConnection() != null)
            server.getConnection().tick();
        server.getPlayerList().tick();
        ci.cancel();
    }
}
