package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.replay.ReplayClientEvents;
import com.solegendary.reignofnether.resources.IncomeTracker;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Caster view for spectators: a scoreboard at the top and a small tab bar at the bottom that opens one
 * panel at a time (army, economy, camera). The rest of the screen is the battlefield. Client-side only:
 * it shows what the server already sends to spectators.
 */
public final class SpectatorHud {

    private SpectatorHud() { }

    private static final Minecraft MC = Minecraft.getInstance();

    private static boolean armyOpen = false;
    @Nullable private static String follow = null;
    private static double followX, followZ;

    private static final int ROW_H = HudLayout.TOP_CARD_H; // a row is the player's own resource bar
    private static final int NAME_W = 62;
    private static int SIDE_W = 310; // set each frame from the resource columns
    private static final float VALUE = Text.px(20); // medium values: the scoreboard is a summary
    private static int[] cols; // resource cell widths shared by every row, so the columns line up
    private static final int MID_W = 104;
    private static final int TAB_H = 18;
    private static final int CARD_W = 112;

    public static void reset() {
        armyOpen = false;
        follow = null;
    }

    private static int bottom = 0;

    /** Bottom edge of the scoreboard as last drawn, so panels below it can clear it. */
    public static int bottom() {
        return bottom;
    }

    public static boolean isActive() {
        return !PlayerClientEvents.isRTSPlayer() && !PlayerClientEvents.rtsPlayers.isEmpty()
                && !TutorialClientEvents.isEnabled() && !SandboxClientEvents.isSandboxPlayer();
    }

    public static void render(HudFrame f) {
        List<RTSPlayer> players = orderedPlayers();
        if (players.isEmpty())
            return;
        renderScoreboard(f, players);
        renderArmy(f, players);
    }

    /** Allies next to each other, so the first half of the list is one side of the scoreboard. */
    private static List<RTSPlayer> orderedPlayers() {
        List<RTSPlayer> left;
        synchronized (PlayerClientEvents.rtsPlayers) {
            left = new ArrayList<>(PlayerClientEvents.rtsPlayers);
        }
        List<RTSPlayer> out = new ArrayList<>();
        while (!left.isEmpty()) {
            RTSPlayer first = left.remove(0);
            out.add(first);
            left.removeIf(p -> {
                if (AlliancesClient.isAllied(first.name, p.name)) {
                    out.add(p);
                    return true;
                }
                return false;
            });
        }
        return out;
    }

    // ------------------------------------------------------------------ scoreboard

