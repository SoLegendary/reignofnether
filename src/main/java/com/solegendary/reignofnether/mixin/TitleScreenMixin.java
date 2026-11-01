package com.solegendary.reignofnether.mixin;

import com.solegendary.reignofnether.ui.Backdrop;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ui.Account;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.mappack.MapPack;
import com.solegendary.reignofnether.mappack.MapPackScreen;
import com.solegendary.reignofnether.p2p.JoinScreen;
import com.solegendary.reignofnether.worldselect.WorldSelectScreen;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import com.solegendary.reignofnether.hud.TitleClientEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
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
import com.solegendary.reignofnether.replay.ReplayBrowserScreen;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.gui.TitleScreenModUpdateIndicator;
import net.minecraftforge.internal.BrandingControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The title screen of the RoN design (Title Screen v2): the RoN logo and splash, then a grid of
 * square | wide | wide | square cells: Singleplayer + map pack, the locked campaign, Tutorial | Replays,
 * Multiplayer | Mods and Options | Quit, with language/accessibility left and Discord right. "Join a game" (P2P)
 * sits top right, next to the account.
 * Laid out in GUI coordinates, so it follows the GUI scale like the pause menu.
 */
@Mixin(TitleScreen.class)
public class TitleScreenMixin extends Screen {

    private static final ResourceLocation LOGO =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/title.png");
    private static final ResourceLocation LANGUAGE_ICON =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/menu_language.png");
    private static final ResourceLocation ACCESSIBILITY_ICON =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/gui/menu_accessibility.png");
    private static final ResourceLocation DISCORD_ICON =
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/discord_icon.png");

    // the design's measurements, in its pixels (1600x900 frame)
    @Unique private static final int LOGO_TEX_W = 1906, LOGO_TEX_H = 631;
    @Unique private static final int GRID_W = 660, SQ = 56, GAP = 12, HERO_H = 80, ROW_H = 50, LOGO_W = 760;
    @Unique private static final int[] GOLD = { Theme.GOLD, Theme.GOLD, Theme.GOLD, Theme.GOLD };
    @Unique private static final int CAMPAIGN_GREY = 0xFF575A82;

    @Shadow @Final private boolean fading;
    @Shadow private long fadeInStart;
    @Nullable @Shadow(remap = false) private TitleScreenModUpdateIndicator modUpdateNotification;
    @Unique private AbstractWidget discordButton;
    @Unique private AbstractWidget websiteButton;
    @Unique private AbstractWidget mapPackButton;
    @Unique private PlainTextButton vanillaCopyright;
    @Unique private static boolean tutorialLaunched = false;
    @Unique private static boolean joinLaunched = false;
    private static final String NETWORK_URL = "https://reignofnether.net";

    // layout, in GUI coordinates
    @Unique private float ronScale; // units per design pixel
    @Unique private int gridX, gridY, logoX, logoY, logoW, logoH;

