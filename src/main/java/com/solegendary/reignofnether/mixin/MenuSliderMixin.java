package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// option sliders: sunken track with an orange handle, same look as the RTS UI
@Mixin(AbstractSliderButton.class)
public abstract class MenuSliderMixin extends AbstractWidget {

    @Shadow protected double value;

    public MenuSliderMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void renderMenuSlider(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hovered = active && isHoveredOrFocused();
        Theme.outline(g, x, y, w, h, 1, Theme.withAlpha(Theme.INK, alpha));
        g.fill(x, y, x + w, y + h, Theme.withAlpha(Theme.SUNKEN, alpha));
        g.fill(x, y, x + w, y + 1, Theme.withAlpha(0x66000000, alpha));
        int handle = 8;
        int hx = x + (int) (value * (w - handle));
        g.fill(hx, y, hx + handle, y + h, Theme.withAlpha(hovered ? Theme.YELLOW : Theme.ORANGE, alpha));
        g.fill(hx, y, hx + handle, y + 1, Theme.withAlpha(Theme.GOLD, alpha));
        g.fill(hx, y + h - 1, hx + handle, y + h, Theme.withAlpha(Theme.ORANGE_DARK, alpha));
        int colour = !active ? Theme.DIM : hovered ? Theme.GOLD : Theme.BODY;
        // keep a colour another mod set with setFGColor; labels too long to fit scroll like vanilla
        if (packedFGColor != UNSET_FG_COLOR)
            colour = packedFGColor | 0xFF000000;
        colour = Theme.withAlpha(colour, alpha);
        var font = Minecraft.getInstance().font;
        if (font.width(getMessage()) > w - 4)
            renderScrollingString(g, font, 2, colour);
        else
            g.drawString(font, getMessage(), x + (w - font.width(getMessage())) / 2, y + (h - 8) / 2, colour, false);
    }
}