    private static void renderScoreboard(HudFrame f, List<RTSPlayer> players) {
        cols = null;
        for (RTSPlayer p : players) {
            Resources res = ResourcesClientEvents.getResources(p.name);
            if (res == null)
                continue;
            int[] w = TopLeftPanel.resourceWidths(p.name, res, VALUE);
            if (cols == null || cols.length != w.length)
                cols = w;
            else
                for (int i = 0; i < w.length; i++)
                    cols[i] = Math.max(cols[i], w[i]);
        }
        int widest = 0;
        if (cols != null)
            for (int w : cols)
                widest += w;
        SIDE_W = 7 + 14 + 5 + NAME_W + 6 + widest;
        int leftCount = (players.size() + 1) / 2;
        int rows = Math.max(1, leftCount);
        int w = SIDE_W * 2 + MID_W;
        int h = Math.max(rows * ROW_H, 38); // room for the clock, day/night and mode lines
        int rowsTop = (h - rows * ROW_H) / 2;
        int x = (f.layout.width() - w) / 2;
        int y = HudLayout.MARGIN;
        Rect r = new Rect(x, y, w, h);
        Theme.panel(f.g, x, y, w, h, Theme.floating());
        f.zone(r);
        bottom = r.bottom();

        for (int i = 0; i < players.size(); i++) {
            boolean leftSide = i < leftCount;
            int row = leftSide ? i : i - leftCount;
            int px = leftSide ? x : x + SIDE_W + MID_W;
            playerRow(f, players.get(i), px, y + rowsTop + row * ROW_H, leftSide);
        }

        // the clock in the middle
        int mx = x + SIDE_W;
        Theme.divider(f.g, mx, y + 3, 1, h - 6);
        Theme.divider(f.g, mx + MID_W - 1, y + 3, 1, h - 6);
        int cx = mx + MID_W / 2;
        float big = Text.px(30);
        String time = TimeUtils.getTimeStrFromTicks(PlayerClientEvents.rtsGameTicks);
        long dayTime = TimeClientEvents.serverNormDayTime;
        boolean day = TimeUtils.isDay(dayTime);
        boolean bloodMoon = TimeClientEvents.isBloodMoonActive();
        // the clock icon beside the game time, like the player's clock
        int tw = 12 + 4 + Text.bigWidth(time, big);
        int tx = cx - tw / 2;
        int iy = y + 3 + (Text.bigHeight(big) - 12) / 2;
        if (bloodMoon)
            MyRenderer.renderIcon(f.g, TopRightPanel.BLOOD_MOON, tx, iy, 12);
        else if (day)
            MyRenderer.renderItem(f.g, TopRightPanel.CLOCK, tx - 1, iy - 1, 14 / 16f);
        else
            MyRenderer.renderIcon(f.g, TopRightPanel.MOON, tx, iy, 12);
        Text.big(f.g, time, tx + 16, y + 3, Theme.TEXT, big);
        String phase = bloodMoon ? I18n.get("hud.reignofnether.clock.blood_moon")
                : I18n.get(day ? "hud.reignofnether.clock.day" : "hud.reignofnether.clock.night");
        String until = bloodMoon ? "" : TimeUtils.getTimeUntilStr(dayTime, day ? TimeUtils.DUSK : TimeUtils.DAWN);
        Text.mediumCentered(f.g, phase + (until.isEmpty() ? "" : " · " + until), cx, y + Text.bigHeight(big) + 5,
                bloodMoon ? Theme.ERROR : day ? Theme.GOLD : Theme.MANA_TEXT);
        Text.mediumCentered(f.g, modeLabel(players), cx, y + h - 11, Theme.LABEL);
    }