    protected TitleScreenMixin(Component pTitle) {
        super(pTitle);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        layout();
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().getTexture(LOGO).setFilter(true, false); // the logo is shown smaller than its art
        Account.skin(); // start loading it

        // RoN has no use for Realms. The vanilla buttons stay (hidden), so other mods still find them; the grid
        // has its own buttons that press them. Multiplayer shows the network notice once first.
        for (GuiEventListener child : List.copyOf(this.children())) {
            if (child instanceof PlainTextButton copyright) {
                // drawn small by the copyright line below, which still opens the credits
                this.vanillaCopyright = copyright;
                copyright.visible = false;
                continue;
            }
            if (!(child instanceof Button button) || !(button.getMessage().getContents() instanceof TranslatableContents key))
                continue;
            switch (key.getKey()) {
                case "menu.online" -> this.removeWidget(button);
                case "menu.singleplayer" -> button.visible = false; // replaced by the RoN singleplayer button
                case "fml.menu.mods" -> gridButton(button, cell(2, 1, 3), button::onPress, false);
                case "menu.multiplayer" -> gridButton(button, cell(1, 1, 3), () -> openMultiplayer(button), false);
                case "menu.options" -> gridButton(button, cell(1, 1, 4), button::onPress, false);
                case "menu.quit" -> gridButton(button, cell(2, 1, 4), button::onPress, true);
                case "narrator.button.language" -> sideButton(button, cell(0, 1, 3), LANGUAGE_ICON, 18);
                case "narrator.button.accessibility" -> sideButton(button, cell(0, 1, 4), ACCESSIBILITY_ICON, 16);
            }
        }
        // --- Singleplayer: dark panel with an orange top edge; RoN's map picker also creates, edits and deletes worlds ---
        int[] sp = cell(1, 2, 0);
        this.addRenderableWidget(widget(sp, Component.translatable("menu.reignofnether.skirmish"), this::openWorldSelect, (g, hov) -> {
            int x = sp[0], y = sp[1], w = sp[2], h = sp[3];
            // orange glow above it, dark ring, warm-to-grey gradient over the top 55%, orange top edge
            // with a faint gold line under it, dark bottom edge
            g.fillGradient(x, y - 6, x + w, y - 1, 0x00F19A0F, 0x40F19A0F);
            Theme.outline(g, x, y, w, h, 1, 0xFF14131A);
            int mid = y + h * 55 / 100;
            g.fillGradient(x, y, x + w, mid, hov ? 0xFF4A3A26 : 0xFF3A2F22, hov ? 0xFF33323D : 0xFF26252E);
            g.fill(x, mid, x + w, y + h, hov ? 0xFF33323D : 0xFF26252E);
            g.fill(x, y, x + w, y + 1, Theme.ORANGE);
            g.fill(x, y + 1, x + w, y + 2, 0x2EFEF481);
            g.fill(x, y + h - 2, x + w, y + h, 0x8C000000);
            // vanilla font: title, subtitle under it
            var font = Minecraft.getInstance().font;
            String title = plain(Component.translatable("menu.reignofnether.skirmish"));
            // the subtitle in the vanilla font at the RoN UI's size, under the full-size title
            float k = Text.guiPerUnit();
            Component sub = silk(plain(Component.translatable("menu.reignofnether.singleplayer_sub")));
            int ty = y + Math.round((h - 8 - 3 - 8 * k) / 2);
            g.drawString(font, title, x + (w - font.width(title)) / 2, ty, hov ? Theme.GOLD : Theme.TEXT, true);
            Text.atGui(g, x + w / 2f, ty + 11, () -> g.drawString(font, sub, -font.width(sub) / 2, 0, 0xFF9A9FC1, false));
        }));

        // --- Official map pack: opens the download popup, the dot shows whether it's installed ---
        int[] row1 = cell(1, 2, 1);
        int[] mp = { row1[0] + row1[2] - row1[3], row1[1], row1[3], row1[3] };
        this.mapPackButton = widget(mp, Component.translatable("menu.reignofnether.map_pack"), this::openMapPack, (g, hov) -> {
            flat(g, mp, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);
            Icon.MAP.draw(g, mp[0] + (mp[2] - 14) / 2, mp[1] + (mp[3] - 14) / 2, 14, Theme.BODY); // 12 visible, like the other icons
            int dot = switch (MapPack.state()) {
                case INSTALLED -> Theme.OK;
                case DOWNLOADING -> Theme.GOLD;
                default -> Theme.ORANGE;
            };
            int d = 4, dx = mp[0] + mp[2] - 3 - d, dy = mp[1] + 3;
            Theme.outline(g, dx, dy, d, d, 1, Theme.INK);
            g.fill(dx, dy, dx + d, dy + d, dot);
        });
        this.addRenderableWidget(this.mapPackButton);

        // --- Story campaign: locked, not out yet ---
        int[] cp = { row1[0], row1[1], row1[2] - row1[3] - Math.round(GAP * ronScale), row1[3] };
        AbstractWidget campaign = widget(cp, Component.translatable("menu.reignofnether.campaign"), () -> { }, (g, hov) -> {
            g.fill(cp[0], cp[1], cp[0] + cp[2], cp[1] + cp[3], Theme.PANEL);
            Theme.outline(g, cp[0], cp[1], cp[2], cp[3], 1, 0xFF24232C); // outside, like flat(), so it lines up with the grid
            String title = plain(Component.translatable("menu.reignofnether.campaign"));
            String chip = plain(Component.translatable("menu.reignofnether.campaign_soon"));
            // lock and grey title at full size like the other buttons, the "Coming later" chip at the RoN UI's size
            var font = Minecraft.getInstance().font;
            float k = Text.guiPerUnit();
            int lock = 14, gap = 5, chipPad = 4, chipH = 13;
            int chipU = font.width(silk(chip)) + 2 * chipPad, chipW = Math.round(chipU * k);
            String t = Text.fit(title, cp[2] - 8 - lock - 2 * gap - chipW);
            int total = lock + gap + font.width(t) + gap + chipW;
            int x = cp[0] + (cp[2] - total) / 2, cy = cp[1] + cp[3] / 2;
            Icon.LOCK.draw(g, x, cy - lock / 2, lock, CAMPAIGN_GREY);
            x += lock + gap;
            g.drawString(font, t, x, cy - 4, CAMPAIGN_GREY, false);
            x += font.width(t) + gap;
            Text.atGui(g, x, cy, () -> {
                g.fill(0, -chipH / 2 - 1, chipU, chipH - chipH / 2 - 1, 0xFF24232C);
                g.drawString(font, silk(chip), chipPad, -4, 0xFF9A9FC1, false);
            });
        });
        campaign.active = false;
        this.addRenderableWidget(campaign);

        // --- Tutorial | Replays ---
        int[] tu = cell(1, 1, 2);
        String tutorial = plain(Component.translatable("menu.reignofnether.tutorial"));
        this.addRenderableWidget(widget(tu, Component.literal(tutorial), this::openTutorial, (g, hov) -> {
            flat(g, tu, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);
            int icon = 12, gap = 5, total = icon + gap + Text.width(tutorial);
            int x = tu[0] + (tu[2] - total) / 2, cy = tu[1] + tu[3] / 2;
            Icon.BOOK_OPEN.draw(g, x, cy - icon / 2, icon, Theme.ORANGE);
            Text.draw(g, tutorial, x + icon + gap, cy - 4, hov ? Theme.GOLD : Theme.BODY);
        }));
        int[] rp = cell(2, 1, 2);
        String replays = plain(Component.translatable("menu.reignofnether.replays"));
        this.addRenderableWidget(widget(rp, Component.literal(replays), () -> Minecraft.getInstance().setScreen(new ReplayBrowserScreen(this)), (g, hov) -> {
            flat(g, rp, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);
            int icon = 12, gap = 5, total = icon + gap + Text.width(replays);
            int x = rp[0] + (rp[2] - total) / 2, cy = rp[1] + rp[3] / 2;
            Icon.EYE.draw(g, x, cy - icon / 2, icon, Theme.ORANGE);
            Text.draw(g, replays, x + icon + gap, cy - 4, hov ? Theme.GOLD : Theme.BODY);
        }));

        // --- "Join our Discord", right of Options | Quit ---
        int[] dc = cell(3, 1, 4);
        this.discordButton = widget(dc, Component.translatable("menu.reignofnether.discord"), () -> openLink("https://discord.gg/jJV5zK3hT9"), (g, hov) -> {
            // same bevel as the other squares, in Discord's own colour, the logo drawn on it
            flat(g, dc, hov ? 0xFF7983F5 : 0xFF5865F2);
            int s = 12;
            g.blit(DISCORD_ICON, dc[0] + (dc[2] - s) / 2, dc[1] + (dc[3] - s) / 2, s, s, 0, 0, 16, 16, 16, 16);
        });
        this.addRenderableWidget(this.discordButton);

        // --- Official website, above Discord ---
        int[] ws = cell(3, 1, 3);
        this.websiteButton = widget(ws, Component.translatable("menu.reignofnether.website"), () -> openLink(NETWORK_URL), (g, hov) -> {
            flat(g, ws, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);
            Icon.GLOBE.draw(g, ws[0] + (ws[2] - 14) / 2, ws[1] + (ws[3] - 14) / 2, 14, Theme.BODY);
        });
        this.addRenderableWidget(this.websiteButton);

        // --- Copyright, small in the bottom right like the design; still opens the credits ---
        if (this.vanillaCopyright != null) {
            String copyright = this.vanillaCopyright.getMessage().getString().toUpperCase();
            int cw = Math.round(Minecraft.getInstance().font.width(silk(copyright)) * Text.guiPerUnit());
            int ch = Math.max(4, Math.round(8 * Text.guiPerUnit()));
            this.addRenderableWidget(new AbstractWidget(this.width - cw - Math.round(14 * Text.guiPerUnit()), this.height - Math.round(16 * Text.guiPerUnit()), cw, ch, Component.literal(copyright)) {
                @Override
                public void onClick(double pMouseX, double pMouseY) {
                    vanillaCopyright.onPress();
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
                @Override
                public void renderWidget(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
                    Text.atGui(guiGraphics, getX(), getY(), () ->
                            guiGraphics.drawString(Minecraft.getInstance().font, silk(copyright), 0, 0, isHoveredOrFocused() ? Theme.GOLD : Theme.DIM, false));
                }
            });
        }

        // --- Join a game (P2P code): top right, left of the account chip ---
        float k = Text.guiPerUnit();
        String join = plain(Component.translatable("menu.reignofnether.join_game"));
        int jw = 9 + 12 + 5 + Text.width(join) + 9, jh = 24;
        int jx = Math.round(this.width / k) - 14 - accountWidth() - 8 - jw;
        int[] jb = { Math.round(jx * k), Math.round(10 * k), Math.round(jw * k), Math.round(jh * k) };
        this.addRenderableWidget(widget(jb, Component.literal(join), () -> Minecraft.getInstance().setScreen(new JoinScreen(this, "")), (g, hov) ->
                Text.atGui(g, jb[0], jb[1], () -> {
                    Theme.secondaryButton(g, 0, 0, jw, jh, hov);
                    Icon.USERS.draw(g, 9, (jh - 12) / 2, 12, hov ? Theme.GOLD : Theme.ORANGE);
                    Text.draw(g, join, 9 + 12 + 5, (jh - 8) / 2 + 1, hov ? Theme.GOLD : Theme.BODY);
                })));

        // The launcher's P2P games start the game with -Dreignofnether.join=CODE: open the join screen with it, once.
        String launchCode = System.getProperty("reignofnether.join", "");
        if (!joinLaunched && launchCode.matches("[A-Za-z0-9]{8}")) {
            joinLaunched = true;
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new JoinScreen(this, launchCode)));
        }

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

