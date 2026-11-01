package com.solegendary.reignofnether.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.TitleClientEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.internal.BrandingControl;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.net.URI;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    private static final ResourceLocation MINECRAFT_LOGO =
            ResourceLocation.parse("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION =
            ResourceLocation.parse("textures/gui/title/edition.png");
    private static final ResourceLocation DISCORD_TEXTURE =
            ResourceLocation.parse( "textures/gui/title/discord.png");

    @Shadow @Final private PanoramaRenderer panorama;
    @Shadow @Final private boolean fading;
    @Shadow private long fadeInStart;
    @Nullable @Shadow(remap = false) private TitleScreenModUpdateIndicator modUpdateNotification;
    private AbstractWidget discordButton;
    private AbstractWidget mapsButton;

    protected TitleScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        int discordX = this.width - 1022;
        int discordY = this.height - 95;

        this.discordButton = new AbstractWidget(discordX, discordY, 114, 38, Component.empty()) {
            @Override
            public void onClick(double pMouseX, double pMouseY) {
                openLink("https://discord.com/jJV5zK3hT9");
            }

            @Override
            public void updateWidgetNarration(NarrationElementOutput output) {
                output.add(NarratedElementType.TITLE, Component.literal("Join our Discord"));
            }

            @Override
            public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                PoseStack pPoseStack = guiGraphics.pose();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, DISCORD_TEXTURE);

                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                pPoseStack.pushPose();
                float scaleX = 0.8f;
                float scaleY = 0.8f;

                pPoseStack.translate(this.getX(), this.getY(), 0);
                pPoseStack.scale(scaleX, scaleY, 1.0f);

                guiGraphics.blit(DISCORD_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

                if (isHoveredOrFocused())
                    guiGraphics.fill( // x1,y1, x2,y2,
                            0,0,
                            this.width,
                            this.height,
                            0x32FFFFFF); //ARGB(hex); note that alpha ranges between ~0-16, not 0-255

                pPoseStack.popPose();
            }
        };

        // --- "Get RTS maps!" button ---
        // Styled like the vanilla language-select button, to the right of the singleplayer button
        int mapsButtonSize = 20;
        int mapsButtonX = (this.width / 2) + 104;
        int mapsButtonY = (this.height / 4) + 48;

        this.mapsButton = new AbstractWidget(mapsButtonX, mapsButtonY, mapsButtonSize, mapsButtonSize, Component.empty()) {
            @Override
            public void onClick(double pMouseX, double pMouseY) {
                openLink("https://www.curseforge.com/minecraft/mc-mods/reign-of-nether-rts-in-minecraft/files/8714818");
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Get RTS maps!"));
            }
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                // Same background sprite vanilla uses for the language/accessibility square buttons
                ResourceLocation buttonSprite = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID,
                        this.isHoveredOrFocused() ? "textures/hud/title_button_highlighted.png" : "textures/hud/title_button.png");
                guiGraphics.blit(buttonSprite, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
                ItemStack mapStack = new ItemStack(Items.FILLED_MAP);
                mapStack.enchant(Enchantments.UNBREAKING, 1);
                int itemX = this.getX() + (this.width - 16) / 2;
                int itemY = this.getY() + (this.height - 16) / 2;

                // prevent rendering over essential tooltip
                int essX = mapsButtonX;
                int essY = mapsButtonY + mapsButtonSize + 4;
                boolean mouseOverEssentialButton = pMouseX >= essX && pMouseX < essX + mapsButtonSize &&
                                                    pMouseY >= essY && pMouseY < essY + mapsButtonSize &&
                                                    !ModList.get().isLoaded("essential");
                if (!mouseOverEssentialButton)
                    guiGraphics.renderItem(mapStack, itemX, itemY);
                RenderSystem.enableDepthTest();
            }
        };
        this.mapsButton.setTooltip(Tooltip.create(Component.literal("Get RTS maps!")));

        this.addRenderableWidget(this.discordButton);
        this.addRenderableWidget(this.mapsButton);
    }


    private void openLink(String url) {
        try {
            URI uri = new URI(url);
            Util.getPlatform().openUri(uri);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to open URL: " + url, e);
        }
    }

    private static final int titleX = -31;
    private static final int titleY = 12;
    private static final int titleWidth = 330;
    private static final int titleHeight = 110;
    private static final int editionY = 88;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY,
                        float pPartialTick, CallbackInfo ci) {
        ci.cancel();
        PoseStack pPoseStack = guiGraphics.pose();
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
            guiGraphics.blit(MINECRAFT_LOGO, logoX + titleX, titleY, 0, 0,
                    titleWidth, titleHeight,
                    titleWidth, titleHeight
            );

            // Render Edition logo
            RenderSystem.setShaderTexture(0, MINECRAFT_EDITION);
            guiGraphics.blit(MINECRAFT_EDITION, logoX + 44,  editionY, 0.0F, 0.0F, 186, 14, 186, 16);

            // Render main menu elements and splash text
            ForgeHooksClient.renderMainMenu((TitleScreen) Minecraft.getInstance().screen,
                    guiGraphics, this.font, this.width, this.height, alphaMask);

            if (TitleClientEvents.splash != null) {
                pPoseStack.pushPose();
                pPoseStack.translate(this.width / 2 + 90, 82.0, 0.0);
                pPoseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F));
                float scale = 1.8F - Mth.abs(Mth.sin((float) (Util.getMillis() % 1000L) / 1000.0F * 6.2831855F) * 0.1F);
                scale = scale * 100.0F / (float) (this.font.width(TitleClientEvents.splash) + 32);
                pPoseStack.scale(scale, scale, scale);
                int splashX = Math.max(0, this.font.width(TitleClientEvents.splash) / 5);
                guiGraphics.drawCenteredString(this.font, TitleClientEvents.splash, splashX + 14, splashX - 2, 16776960 | alphaMask);
                pPoseStack.popPose();
            }

            // Render branding lines
            BrandingControl.forEachLine(true, true, (line, text) -> {
                if (line == 1) {
                    text = "Reign of Nether " + ReignOfNether.VERSION_STRING;
                }
                guiGraphics.drawString(this.font, text, 2, this.height - (10 + line * (9 + 1)), 16777215 | alphaMask);
            });

            //BrandingControl.forEachAboveCopyrightLine((line, text) -> {
            //    int xPos = this.width - this.font.width(text);
            //    drawString(pPoseStack, this.font, text, xPos, this.height - (10 + (line + 1) * (9 + 1)), 16777215 | alphaMask);
            //});

            // Adjust widget transparency
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setAlpha(alpha);
                }
            }

            // Call the superclass render
            super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

            // Render mod update notification
            if (alpha >= 1.0F && this.modUpdateNotification != null) {
                this.modUpdateNotification.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            // Disable blending after rendering
            RenderSystem.disableBlend();
        }
    }
}