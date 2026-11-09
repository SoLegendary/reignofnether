package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Backdrop;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// create world draws its own light dirt instead of calling Screen's, so ScreenBackgroundMixin never reaches it, and
// no screen event lets the background be swapped; it gets the website's background like the other menus
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    // the tabs above and the buttons below it sit on the background; the options in between sit on a card
    private static final int TABS_H = 24;
    private static final int FOOTER_H = 38;

    @Inject(method = "renderDirtBackground", at = @At("HEAD"), cancellable = true)
    private void renderWebsiteInsteadOfDirt(GuiGraphics g, CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        Backdrop.website(g, self.width, self.height, 1);
        int w = Math.min(self.width - 32, 360);
        Theme.popup(g, (self.width - w) / 2, TABS_H + 8, w, self.height - TABS_H - FOOTER_H - 16, Theme.ORANGE);
        ci.cancel();
    }

    // the light line over the buttons belonged with the dirt
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void skipFooterLine(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
    }
}