    /** 1V1, 2V2, 3V3, 2V1 or FFA, from the alliances (orderedPlayers puts allies next to each other). */
    private static String modeLabel(List<RTSPlayer> players) {
        List<Integer> teams = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (i > 0 && AlliancesClient.isAllied(players.get(i - 1).name, players.get(i).name))
                teams.set(teams.size() - 1, teams.get(teams.size() - 1) + 1);
            else
                teams.add(1);
        }
        if (teams.size() > 2 && teams.stream().allMatch(n -> n == 1))
            return I18n.get("hud.reignofnether.spectator.ffa", players.size());
        StringBuilder sb = new StringBuilder();
        for (int n : teams)
            sb.append(sb.isEmpty() ? "" : "V").append(n);
        return sb.toString();
    }

    /** Colour strip, head, name and faction, then food, wood, ore and population; mirrored on the right. */
    private static void playerRow(HudFrame f, RTSPlayer p, int x, int y, boolean leftSide) {
        int colour = 0xFF000000 | PlayerColors.getPlayerColorHex(p.name);
        int stripX = leftSide ? x : x + SIDE_W - 3;
        f.g.fill(stripX, y + 2, stripX + 3, y + ROW_H - 2, colour);

        int headX = leftSide ? x + 7 : x + SIDE_W - 7 - 14;
        head(f, p, headX, y + (ROW_H - 14) / 2, 14);

        int nameW = NAME_W;
        int nameX = leftSide ? headX + 14 + 5 : headX - 5 - nameW;
        String name = Text.fit(p.name, nameW);
        String faction = p.faction != null && !p.faction.getName().isEmpty() ? MiscUtil.getFactionName(p.faction) : "";
        if (leftSide) {
            Text.draw(f.g, name, nameX, y + 3, Theme.TEXT);
            Text.small(f.g, faction, nameX, y + 13, Theme.MUTED);
        } else {
            Text.draw(f.g, name, nameX + nameW - Text.width(name), y + 3, Theme.TEXT);
            Text.smallRight(f.g, faction, nameX + nameW, y + 13, Theme.MUTED);
        }

        // the same cells as the player's own bar: value, income per minute and workers, divided
        Resources res = ResourcesClientEvents.getResources(p.name);
        Rect bar = null;
        if (res != null) {
            int barW = 0;
            for (int w : cols)
                barW += w;
            int bx = leftSide ? nameX + nameW + 6 : nameX - 6 - barW;
            Theme.divider(f.g, leftSide ? bx - 1 : bx + barW, y + 3, 1, ROW_H - 6);
            bar = TopLeftPanel.renderResources(f, bx, y, p.name, res, false, VALUE, cols);
        }

        Rect row = new Rect(x, y, SIDE_W, ROW_H);
        Button b = new ButtonBuilder(p.name)
                .onLeftClick(() -> FogOfWarClientEvents.toggleSpectatorView(p.name))
                .onRightClick(() -> startFollowing(p.name)).build();
        Rect cells = bar;
        // over the resource cells their own tooltip (income, spending, workers) shows instead
        f.register(b, row, null, () -> cells != null && f.hovered(cells) ? null : Tooltip.of(p.name)
                .status(I18n.get("hud.reignofnether.spectator.click_view"), Theme.MUTED)
                .status(I18n.get("hud.reignofnether.spectator.click_follow"), Theme.MUTED));
        if (FogOfWarClientEvents.isViewing(p.name))
            Theme.ring(f.g, x, y, SIDE_W, ROW_H, 1, Theme.OK);
        if (p.name.equals(follow))
            Theme.ring(f.g, x + 1, y + 1, SIDE_W - 2, ROW_H - 2, 1, Theme.GOLD);
    }

    private static void head(HudFrame f, RTSPlayer p, int x, int y, int size) {
        Theme.outline(f.g, x, y, size, size, 1, Theme.INK);
        AbstractClientPlayer player = playerEntity(p.name);
        if (player != null && player.isSkinLoaded()) {
            ResourceLocation skin = player.getSkinTextureLocation();
            f.g.blit(skin, x, y, size, size, 8f, 8f, 8, 8, 64, 64);
            f.g.blit(skin, x, y, size, size, 40f, 8f, 8, 8, 64, 64);
        } else if (p.faction != null && p.faction.icon != null) {
            MyRenderer.renderIcon(f.g, p.faction.icon, x, y, size);
        }
    }

    @Nullable
    private static AbstractClientPlayer playerEntity(String name) {
        if (MC.level == null)
            return null;
        for (AbstractClientPlayer pl : MC.level.players())
            if (pl.getName().getString().equals(name))
                return pl;
        return null;
    }

    // ------------------------------------------------------------------ army

    private static final ResourceLocation ARMY_ICON = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/sword_and_bow.png");

    private record Army(int value, @Nullable HeroUnit hero, Map<String, int[]> units, Map<String, ResourceLocation> icons) { }

    /** Units this client has loaded; the server only sends far units' owner and population, never their type. */
    private static Army army(String owner) {
        long tick = System.currentTimeMillis() / 50; // scanned once a tick, not every frame
        if (tick != armyTick) {
            armyCache.clear();
            armyTick = tick;
        }
        return armyCache.computeIfAbsent(owner, SpectatorHud::scanArmy);
    }

    private static final Map<String, Army> armyCache = new HashMap<>();
    private static long armyTick = -1;

    private static Army scanArmy(String owner) {
        int value = 0;
        HeroUnit hero = null;
        Map<String, int[]> units = new LinkedHashMap<>();
        Map<String, ResourceLocation> icons = new LinkedHashMap<>();
        for (LivingEntity e : UnitClientEvents.getAllUnits()) {
            if (!(e instanceof Unit u) || e instanceof WorkerUnit || !owner.equals(u.getOwnerName()))
                continue;
            if (e instanceof HeroUnit h && hero == null)
                hero = h;
            var cost = u.getCost();
            if (cost != null)
                value += cost.food + cost.wood + cost.ore;
            if (e instanceof HeroUnit)
                continue; // the hero has its own line
            String name = SelectionPanel.displayName(e);
            units.computeIfAbsent(name, k -> new int[1])[0]++;
            icons.putIfAbsent(name, MiscUtil.mobHeadIcon(e));
        }
        return new Army(value, hero, units, icons);
    }

    private static final int UNIT = 16;
    private static final int UNITS_PER_ROW = 6;

    /**
     * Bottom centre: a small ARMY handle (key 1) that opens one card per player, left team on the left like
     * the scoreboard, under a bar of army value split by player colour.
     */
    private static void renderArmy(HudFrame f, List<RTSPlayer> players) {
        boolean selection = HudClientEvents.hudSelectedEntity != null || HudClientEvents.hudSelectedPlacement != null;
        int bottom = selection ? f.layout.console().y() - HudLayout.GAP : f.layout.height() - HudLayout.MARGIN;
        int pad = 4;
        int gap = 3;

        String label = I18n.get("hud.reignofnether.spectator.army");
        String key = Keybindings.nums[1].getCurrentLabel();
        int handleW = 9 + 4 + Text.smallWidth(label) + 6 + Text.smallWidth(key) + 3 + 10;
        int handleH = 14;

        List<Army> armies = new ArrayList<>();
        int unitRows = 1;
        int cardW = Math.min(CARD_W, (f.layout.width() - 2 * HudLayout.MARGIN - pad * 2 + gap) / Math.max(1, players.size()) - gap);
        int perRow = Math.max(1, (cardW - 6) / (UNIT + 1));
        if (armyOpen)
            for (RTSPlayer p : players) {
                Army a = army(p.name);
                armies.add(a);
                unitRows = Math.max(unitRows, Math.min(2, (a.units().size() + perRow - 1) / perRow));
            }
        int cardH = 3 + 10 + 13 + unitRows * (UNIT + 1) + 3;
        int cardsW = players.size() * (cardW + gap) - gap;
        int w = armyOpen ? cardsW + pad * 2 : handleW + pad * 2;
        int h = handleH + pad * 2 + (armyOpen ? cardH + gap : 0);
        int x = (f.layout.width() - w) / 2;
        int y = bottom - h;
        Theme.panel(f.g, x, y, w, h, Theme.floating());
        f.zone(x, y, w, h);

        // top row: the handle, then the army value bar beside it
        Rect hr = new Rect(x + pad, y + pad, handleW, handleH);
        Theme.cell(f.g, hr.x(), hr.y(), hr.w(), hr.h(), armyOpen ? Theme.CellState.ACTIVE : Theme.CellState.READY, f.hovered(hr));
        MyRenderer.renderIcon(f.g, ARMY_ICON, hr.x() + 5, hr.y() + (handleH - 9) / 2, 9);
        Text.small(f.g, label, hr.x() + 18, hr.y() + (handleH - 8) / 2, armyOpen ? Theme.GOLD : Theme.BODY);
        Theme.keyCap(f.g, key, hr.right() - Text.smallWidth(key) - 6, hr.y() + (handleH - 7) / 2, armyOpen ? Theme.GOLD : Theme.MUTED);
        f.register(new ButtonBuilder(label).onLeftClick(() -> armyOpen = !armyOpen).build(), hr, Keybindings.nums[1], null);
        if (!armyOpen)
            return;

        int[] values = armies.stream().mapToInt(Army::value).toArray();
        splitBar(f, players, values, hr.right() + gap, hr.y() + (handleH - 9) / 2, x + w - pad - hr.right() - gap);

        int cy = hr.bottom() + gap;
        for (int i = 0; i < players.size(); i++)
            armyCard(f, players.get(i), armies.get(i), x + pad + i * (cardW + gap), cy, cardW, cardH, perRow, unitRows);
    }

    /** Name and army value, the hero on one line (portrait, level, health, mana), then units by type. */
    private static void armyCard(HudFrame f, RTSPlayer p, Army a, int x, int y, int w, int h, int perRow, int rows) {
        int colour = 0xFF000000 | PlayerColors.getPlayerColorHex(p.name);
        Theme.card(f.g, x, y, w, h);
        f.g.fill(x, y, x + w, y + 1, colour);
        String value = String.valueOf(a.value());
        Text.draw(f.g, Text.fit(p.name, w - Text.smallWidth(value) - 12), x + 3, y + 3, Theme.TEXT);
        Text.smallRight(f.g, value, x + w - 3, y + 3, Theme.GOLD);

        int hy = y + 13;
        if (a.hero() instanceof LivingEntity hero) {
            ResourceLocation icon = MiscUtil.mobHeadIcon(hero);
            MyRenderer.renderIcon(f.g, icon, x + 3, hy - 1, 8);
            String lv = I18n.get("hud.reignofnether.spectator.hero_level", a.hero().getHeroLevel());
            Text.small(f.g, lv, x + 14, hy, Theme.GOLD);
            int bx = x + 14 + Text.smallWidth(lv) + 4;
            int bw = x + w - 3 - bx;
            float hp = hero.getHealth() / Math.max(1f, hero.getMaxHealth());
            Theme.bar(f.g, bx, hy + 1, bw, 4, hp, Theme.hpColor(hp));
            Theme.bar(f.g, bx, hy + 6, bw, 3, a.hero().getMana() / Math.max(1f, a.hero().getMaxMana()), Theme.MANA);
        } else {
            Text.small(f.g, I18n.get("hud.reignofnether.spectator.no_hero"), x + 3, hy + 1, Theme.DIM);
        }

        int uy = hy + 13;
        int i = 0;
        for (Map.Entry<String, int[]> unit : a.units().entrySet()) {
            if (i >= perRow * rows)
                break;
            int ux = x + 3 + (i % perRow) * (UNIT + 1);
            int uyy = uy + (i / perRow) * (UNIT + 1);
            f.g.fill(ux, uyy, ux + UNIT, uyy + UNIT, Theme.RAISED);
            MyRenderer.renderIcon(f.g, a.icons().get(unit.getKey()), ux, uyy, UNIT);
            String n = String.valueOf(unit.getValue()[0]);
            Text.smallShadowed(f.g, n, ux + UNIT - Text.smallWidth(n), uyy + UNIT - 7, Theme.TEXT);
            f.register(new ButtonBuilder(unit.getKey()).build(), new Rect(ux, uyy, UNIT, UNIT), null,
                    () -> Tooltip.of(unit.getKey() + " ×" + unit.getValue()[0]));
            i++;
        }
        if (i == 0)
            Text.small(f.g, I18n.get("hud.reignofnether.spectator.no_army"), x + 3, uy + 3, Theme.DIM);
    }

    /** One bar split by player colour, each part sized by its share and labelled with its value. */
    private static void splitBar(HudFrame f, List<RTSPlayer> players, int[] values, int x, int y, int w) {
        int total = 0;
        for (int v : values)
            total += v;
        int h = 9;
        f.g.fill(x, y, x + w, y + h, Theme.INK);
        if (total <= 0)
            return;
        int cx = x;
        for (int i = 0; i < values.length; i++) {
            int pw = i == values.length - 1 ? x + w - cx : Math.round(w * values[i] / (float) total);
            if (pw <= 0)
                continue;
            f.g.fill(cx, y, cx + pw, y + h, 0xFF000000 | PlayerColors.getPlayerColorHex(players.get(i).name));
            String label = String.valueOf(values[i]);
            if (Text.smallWidth(label) + 4 <= pw)
                Text.smallShadowed(f.g, label, cx + (pw - Text.smallWidth(label)) / 2, y, Theme.TEXT);
            cx += pw;
        }
    }

    // ------------------------------------------------------------------ camera follow

    private static void startFollowing(String name) {
        follow = name.equals(follow) ? null : name;
        if (follow != null && MC.player != null) {
            followX = MC.player.getX();
            followZ = MC.player.getZ();
        }
    }

    /**
     * A commander's camera is their player entity, so following them is copying its position every tick.
     * Panning yourself stops following.
     */
    public static void tick() {
        if (follow == null || MC.player == null || !OrthoviewClientEvents.isEnabled() || !isActive())
            return;
        if (Math.abs(MC.player.getX() - followX) > 0.5 || Math.abs(MC.player.getZ() - followZ) > 0.5) {
            follow = null;
            return;
        }
        // a replay has their real camera, zoom and rotation included
        float[] cam = ReplayClientEvents.camera(follow);
        if (cam != null) {
            MC.player.setPos(cam[0], MC.player.getY(), cam[1]);
            OrthoviewClientEvents.setCam(cam[2], cam[3]);
            followX = MC.player.getX();
            followZ = MC.player.getZ();
            return;
        }
        AbstractClientPlayer target = playerEntity(follow);
        if (target == null)
            return;
        MC.player.setPos(target.getX(), MC.player.getY(), target.getZ());
        followX = target.getX();
        followZ = target.getZ();
    }
}
