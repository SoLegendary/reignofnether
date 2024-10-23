package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.hud.TitleClientEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import net.minecraftforge.internal.BrandingControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    private static final ResourceLocation MINECRAFT_LOGO =
            new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION =
            new ResourceLocation("textures/gui/title/edition.png");

    @Shadow @Final private PanoramaRenderer panorama;
    @Shadow @Final private boolean fading;
    @Shadow private long fadeInStart;
    @Nullable @Shadow private TitleScreenModUpdateIndicator modUpdateNotification;

    protected TitleScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(PoseStack pPoseStack, int pMouseX, int pMouseY,
                        float pPartialTick, CallbackInfo ci) {
        ci.cancel();

        // Handle fade-in effect
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }
        float fadeProgress = this.fading
                ? (float) (Util.getMillis() - this.fadeInStart) / 1000.0F
                : 1.0F;

        TitleClientEvents.getPanorama().render(pPartialTick, Mth.clamp(fadeProgress, 0.0F, 1.0F));
        int logoX = this.width / 2 - 137;

        float alpha = this.fading ? Mth.clamp(fadeProgress - 1.0F, 0.0F, 1.0F) : 1.0F;
        int alphaMask = Mth.ceil(alpha * 255.0F) << 24;

        if ((alphaMask & -67108864) != 0) {
            // Ensure proper blending and color state
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);

            // Render Minecraft logo
            RenderSystem.setShaderTexture(0, MINECRAFT_LOGO);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            blit(pPoseStack, logoX - 54, 30, 0, 0, 380, 36, 380, 36);

            // Render Edition logo
            RenderSystem.setShaderTexture(0, MINECRAFT_EDITION);
            blit(pPoseStack, logoX + 44, 67, 0.0F, 0.0F, 186, 14, 186, 16);

            // Render main menu elements and splash text
            ForgeHooksClient.renderMainMenu((TitleScreen) Minecraft.getInstance().screen,
                    pPoseStack, this.font, this.width, this.height, alphaMask);

            if (TitleClientEvents.splash != null) {
                pPoseStack.pushPose();
                pPoseStack.translate(this.width / 2 + 90, 70.0, 0.0);
                pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-20.0F));
                float scale = 1.8F - Mth.abs(Mth.sin((float) (Util.getMillis() % 1000L) / 1000.0F * 6.2831855F) * 0.1F);
                scale = scale * 100.0F / (float) (this.font.width(TitleClientEvents.splash) + 32);
                pPoseStack.scale(scale, scale, scale);
                int splashX = Math.max(0, this.font.width(TitleClientEvents.splash) / 5);
                drawCenteredString(pPoseStack, this.font, TitleClientEvents.splash, splashX + 14, splashX - 2, 16776960 | alphaMask);
                pPoseStack.popPose();
            }

            // Render branding lines
            BrandingControl.forEachLine(true, true, (line, text) -> {
                drawString(pPoseStack, this.font, text, 2, this.height - (10 + line * (9 + 1)), 16777215 | alphaMask);
            });

            BrandingControl.forEachAboveCopyrightLine((line, text) -> {
                int xPos = this.width - this.font.width(text);
                drawString(pPoseStack, this.font, text, xPos, this.height - (10 + (line + 1) * (9 + 1)), 16777215 | alphaMask);
            });

            // Adjust widget transparency
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setAlpha(alpha);
                }
            }

            // Call the superclass render
            super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

            // Render mod update notification
            if (alpha >= 1.0F && this.modUpdateNotification != null) {
                this.modUpdateNotification.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            }

            // Disable blending after rendering
            RenderSystem.disableBlend();
        }
    }
}

