package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Backdrop;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// lists (worlds, servers, options...) sit on a dark panel over the menu backdrop instead of dirt bands
@Mixin(AbstractSelectionList.class)
public abstract class MenuListMixin {

    @Shadow private boolean renderBackground;
    @Shadow private boolean renderTopAndBottom;
    @Shadow protected int x0;
    @Shadow protected int x1;
    @Shadow protected int y0;
    @Shadow protected int y1;

    // lists that drew the dirt bands own the whole screen background (world select, servers...)
    @Unique private Boolean reignofnether$ownsScreen;
    // lists a screen or mod left without a background stay transparent
    @Unique private Boolean reignofnether$hasBackground;

    @Inject(method = "render", at = @At("HEAD"))
    private void renderPanel(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (reignofnether$ownsScreen == null) {
            reignofnether$ownsScreen = renderTopAndBottom;
            reignofnether$hasBackground = renderBackground;
        }
        renderBackground = false;
        renderTopAndBottom = false;
        Minecraft mc = Minecraft.getInstance();
        if (reignofnether$ownsScreen && mc.level == null)
            Backdrop.render(g, g.guiWidth(), g.guiHeight(), 0.55f);
        if (!reignofnether$hasBackground)
            return;
        g.fill(x0, y0, x1, y1, Theme.floating());
        g.fill(x0, y0, x1, y0 + 1, Theme.INK);
        g.fill(x0, y1 - 1, x1, y1, Theme.INK);
    }
}
