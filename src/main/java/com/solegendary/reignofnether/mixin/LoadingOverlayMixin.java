package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.loading.LoadingScreenClientEvents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// a map's resources.zip reloads the resources while its world opens, under vanilla's Mojang overlay: an Overlay, not a
// Screen, so no Forge screen event reaches it. TAIL keeps the overlay's own finish (onFinish, setOverlay(null))
@Mixin(LoadingOverlay.class)
public class LoadingOverlayMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void coverWorldLoad(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        LoadingScreenClientEvents.drawOverOverlay(g);
    }
}
