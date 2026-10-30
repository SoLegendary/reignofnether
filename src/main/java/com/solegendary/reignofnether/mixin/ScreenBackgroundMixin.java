package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Backdrop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// menus without a world show the blurred nether image instead of dirt
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void renderNetherInsteadOfDirt(GuiGraphics g, CallbackInfo ci) {
        Backdrop.render(g, width, height, 0.55f);
        ci.cancel();
    }

    // the pause menu keeps the world visible under the match setup's nether-red wash
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void renderRedWashBehindPauseMenu(GuiGraphics g, CallbackInfo ci) {
        if ((Object) this instanceof PauseScreen) {
            g.fillGradient(0, 0, width, height, 0xA62A0806, 0xCC140303);
            // cancelling skips vanilla's post of this event, which other mods draw on
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen) (Object) this, g));
            ci.cancel();
        }
    }
}