    /**
     * Sizes the design in GUI pixels, like vanilla menus: a 50px row of the design is a 20px vanilla button,
     * smaller only when the window can't fit it.
     */
    @Unique
    private void layout() {
        float sw = this.width, sh = this.height;
        this.ronScale = Math.min(0.4f, Math.min(sw / 820f, sh / 760f));
        float s = this.ronScale;
        this.logoW = Math.round(Math.min(LOGO_W * s, sw * 0.7f));
        this.logoH = this.logoW * LOGO_TEX_H / LOGO_TEX_W;
        this.logoX = Math.round((sw - this.logoW) / 2);
        this.logoY = Math.round(Math.max(sh * 0.07f, 88 * s));
        this.gridX = Math.round((sw - GRID_W * s) / 2);
        this.gridY = this.logoY + this.logoH + Math.round(sh * 0.04f);
    }

    /** A grid cell {x, y, w, h}: columns square | wide | wide | square, row 0 is the tall one. */
    @Unique
    private int[] cell(int col, int span, int row) {
        int wide = (GRID_W - 2 * SQ - 3 * GAP) / 2;
        int[] colX = { 0, SQ + GAP, SQ + 2 * GAP + wide, SQ + 3 * GAP + 2 * wide };
        int[] colW = { SQ, wide, wide, SQ };
        int top = row == 0 ? 0 : HERO_H + GAP + (row - 1) * (ROW_H + GAP);
        int bottom = top + (row == 0 ? HERO_H : ROW_H);
        int last = col + span - 1;
        int x = this.gridX + Math.round(colX[col] * ronScale);
        int y = this.gridY + Math.round(top * ronScale);
        int w = this.gridX + Math.round((colX[last] + colW[last]) * ronScale) - x;
        int h = this.gridY + Math.round(bottom * ronScale) - y;
        if (row > 0 && span == 1 && (col == 0 || col == 3)) { // side squares stay square, against the grid
            if (col == 0)
                x += w - h;
            w = h;
        }
        return new int[]{ x, y, w, h };
    }

