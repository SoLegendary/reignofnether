package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// vanilla's language and accessibility squares: their own icons on the dark button instead of stone
@Mixin(ImageButton.class)
public abstract class MenuImageButtonMixin extends Button {

    @Shadow @Final protected ResourceLocation resourceLocation;
    @Shadow @Final protected int yTexStart;

    private static final ResourceLocation ACCESSIBILITY = ResourceLocation.withDefaultNamespace("textures/gui/accessibility.png");
    private static final ResourceLocation LANGUAGE_ICON = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/menu_language.png");
    private static final ResourceLocation ACCESSIBILITY_ICON = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/menu_accessibility.png");

    protected MenuImageButtonMixin(int x, int y, int w, int h, Component message, OnPress onPress, CreateNarration narration) {
        super(x, y, w, h, message, onPress, narration);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void renderMenuIcon(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ResourceLocation icon = resourceLocation.equals(WIDGETS_LOCATION) && yTexStart == 106 ? LANGUAGE_ICON
                : resourceLocation.equals(ACCESSIBILITY) ? ACCESSIBILITY_ICON : null;
        if (icon == null)
            return;
        ci.cancel();
        Theme.secondaryButton(g, getX(), getY(), width, height, isHoveredOrFocused());
        g.blit(icon, getX() + (width - 20) / 2, getY() + (height - 20) / 2, 0, 0, 20, 20, 20, 20);
    }
}
