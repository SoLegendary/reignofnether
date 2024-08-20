package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.solegendary.reignofnether.hud.TitleClientEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import java.util.Iterator;
import java.util.Objects;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    private static final ResourceLocation MINECRAFT_LOGO = new ResourceLocation("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION = new ResourceLocation("textures/gui/title/edition.png");
    @Shadow @Final private PanoramaRenderer panorama;
    @Shadow @Final private boolean fading;
    @Shadow private long fadeInStart;
    @Nullable
    @Shadow private TitleScreenModUpdateIndicator modUpdateNotification;

    protected TitleScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        ci.cancel();

        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }
        float f = this.fading ? (float)(Util.getMillis() - this.fadeInStart) / 1000.0F : 1.0F;

        TitleClientEvents.getPanorama().render(pPartialTick, Mth.clamp(f, 0.0F, 1.0F));
        int j = this.width / 2 - 137;

        /*
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, PANORAMA_OVERLAY);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.fading ? (float)Mth.ceil(Mth.clamp(f, 0.0F, 1.0F)) : 1.0F);
        blit(pPoseStack, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
         */

        float f1 = this.fading ? Mth.clamp(f - 1.0F, 0.0F, 1.0F) : 1.0F;
        int l = Mth.ceil(f1 * 255.0F) << 24;
        if ((l & -67108864) != 0) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, MINECRAFT_LOGO);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f1);
            blit(pPoseStack, j-54, 30, 0, 0, 380, 36, 380, 36);
            RenderSystem.setShaderTexture(0, MINECRAFT_EDITION);
            blit(pPoseStack, j + 44, 67, 0.0F, 0.0F, 186, 14, 186, 16);

            ForgeHooksClient.renderMainMenu((TitleScreen) Minecraft.getInstance().screen, pPoseStack, this.font, this.width, this.height, l);
            if (TitleClientEvents.splash != null) {
                pPoseStack.pushPose();
                pPoseStack.translate(this.width / 2 + 90, 70.0, 0.0);
                pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-20.0F));
                float f2 = 1.8F - Mth.abs(Mth.sin((float)(Util.getMillis() % 1000L) / 1000.0F * 6.2831855F) * 0.1F);
                f2 = f2 * 100.0F / (float)(this.font.width(TitleClientEvents.splash) + 32);
                pPoseStack.scale(f2, f2, f2);
                int f3 = Math.max(0, this.font.width(TitleClientEvents.splash)/5);
                drawCenteredString(pPoseStack, this.font, TitleClientEvents.splash, f3 + 14, f3, 16776960 | l);
                pPoseStack.popPose();
            }

            BrandingControl.forEachLine(true, true, (brdline, brd) -> {
                Font var10001 = this.font;
                int var10004 = this.height;
                int var10006 = brdline;
                Objects.requireNonNull(this.font);
                drawString(pPoseStack, var10001, brd, 2, var10004 - (10 + var10006 * (9 + 1)), 16777215 | l);
            });
            BrandingControl.forEachAboveCopyrightLine((brdline, brd) -> {
                Font var10001 = this.font;
                int var10003 = this.width - this.font.width(brd);
                int var10004 = this.height;
                int var10006 = brdline + 1;
                Objects.requireNonNull(this.font);
                drawString(pPoseStack, var10001, brd, var10003, var10004 - (10 + var10006 * (9 + 1)), 16777215 | l);
            });
            Iterator var12 = this.children().iterator();

            while(var12.hasNext()) {
                GuiEventListener guieventlistener = (GuiEventListener)var12.next();
                if (guieventlistener instanceof AbstractWidget) {
                    ((AbstractWidget)guieventlistener).setAlpha(f1);
                }
            }
            super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);

            if (f1 >= 1.0F) {
                this.modUpdateNotification.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
            }
        }
    }
}
