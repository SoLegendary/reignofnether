package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.panels.SettingsMenu;
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

// menus without a world show the website's background instead of dirt
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void renderNetherInsteadOfDirt(GuiGraphics g, CallbackInfo ci) {
        Backdrop.website(g, width, height, 1);
        ci.cancel();
    }

    // the pause menu and the menus opened from it: the website's background, with the world and the HUD showing
    // dimly through it
    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void renderRedWashBehindPauseMenu(GuiGraphics g, CallbackInfo ci) {
        if ((Object) this instanceof PauseScreen || SettingsMenu.isInPauseMenu()) {
            HudClientEvents.renderUnder(g, true); // the HUD stays in view, greyed out under the menu's background
            Backdrop.website(g, width, height, 0.7f);
            // cancelling skips vanilla's post of this event, which other mods draw on
            MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered((Screen) (Object) this, g));
            ci.cancel();
        }
    }
}
