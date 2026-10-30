package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// every vanilla text button (menus, options, world list...) uses the RTS UI's button style instead of stone
@Mixin(AbstractButton.class)
public abstract class MenuButtonMixin extends AbstractWidget {

    public MenuButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void renderMenuButton(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ci.cancel();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hovered = active && isHoveredOrFocused();
        Theme.outline(g, x, y, w, h, 1, Theme.withAlpha(Theme.INK, alpha));
        g.fill(x, y, x + w, y + h, Theme.withAlpha(hovered ? Theme.RAISED_HOVER : active ? Theme.SECONDARY : Theme.LOCKED, alpha));
        g.fill(x, y, x + w, y + 1, Theme.withAlpha(0x17FFFFFF, alpha));
        g.fill(x, y + h - 1, x + w, y + h, Theme.withAlpha(0x8C000000, alpha));
        if (hovered)
            g.fill(x, y, x + 2, y + h, Theme.withAlpha(Theme.ORANGE, alpha));
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
