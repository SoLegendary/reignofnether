package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.attackwarnings.AttackWarningClientEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.config.ConfigClientEvents;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.matchstart.MatchEndClientEvents;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.buttons.HelperButtons;
import com.solegendary.reignofnether.hud.buttons.StartButtons;
import com.solegendary.reignofnether.hud.custombutton.CustomButtonClientEvents;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.scenario.ScenarioClientEvents;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-right of the HUD: the utility bar (idle workers, army, players, chat), a second row with the
 * buttons that only exist in some situations (match start, scenario, beacon, game rules,
 * datapack buttons), the clock to the left of the bar and the attack alert under it.
 */
public final class TopRightPanel {

    private TopRightPanel() { }

    private static final int BTN = HudLayout.CELL;
    private static final int GAP = 3;
    private static final int PAD = (HudLayout.TOP_CARD_H - BTN) / 2;

    static final ResourceLocation MOON = rl("textures/hud/moon.png");
    static final ResourceLocation BLOOD_MOON = rl("textures/hud/blood_moon.png");
    private static final ResourceLocation WARNING = rl("textures/hud/warning.png");
    static final ItemStack CLOCK = new ItemStack(Items.CLOCK);

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, path);
    }

    public static int bottom = 48; // where the free space under the panel starts, for the debug text

    public static void render(HudFrame f) {
        int right = f.layout.width() - HudLayout.MARGIN;
        int top = HudLayout.MARGIN;

        List<Button> main = new ArrayList<>();
        addIfShown(main, HelperButtons.idleWorkerButton);
        addIfShown(main, HelperButtons.armyButton);
        if (!SpectatorHud.isActive()) // the scoreboard already shows every player
            addIfShown(main, PlayerDisplayClientEvents.observerButton);
        addIfShown(main, PlayerDisplayClientEvents.diplomacyButton);
        addIfShown(main, TutorialClientEvents.helpButton);

        Rect bar = renderBar(f, main, right, top);
        int barLeft = bar.isEmpty() ? right : bar.x();
        // a fixed second row, under the clock, so the contextual buttons don't jump as the main ones come and go
        int below = top + HudLayout.TOP_CARD_H + HudLayout.GAP;

        // spectators have the clock in their scoreboard
        if (TutorialClientEvents.isAtOrPastStage(TutorialStage.MINIMAP_CLICK) && !SpectatorHud.isActive())
            renderClock(f, barLeft - (bar.isEmpty() ? 0 : HudLayout.GAP), top);

        Rect contextBar = renderBar(f, contextButtons(), right, below);
        if (!contextBar.isEmpty())
            below = contextBar.bottom() + HudLayout.GAP;
        bottom = renderAlert(f, right, below);
    }

    private static void addIfShown(List<Button> list, Button b) {
        if (b != null && !b.isHidden.get())
            list.add(b);
    }

    /** Draws buttons right-aligned at (right, top) in one panel. Returns the panel rectangle. */
    private static Rect renderBar(HudFrame f, List<Button> buttons, int right, int top) {
        if (buttons.isEmpty())
            return Rect.EMPTY;
        int w = PAD * 2 + buttons.size() * BTN + (buttons.size() - 1) * GAP;
        int h = PAD * 2 + BTN;
        int x = right - w;
        Theme.panel(f.g, x, top, w, h, Theme.floating());
        f.zone(x, top, w, h);
        int bx = x + PAD;
        for (Button b : buttons) {
            String key = b.hotkey != null ? b.hotkey.getCurrentLabel() : null;
            Rect cell = new Rect(bx, top + PAD, BTN, BTN);
            ButtonCell.render(f.g, b, bx, top + PAD, BTN, key, f.hovered(cell));
            f.register(b, cell, b.hotkey, () -> ButtonCell.tooltipFor(b, key));
            bx += BTN + GAP;
        }
        // badges last, so the next button never covers them
        int idle = buttons.indexOf(HelperButtons.idleWorkerButton);
        if (idle >= 0)
            Theme.badge(f.g, String.valueOf(UnitClientEvents.idleWorkerIds.size()), x + PAD + idle * (BTN + GAP) + BTN + 3, top + PAD - 3);
        return new Rect(x, top, w, h);
    }

    /** Buttons that only appear in some game states, in a second row under the utility bar. */
    private static List<Button> contextButtons() {
        List<Button> list = new ArrayList<>();
        addIfShown(list, MatchEndClientEvents.getResultsButton());
        if (!PlayerClientEvents.isRTSPlayer() && !PlayerClientEvents.rtsLocked) {
            if (GameruleClient.scenarioMode) {
                Button cycleRole = ScenarioClientEvents.getCycleRoleToPlayButton();
                if (cycleRole != null)
                    list.add(cycleRole);
                list.add(ScenarioClientEvents.getScenarioStartButton());
            } else {
                addIfShown(list, ConfigClientEvents.getDiffsButton());
                if (ClientGameModeHelper.gameMode != GameMode.SANDBOX) {
                    if (!StartPosClientEvents.isEnabled()) {
                        addIfShown(list, StartButtons.villagerStartButton);
                        addIfShown(list, StartButtons.monsterStartButton);
                        addIfShown(list, StartButtons.piglinStartButton);
                    }
                } else {
                    addIfShown(list, StartButtons.sandboxStartButton);
                }
                Button gamemode = ClientGameModeHelper.getButton();
                if (gamemode != null && !TutorialClientEvents.isEnabled())
                    addIfShown(list, gamemode);
            }
        } else if (SurvivalClientEvents.isEnabled) {
            addIfShown(list, SurvivalClientEvents.getNextWaveButton());
        }

        BeaconPlacement beacon = BuildingUtils.getBeacon(true);
        if (beacon != null)
            addIfShown(list, HelperButtons.getBeaconButton(beacon.ownerName));

        for (ResourceLocation id : CustomButtonClientEvents.alwaysRenderButtons)
            addIfShown(list, CustomButtonClientEvents.getButton(id));
        if (HudClientEvents.hudSelectedEntity != null) {
            var ids = CustomButtonClientEvents.entityMappings.get(HudClientEvents.hudSelectedEntity.getType());
            if (ids != null)
                for (ResourceLocation id : ids)
                    addIfShown(list, CustomButtonClientEvents.getButton(id));
        }
        if (HudClientEvents.hudSelectedPlacement != null) {
            var ids = CustomButtonClientEvents.buildingMappings.get(HudClientEvents.hudSelectedPlacement.getBuilding());
            if (ids != null)
                for (ResourceLocation id : ids)
                    addIfShown(list, CustomButtonClientEvents.getButton(id));
        }
        return list;
    }

    // ------------------------------------------------------------------ clock

    private static void renderClock(HudFrame f, int right, int top) {
        long time = TimeClientEvents.serverNormDayTime;
        boolean bloodMoon = TimeClientEvents.isBloodMoonActive();
        boolean day = TimeUtils.isDay(time);
        boolean showGameTime = PlayerClientEvents.isRTSPlayer() && !SurvivalClientEvents.isEnabled;

        String phase = bloodMoon ? I18n.get("hud.reignofnether.clock.blood_moon")
                : I18n.get(day ? "hud.reignofnether.clock.day" : "hud.reignofnether.clock.night") + " " + TimeClientEvents.dayNumber();
        String until = bloodMoon ? TimeUtils.getTimeStrFromTicks(TimeClientEvents.getBloodMoonTicksLeft())
                : I18n.get(day ? "hud.reignofnether.clock.dusk_in" : "hud.reignofnether.clock.dawn_in",
                        TimeUtils.getTimeUntilStr(time, day ? TimeUtils.DUSK : TimeUtils.DAWN));
        String gameTime = TimeUtils.getTimeStrFromTicks(PlayerClientEvents.rtsGameTicks);

        int phaseColour = bloodMoon ? Theme.ERROR : day ? Theme.GOLD : Theme.MANA_TEXT;
        float big = Text.px(20);
        int barW = Math.max(80, Text.bigWidth(phase, big) + Text.smallWidth(until) + 10);
        int timeW = showGameTime ? Math.max(Text.bigWidth(gameTime, big), Text.smallWidth(I18n.get("hud.reignofnether.clock.game_time"))) + 12 : 0;
        int w = 7 + 15 + 7 + barW + timeW + 7;
        int h = HudLayout.TOP_CARD_H;
        int x = right - w;
        Rect r = new Rect(x, top, w, h);
        Theme.panel(f.g, x, top, w, h, Theme.floating());
        if (f.hovered(r))
            f.g.fill(x, top, x + w, top + h, Theme.HOVER);

        int cy = top + 2; // content offsets are from a 32-high card; this centres them in TOP_CARD_H
        int ix = x + 7;
        if (bloodMoon)
            MyRenderer.renderIcon(f.g, BLOOD_MOON, ix, cy + 8, 15);
        else if (day)
            MyRenderer.renderItem(f.g, CLOCK, ix - 1, cy + 7, 15 / 16f);
        else
            MyRenderer.renderIcon(f.g, MOON, ix, cy + 8, 15);

        int tx = ix + 15 + 7;
        Text.big(f.g, phase, tx, cy + 6, phaseColour, big);
        Text.smallRight(f.g, until, tx + barW, cy + 7, Theme.MUTED);
        Theme.bar(f.g, tx, cy + 22, barW, 3, phaseProgress(time, day, bloodMoon), phaseColour);

        if (showGameTime) {
            int gx = tx + barW + 6;
            Theme.divider(f.g, gx, top + 4, 1, h - 8);
            int mid = gx + timeW / 2;
            Text.smallCentered(f.g, I18n.get("hud.reignofnether.clock.game_time"), mid, cy + 4, Theme.DIM);
            Text.bigCentered(f.g, gameTime, mid, cy + 15, Theme.TEXT, big);
        }

        f.zone(r);
        Button clock = new ButtonBuilder("Clock")
                .onRightClick(TimeClientEvents::centreOnBloodMoon)
                .build();
        f.register(clock, r, null, TimeClientEvents::clockTooltip);
    }

    private static float phaseProgress(long time, boolean day, boolean bloodMoon) {
        if (bloodMoon)
            return 1f;
        long t = TimeUtils.normaliseTime(time);
        long start = day ? TimeUtils.DAWN : TimeUtils.DUSK;
        long elapsed = (t - start + 24000) % 24000;
        return elapsed / 12000f;
    }

    // ------------------------------------------------------------------ alert

    /** Returns the y under it. */
    private static int renderAlert(HudFrame f, int right, int top) {
        Button warning = AttackWarningClientEvents.getWarningButton();
        if (warning.isHidden.get())
            return top;
        String title = I18n.get("hud.reignofnether.under_attack");
        String hint = I18n.get("hud.reignofnether.under_attack_hint");
        int w = Math.max(150, 7 + 16 + 7 + Math.max(Text.mediumWidth(title), Text.width(hint)) + 30);
        int h = HudLayout.TOP_CARD_H;
        int x = right - w;
        Rect r = new Rect(x, top, w, h);
        boolean flash = warning.isSelected.get();
        Theme.popup(f.g, x, top, w, h, flash ? Theme.ERROR : Theme.DANGER_TEXT);
        if (flash)
            Theme.outline(f.g, x - 1, top - 1, w + 2, h + 2, 1, Theme.ALARM_GLOW);
        int cy = top + 2; // centred in TOP_CARD_H, like the clock
        MyRenderer.renderIcon(f.g, WARNING, x + 7, cy + 8, 16);
        Text.medium(f.g, title, x + 30, cy + 5, Theme.ERROR);
        Text.draw(f.g, hint, x + 30, cy + 17, Theme.LABEL);
        f.zone(r);
        f.register(warning, r, Keybindings.jumpToAlert, () -> Tooltip.fromLines(warning.tooltipLines).key(Keybindings.jumpToAlert.getCurrentLabel()));
        return top + h + HudLayout.GAP;
    }
}
