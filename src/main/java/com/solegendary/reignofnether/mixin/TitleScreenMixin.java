package com.solegendary.reignofnether.mixin;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import com.solegendary.reignofnether.hud.TitleClientEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.internal.BrandingControl;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    private static final ResourceLocation MINECRAFT_LOGO =
            ResourceLocation.parse("textures/gui/title/minecraft.png");
    private static final ResourceLocation MINECRAFT_EDITION =
            ResourceLocation.parse("textures/gui/title/edition.png");
    private static final ResourceLocation DISCORD_ICON =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/discord_icon.png");
    private static final ResourceLocation LILYPAD_TEXTURE =
            ResourceLocation.parse( "textures/gui/title/lilypad.png");

    @Shadow @Final private boolean fading;
    @Shadow private long fadeInStart;
    @Nullable @Shadow(remap = false) private TitleScreenModUpdateIndicator modUpdateNotification;
    @Unique private AbstractWidget lilypadButton;
    @Unique private AbstractWidget discordButton;
    @Unique private AbstractWidget mapsButton;
    @Unique private AbstractWidget tutorialButton;
    @Unique private ItemStack tutorialStack;
    @Unique private static boolean tutorialLaunched = false;
    @Unique private ItemStack mapStack;
    private static final String NETWORK_URL = "https://reignofnether.net";

    protected TitleScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        int lilypadX = this.width - 100;
        int lilypadY = this.height - 57;

        this.lilypadButton = new AbstractWidget(lilypadX, lilypadY, 110, 40, Component.empty()) {
            @Override
            public void onClick(double pMouseX, double pMouseY) {
                openLink("https://lilypad.gg/reignofnether");
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                narrationElementOutput.add(NarratedElementType.TITLE, Component.literal("Choose Lilypad Hosting"));
            }

            @Override
            public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                PoseStack pPoseStack = guiGraphics.pose();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderTexture(0, LILYPAD_TEXTURE);

                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

                pPoseStack.pushPose();
                float scale = 0.8f;
                pPoseStack.translate(this.getX(), this.getY(), 0);
                pPoseStack.scale(scale, scale, 1.0f);

                guiGraphics.blit(LILYPAD_TEXTURE, 0, 0, 0, 0, this.width, this.height, this.width, this.height);

                if (isHoveredOrFocused())
                    guiGraphics.fill( // x1,y1, x2,y2,
                            0,0,
                            this.width,
                            this.height,
                            Theme.HOVER);

                pPoseStack.popPose();
            }
        };


        // --- "Get RTS maps!" button ---
        // Styled like the vanilla language-select button, to the right of the singleplayer button
        int mapsButtonSize = 20;
        this.mapStack = new ItemStack(Items.FILLED_MAP);
        this.mapStack.enchant(Enchantments.UNBREAKING, 1);
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
                Theme.secondaryButton(guiGraphics, getX(), getY(), width, height, isHoveredOrFocused());
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

        // --- "Join our Discord" button ---
        // Same square style, two rows below the maps button (Essential's button sits in between)
        this.discordButton = new AbstractWidget(mapsButtonX, mapsButtonY + 48, mapsButtonSize, mapsButtonSize, Component.empty()) {
            @Override
            public void onClick(double pMouseX, double pMouseY) {
                openLink("https://discord.gg/jJV5zK3hT9");
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("menu.reignofnether.discord"));
            }
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                // Same bevel and drop shadow as the maps button, in Discord's own colour, the logo drawn on it
                Theme.colourButton(guiGraphics, getX(), getY(), width, height, isHoveredOrFocused() ? 0xFF7983F5 : 0xFF5865F2);
                guiGraphics.blit(DISCORD_ICON, this.getX() + 2, this.getY() + 2, 0, 0, 16, 16, 16, 16);
            }
        };

        // --- "Tutorial" button ---
        // Mirrors the maps button on the left of the singleplayer button
        this.tutorialStack = new ItemStack(Items.WRITABLE_BOOK);
        this.tutorialButton = new AbstractWidget(this.width / 2 - 124, mapsButtonY, mapsButtonSize, mapsButtonSize, Component.empty()) {
            @Override
            public void onClick(double pMouseX, double pMouseY) {
                openTutorial();
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("menu.reignofnether.tutorial"));
            }
            @Override
            public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                Theme.secondaryButton(guiGraphics, getX(), getY(), width, height, isHoveredOrFocused());
                guiGraphics.renderItem(tutorialStack, this.getX() + (this.width - 16) / 2, this.getY() + (this.height - 16) / 2);
                RenderSystem.enableDepthTest();
            }
        };

        // RoN has no use for Realms: drop it and let the Mods button take the whole row.
        // Multiplayer shows the network notice once before going on as normal.
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (!(child instanceof Button button) || !(button.getMessage().getContents() instanceof TranslatableContents key))
                continue;
            switch (key.getKey()) {
                case "menu.online" -> this.removeWidget(button);
                case "fml.menu.mods" -> {
                    button.setX(this.width / 2 - 100);
                    button.setWidth(200);
                }
                case "menu.multiplayer" -> {
                    Button multiplayer = Button.builder(button.getMessage(), b -> openMultiplayer(button))
                            .bounds(button.getX(), button.getY(), button.getWidth(), button.getHeight()).build();
                    multiplayer.active = button.active;
                    multiplayer.setTooltip(button.getTooltip());
                    this.removeWidget(button);
                    this.addRenderableWidget(multiplayer);
                }
            }
        }

        this.addRenderableWidget(this.lilypadButton);
        this.addRenderableWidget(this.discordButton);
        this.addRenderableWidget(this.mapsButton);
        this.addRenderableWidget(this.tutorialButton);

        // The launcher's Tutorial button starts the game with -Dreignofnether.tutorial=true: go straight in, once.
        if (!tutorialLaunched && Boolean.getBoolean("reignofnether.tutorial")) {
            tutorialLaunched = true;
            Minecraft.getInstance().tell(this::openTutorial);
        }
    }

    // Opens the tutorial world, generating it from the tutorial seed the first time.
    // Settings match the original tutorial map; TutorialServerEvents sets the spawn in the tutorial area.
    @Unique
    private void openTutorial() {
        Minecraft mc = Minecraft.getInstance();
        String name = TutorialServerEvents.TUTORIAL_MAP_NAME;
        if (mc.getLevelSource().levelExists(name)) {
            mc.createWorldOpenFlows().loadLevel(this, name);
            return;
        }
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
        rules.getRule(GameRules.RULE_DOINSOMNIA).set(false, null);
        rules.getRule(GameRules.RULE_RANDOMTICKING).set(12, null);
        mc.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT),
                new WorldOptions(TutorialServerEvents.TUTORIAL_MAP_SEED, true, false),
                WorldPresets::createNormalWorldDimensions);
    }


    private void openMultiplayer(Button vanillaButton) {
        if (ReignOfNetherClientConfigs.SEEN_NETWORK_NOTICE.get()) {
            vanillaButton.onPress();
            return;
        }
        ReignOfNetherClientConfigs.SEEN_NETWORK_NOTICE.set(true);
        ReignOfNetherClientConfigs.SEEN_NETWORK_NOTICE.save();
        Minecraft.getInstance().setScreen(new ConfirmScreen(getWebsite -> {
            if (getWebsite)
                openLink(NETWORK_URL);
            vanillaButton.onPress();
        }, Component.translatable("title.reignofnether.network_notice.title"),
                Component.translatable("title.reignofnether.network_notice.message"),
                Component.translatable("title.reignofnether.network_notice.website"),
                Component.translatable("title.reignofnether.network_notice.continue")));
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

        // a faction's panorama, dark at the top and darker still at the bottom
        TitleClientEvents.getPanorama().render(pPartialTick, Mth.clamp(fadeProgress, 0.0F, 1.0F));
        guiGraphics.fill(0, 0, this.width, this.height, 0x800D0305);
        int h = this.height;
        guiGraphics.fillGradient(0, 0, this.width, h * 24 / 100, 0xB30D0305, 0x000D0305);
        guiGraphics.fillGradient(0, h * 24 / 100, this.width, h / 2, 0x000D0305, 0x330D0305);
        guiGraphics.fillGradient(0, h / 2, this.width, h, 0x330D0305, 0xE60D0305);
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
            ForgeHooksClient.renderMainMenu((TitleScreen) (Object) this,
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

            // Adjust widget transparency
            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setAlpha(alpha);
                }
            }

            // Call the superclass render
            super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

            // icon buttons get the RTS tooltip rather than vanilla's
            String tip = mapsButton.isHovered() ? "Get RTS maps!"
                    : discordButton.isHovered() ? "Join our Discord"
                    : tutorialButton.isHovered() ? "Tutorial" : null;
            if (tip != null)
                MyRenderer.renderTooltip(guiGraphics, Tooltip.of(tip), pMouseX, pMouseY);

            // Render mod update notification
            if (alpha >= 1.0F && this.modUpdateNotification != null) {
                this.modUpdateNotification.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            // Disable blending after rendering
            RenderSystem.disableBlend();
        }
    }
}