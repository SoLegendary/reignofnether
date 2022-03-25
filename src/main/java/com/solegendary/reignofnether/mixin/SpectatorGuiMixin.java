package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.gui.components.spectator.SpectatorGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// disable spectator GUI entirely when in orthoview mode as it has cheaty functions like tp to player

@Mixin(SpectatorGui.class)
public class SpectatorGuiMixin {

    @Inject(
            method = "onHotbarSelected(I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onHotbarSelected(
            int slotIndex, CallbackInfo ci
    ) {
        if (OrthoviewClientEvents.isEnabled())
            ci.cancel();
    }
}
