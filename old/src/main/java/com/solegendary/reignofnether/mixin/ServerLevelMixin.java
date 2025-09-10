package com.solegendary.reignofnether.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(
            method = "sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z",
            at = @At("TAIL")
    )
    private void sendParticles(ServerPlayer pPlayer, boolean pLongDistance, double pPosX, double pPosY, double pPosZ,
                               Packet<?> pPacket, CallbackInfoReturnable<Boolean> cir) {
        boolean originalSuccess = false;
        BlockPos blockpos = pPlayer.blockPosition();
        if (blockpos.closerToCenterThan(new Vec3(pPosX, pPosY, pPosZ), pLongDistance ? 512.0 : 32.0))
            originalSuccess = true;

        // retry at further distance
        if (!originalSuccess && blockpos.closerToCenterThan(new Vec3(pPosX, pPosY, pPosZ), 512))
            pPlayer.connection.send(pPacket);
    }
}
