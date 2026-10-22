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

// menus without a world (and the pause menu) show the blurred nether image instead of dirt or the game
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void renderNetherInsteadOfDirt(GuiGraphics g, CallbackInfo ci) {
        Backdrop.render(g, width, height, 0.55f);
        ci.cancel();
    }

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void renderNetherBehindPauseMenu(GuiGraphics g, CallbackInfo ci) {
        if ((Object) this instanceof PauseScreen) {
            Backdrop.render(g, width, height, 0.45f);
            // cancelling skips vanilla's post of this event, which other mods draw on
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen) (Object) this, g));
            ci.cancel();
        }
    }
}