    /** The design's grid button: fill, one-unit top highlight and bottom shade, a dark ring. */
    @Unique
    private static void flat(GuiGraphics g, int[] u, int fill) {
        int x = u[0], y = u[1], w = u[2], h = u[3];
        Theme.outline(g, x, y, w, h, 1, 0xFF14131A);
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, 0x17FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x8C000000);
    }

    /** The grid is laid out in GUI coordinates. */
    @Unique
    private static int[] gui(int[] u) {
        return u;
    }

    /** Pixelify scale for a font size of the design. */
    @Unique
    private float fontScale(int designPx) {
        return designPx * this.ronScale / 10f;
    }

    /** A grid button; draw gets (graphics, hovered). */
    @Unique
    private AbstractWidget widget(int[] u, Component message, Runnable press, BiConsumer<GuiGraphics, Boolean> draw) {
        int[] r = gui(u);
        return new AbstractWidget(r[0], r[1], r[2], r[3], message) {
            @Override
            public void onClick(double pMouseX, double pMouseY) {
                press.run();
            }
            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
            @Override
            public void renderWidget(GuiGraphics g, int pMouseX, int pMouseY, float pPartialTick) {
                boolean hov = this.active && isHoveredOrFocused();
                draw.accept(g, hov);
            }
        };
    }

    /** The design's text button in place of a vanilla one, which stays hidden for other mods to find. */
    @Unique
    private void gridButton(Button vanilla, int[] u, Runnable press, boolean quit) {
        String label = plain(vanilla.getMessage());
        AbstractWidget button = widget(u, Component.literal(label), press, (g, hov) -> {
            if (quit && hov)
                flat(g, u, 0xFFA8321E);
            else
                flat(g, u, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);

            int colour = quit && hov ? 0xFFFFFFFF : hov ? Theme.GOLD : Theme.BODY;
            Text.draw(g, label, u[0] + (u[2] - Text.width(label)) / 2, u[1] + (u[3] - 8) / 2, colour);
        });
        button.active = vanilla.active;
        button.setTooltip(vanilla.getTooltip());
        vanilla.visible = false;
        square(vanilla, u); // Forge's mod update icon is drawn at the Mods button: keep it on ours
        this.addRenderableWidget(button);
    }

    /** A side square in place of a vanilla icon button (language, accessibility), which stays hidden. */
    @Unique
    private void sideButton(Button vanilla, int[] u, ResourceLocation icon, int size) { // size: the art comes out 12 visible
        AbstractWidget button = widget(u, vanilla.getMessage(), vanilla::onPress, (g, hov) -> {
            flat(g, u, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);
            g.blit(icon, u[0] + (u[2] - size) / 2, u[1] + (u[3] - size) / 2, size, size, 0, 0, 20, 20, 20, 20);
        });
        button.setTooltip(vanilla.getTooltip());
        vanilla.visible = false;
        square(vanilla, u);
        this.addRenderableWidget(button);
    }

    /** Moves a vanilla widget onto a grid cell (language and accessibility squares, the hidden text buttons). */
    @Unique
    private static void square(AbstractWidget w, int[] u) {
        int[] r = gui(u);
        w.setPosition(r[0], r[1]);
        w.setWidth(r[2]);
        w.setHeight(r[3]);
    }

    /** The design's label font (Silkscreen capitals) at full size. */
    @Unique
    private static Component silk(String s) {
        return Component.literal(s.toUpperCase(java.util.Locale.ROOT)).withStyle(Text.SMALL_STYLE);
    }

    @Unique
    private static String plain(Component label) {
        return label.getString().replace("...", "").replace("…", "");
    }

    @Unique
    private void openWorldSelect() {
        Minecraft.getInstance().setScreen(new WorldSelectScreen(this));
    }

    @Unique
    private void openMapPack() {
        Minecraft.getInstance().setScreen(new MapPackScreen(this));
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

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY,
                        float pPartialTick, CallbackInfo ci) {
        ci.cancel();
        // Handle fade-in effect
        if (this.fadeInStart == 0L && this.fading) {
            this.fadeInStart = Util.getMillis();
        }
        float fadeProgress = this.fading
                ? (float) (Util.getMillis() - this.fadeInStart) / 1000.0F
                : 1.0F;

        // the nether image at 66% brightness, dark at the top and darker still at the bottom
        Backdrop.image(guiGraphics, this.width, this.height);
        guiGraphics.fill(0, 0, this.width, this.height, 0x800D0305);
        int h = this.height;
        guiGraphics.fillGradient(0, 0, this.width, h * 24 / 100, 0xB30D0305, 0x000D0305);
        guiGraphics.fillGradient(0, h * 24 / 100, this.width, h / 2, 0x000D0305, 0x330D0305);
        guiGraphics.fillGradient(0, h / 2, this.width, h, 0x330D0305, 0xE60D0305);

        float alpha = this.fading ? Mth.clamp(fadeProgress - 1.0F, 0.0F, 1.0F) : 1.0F;
        int alphaMask = Mth.ceil(alpha * 255.0F) << 24;

        if ((alphaMask & -67108864) != 0) {
            RenderSystem.enableBlend();
            ForgeHooksClient.renderMainMenu((TitleScreen) (Object) this,
                    guiGraphics, this.font, this.width, this.height, alphaMask);

            int unitsH = Math.round(this.height / Text.guiPerUnit());
            renderLogo(guiGraphics, alpha);
            Text.atGui(guiGraphics, 0, 0, () -> {
                renderAccount(guiGraphics, Math.round(this.width / Text.guiPerUnit()));

                // Branding, small like the design: RoN's version, then Forge, Minecraft and the mod count on one line
                List<String> brand = new ArrayList<>();
                BrandingControl.forEachLine(true, true, (line, text) -> {
                    if (line != 1) // vanilla's second line from the bottom is where RoN's version went
                        brand.add(0, text.toUpperCase());
                });
                guiGraphics.drawString(font, silk("REIGN OF NETHER " + ReignOfNether.VERSION_STRING), 14, unitsH - 34, Theme.withAlpha(Theme.ORANGE, alpha), false);
                guiGraphics.drawString(font, silk(String.join(" · ", brand)), 14, unitsH - 20, Theme.withAlpha(Theme.DIM, alpha), false);
            });

            for (GuiEventListener child : this.children()) {
                if (child instanceof AbstractWidget widget) {
                    widget.setAlpha(alpha);
                }
            }

            super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);

            // icon buttons get the RTS tooltip rather than vanilla's
            String tip = mapPackButton.isHovered() ? mapPackButton.getMessage().getString()
                    : discordButton.isHovered() ? discordButton.getMessage().getString()
                    : websiteButton.isHovered() ? websiteButton.getMessage().getString() : null;
            if (tip != null)
                MyRenderer.renderTooltip(guiGraphics, Tooltip.of(tip), pMouseX, pMouseY);

            if (alpha >= 1.0F && this.modUpdateNotification != null) {
                this.modUpdateNotification.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            RenderSystem.disableBlend();
        }
    }

    /** The RoN logo (its art has its own glow and shadow), and the splash leaning off its bottom right. */
    @Unique
    private void renderLogo(GuiGraphics g, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        blitLogo(g, logoX, logoY);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        String splash = TitleClientEvents.splash;
        if (splash == null)
            return;
        float scale = fontScale(18); // the design's smallest splash size
        int tw = Text.titleWidth(splash, scale), th = Text.titleHeight(scale);
        float pulse = 1f + 0.035f * (1f - Mth.cos((float) (Util.getMillis() % 600L) / 600f * Mth.TWO_PI));
        PoseStack pose = g.pose();
        pose.pushPose();
        // its right edge 4% past the logo, its bottom 10% above the logo's, turned about its centre
        pose.translate(logoX + logoW * 1.04f - tw / 2f, logoY + logoH * 0.9f - th / 2f, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-18f));
        pose.scale(pulse, pulse, 1);
        Text.title(g, splash, -tw / 2, -th / 2, scale, GOLD, Theme.ON_ORANGE);
        pose.popPose();
    }

    @Unique
    private void blitLogo(GuiGraphics g, int x, int y) {
        g.blit(LOGO, x, y, logoW, logoH, 0, 0, LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
    }

    /** The account chip's width, in HUD units. */
    @Unique
    private static int accountWidth() {
        return 3 + 18 + 6 + Math.max(Text.width(Account.name()), Minecraft.getInstance().font.width(silk(Account.kind()))) + 8;
    }

    /** Top right: the signed-in player's head, name and account type. */
    @Unique
    private void renderAccount(GuiGraphics g, int screenW) {
        String name = Account.name(), kind = Account.kind();
        int head = 18, pad = 3, gap = 6;
        int w = accountWidth();
        int h = head + 2 * pad;
        int x = screenW - 14 - w, y = 10;
        g.fill(x, y, x + w, y + h, 0xB30D0305);
        g.fill(x, y, x + w, y + 1, 0x0DFFFFFF);
        Theme.outline(g, x + pad, y + pad, head, head, 1, Theme.INK);
        PlayerFaceRenderer.draw(g, Account.skin(), x + pad, y + pad, head);
        Text.draw(g, name, x + pad + head + gap, y + pad + 1, Theme.TEXT);
        g.drawString(Minecraft.getInstance().font, silk(kind), x + pad + head + gap, y + pad + 11, 0xFF575A82, false);
    }
}
