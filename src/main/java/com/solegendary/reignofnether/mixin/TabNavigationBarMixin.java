package com.solegendary.reignofnether.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// vanilla's tab bar (create world) paints a black strip and a dirt line behind its tabs; the menu background shows
// through instead. Its tabs are drawn by TabButtonMixin
@Mixin(TabNavigationBar.class)
public abstract class TabNavigationBarMixin {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void skipBlackStrip(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void skipHeaderLine(GuiGraphics g, ResourceLocation texture, int x, int y, float u, float v, int w, int h, int texW, int texH) {
    }
}
