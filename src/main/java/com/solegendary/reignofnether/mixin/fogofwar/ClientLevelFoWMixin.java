package com.solegendary.reignofnether.mixin.fogofwar;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelFoWMixin {

    @Inject(
            method = "addDestroyBlockEffect",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onAddDestroyBlockEffect(BlockPos pPos, BlockState pState, CallbackInfo ci) {
        if (!FogOfWarClientEvents.isInBrightChunk(pPos))
            ci.cancel();
    }
}
