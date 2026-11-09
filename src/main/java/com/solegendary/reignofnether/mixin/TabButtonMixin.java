package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// vanilla tabs (create world) are dirt-textured and not a button, so MenuButtonMixin misses them: they get the RTS
// button look, the open one raised with an orange edge along its top. Vanilla still draws the label and underline
@Mixin(TabButton.class)
public abstract class TabButtonMixin extends AbstractWidget {

    public TabButtonMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Redirect(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitNineSliced(Lnet/minecraft/resources/ResourceLocation;IIIIIIIIIIII)V"))
    private void renderRtsTab(GuiGraphics g, ResourceLocation texture, int x, int y, int w, int h,
                              int a, int b, int c, int d, int e, int f, int u, int v) {
        boolean selected = ((TabButton) (Object) this).isSelected();
        boolean hovered = isHoveredOrFocused();
        Theme.outline(g, x, y, w, h, 1, Theme.INK);
        g.fill(x, y, x + w, y + h, selected ? Theme.RAISED : hovered ? Theme.RAISED_HOVER : Theme.SECONDARY);
        if (selected)
            g.fill(x, y, x + w, y + 2, Theme.ORANGE);
    }
}
