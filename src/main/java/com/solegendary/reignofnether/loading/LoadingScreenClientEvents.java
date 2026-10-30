package com.solegendary.reignofnether.loading;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.matchstart.MatchStartScreen;
import com.solegendary.reignofnether.minimap.MapGrid;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.minimap.StrategicMapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.rtsmap.RTSMapInfo;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoLoader;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.ui.Backdrop;
import com.solegendary.reignofnether.ui.Easing;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.WorldToScreen;
import com.mojang.math.Axis;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Draws over vanilla's loading, joining and saving screens instead of them, which keep doing the work underneath.
 * Each step plays out in turn, at least as long as its animation: the map paints in chunk by chunk from the top
 * corner, the navmesh and fog snapshot sweep across it the same way. It then glides into the match setup map, opens
 * into the satellite view, or dives into the world.
 */
public final class LoadingScreenClientEvents {

    private LoadingScreenClientEvents() { }

    private static final Minecraft MC = Minecraft.getInstance();

    // laid out on a fixed canvas scaled to the screen, so it looks the same on any screen
    private static final int VW = 640;
    private static final int VH = 360;
    private static final int MAP_X = 70, MAP_Y = 55, MAP_SIDE = 250;
    private static final int COL_X = MAP_X + MAP_SIDE + 36, COL_W = 214;

    private static final long FADE_IN_MS = 400;
    private static final long SETTLE_MS = 300;  // lets the screen that follows open before the map heads for it
    private static final long MOVE_MS = 1000;  // the map slides to the middle, or into the match setup map
    private static final long SAVED_FADE_MS = 250;
    private static final long ZOOM_MS = 1100;  // then grows into the satellite view or the world, fading out
    private static final long VEIL_FADE_MS = 600;
    private static final long RTS_VIEW_WAIT_MS = 4000;
    private static final long AFTER_DONE_MS = 1000;
    private static final int MAX_CELLS = 40;    // cells across the map: one a chunk, grouped on big maps
    // the least time each step is shown, so one animation finishes before the next starts
    private static final Map<String, Long> MIN_MS = Map.of("prepare", 700L, "connect", 700L, "map", 1800L, "receive", 1200L,
            "navmesh", 1600L, "fog", 1300L, "join", 800L, "save", 900L);
    private static final List<String> NO_END = List.of("prepare", "connect", "join", "save");
    private static final int TRACK = 0x1C1B23;

    private enum Mode { LOCAL, JOIN, SAVING }
    private enum State { PENDING, ACTIVE, DONE }
    private record Row(String key, State state, float frac, int total) { } // frac < 0: no known end

    @Nullable
    private static Mode mode = null;
    private static long shownMs = 0;
    private static long endMs = 0;   // when the last loading screen closed
    private static long morphMs = 0; // when the map set off for where it goes
    private static boolean rtsMap = false;
    private static boolean localInfoRead = false;
    private static boolean replay = false; // loading a replay: no map, and it waits for the recording to start
    private static String title = "";
    private static String byLine = "";      // the map's authors and version
    private static String description = "";
    @Nullable
    private static Rect target = null; // in canvas units: the match setup map, else the middle of the screen
    private static boolean zoom = false;      // after the slide, grow and fade into what's behind
    private static boolean satellite = false; // the satellite view was opened for the zoom, and closes after it
    private static float[] shown = new float[0];
    private static long[] since = new long[0];
    private static int[] totals = new int[0];
    private static long veilDoneMs = 0;
    @Nullable
    private static Runnable afterDone = null;
    @Nullable
    private static Runnable delayed = null; // afterDone, once played out, waiting AFTER_DONE_MS
    private static long delayedAt = 0;

    private static DynamicTexture texture = null;
    private static ResourceLocation textureLoc = null;
    private static int textureSize = 0;
    private static int uploadedRows = 0;
    private static long lastFrameMs = 0;

    // a dirt message counts only when it's saving or connecting: the others ("preparing world data" before create
    // world's options, reading a world) are a blink between menus, which keep their own background
    private static boolean isLoading(@Nullable Screen s) {
        return s instanceof LevelLoadingScreen || s instanceof ConnectScreen || s instanceof ReceivingLevelScreen
                || s instanceof ProgressScreen || s instanceof GenericDirtMessageScreen && (isSaving(s) || isConnecting(s));
    }

    private static boolean isSaving(Screen s) {
        return s instanceof GenericDirtMessageScreen && s.getTitle().getContents() instanceof TranslatableContents tc
                && tc.getKey().startsWith("menu.saving");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreenRender(ScreenEvent.Render.Pre evt) {
        Screen s = evt.getScreen();
        if (!isLoading(s))
            return;
        begin(s);
        evt.setCanceled(true);
        GuiGraphics g = evt.getGuiGraphics();
        draw(g, s);
        if (s instanceof ConnectScreen) // its cancel button still works, so it stays visible
            for (var child : s.children())
                if (child instanceof Renderable r)
                    r.render(g, evt.getMouseX(), evt.getMouseY(), evt.getPartialTick());
    }

    // over whatever opened next while it plays out
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onScreenRenderPost(ScreenEvent.Render.Post evt) {
        if (mode != null && !isLoading(evt.getScreen()))
            draw(evt.getGuiGraphics(), evt.getScreen());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderGui(RenderGuiEvent.Post evt) {
        if (mode != null && MC.screen == null)
            draw(evt.getGuiGraphics(), null);
    }

    private static boolean isConnecting(Screen s) {
        return s instanceof ConnectScreen || s instanceof GenericDirtMessageScreen
                && s.getTitle().getContents() instanceof TranslatableContents tc && tc.getKey().startsWith("connect.");
    }

    private static void begin(Screen s) {
        // our own world's screens (preparing world data, then loading) are local; only a connection is joining
        Mode wanted = isSaving(s) ? Mode.SAVING : isConnecting(s) || mode == Mode.JOIN && !(s instanceof LevelLoadingScreen)
                ? Mode.JOIN : Mode.LOCAL;
        if (mode == wanted && morphMs == 0) {
            endMs = 0; // another loading screen in the same load
            if (mode == Mode.LOCAL && !localInfoRead && MC.getSingleplayerServer() != null)
                readLocalInfo(); // began on "preparing world data", before the server existed
            return;
        }
        finish(false);
        mode = wanted;
        shownMs = System.currentTimeMillis();
        lastFrameMs = shownMs;
        title = "";
        byLine = "";
        description = "";
        rtsMap = false;
        replay = false;
        localInfoRead = false;
        if (mode == Mode.LOCAL && MC.getSingleplayerServer() != null) {
            readLocalInfo();
        } else if (mode == Mode.JOIN && MC.getCurrentServer() != null) {
            title = MC.getCurrentServer().name;
        }
        sizeRows();
    }

    private static void sizeRows() {
        int n = mode == Mode.SAVING ? 1 : mode == Mode.JOIN || replay ? 3 : rtsMap ? 5 : 2;
        shown = new float[n];
        since = new long[n];
        totals = new int[n];
    }

    // the world's name, its map info and whether it's an RTS map, once the server exists
    private static void readLocalInfo() {
        localInfoRead = true;
        var data = MC.getSingleplayerServer().getWorldData();
        title = data.getLevelName();
        try { // read here rather than waiting for the server, so the column never changes once shown
            RTSMapInfo info = RTSMapInfoLoader.load(MC.getSingleplayerServer().getWorldPath(LevelResource.ROOT));
            if (info != null)
                setMapInfo(info.getName(), info.getAuthor(), info.getVersion(), info.getDescription());
        } catch (RuntimeException e) {
            ReignOfNether.LOGGER.warn("Couldn't read the map info for the loading screen", e);
        }
        // known before the server starts, so the steps never change once shown
        WorldBorder.Settings border = data.overworldData().getWorldBorder();
        boolean wasRts = rtsMap;
        rtsMap = border.getSize() <= WorldBorderServerEvents.RTS_OPTIMIZED_BORDER;
        if (rtsMap != wasRts)
            sizeRows();
    }

    private static void setMapInfo(@Nullable String name, @Nullable Collection<String> authors, @Nullable String version, @Nullable String desc) {
        if (name != null && !name.isBlank())
            title = name;
        String by = authors == null || authors.isEmpty() ? "" : I18n.get("loading.reignofnether.by", String.join(", ", authors));
        String v = version == null || version.isBlank() ? "" : I18n.get("loading.reignofnether.version", version);
        byLine = by.isEmpty() ? v : v.isEmpty() ? by : by + " · " + v;
        description = desc == null ? "" : desc;
    }

    public static boolean isShowing() {
        return mode != null;
    }

    /** Runs once the whole animation has played out, or now if nothing is showing (the resume screen waits for it). */
    public static void whenDone(@Nullable Runnable action) {
        if (delayed != null) // still waiting out the moment after
            delayed = action;
        else if (mode == null && action != null)
            action.run();
        else
            afterDone = action;
    }

    /** @param completed it played out; otherwise a new load replaced it, and what was waiting is dropped */
    private static void finish(boolean completed) {
        Runnable after = afterDone;
        afterDone = null;
        mode = null;
        endMs = 0;
        morphMs = 0;
        target = null;
        zoom = false;
        if (satellite)
            StrategicMapClientEvents.close(); // the camera tilts back down to the normal view
        satellite = false;
        veilDoneMs = 0;
        uploadedRows = 0;
        if (textureLoc != null)
            MC.getTextureManager().release(textureLoc); // also closes the texture
        texture = null;
        textureLoc = null;
        textureSize = 0;
        // a moment after, so the camera has tilted back from the satellite view first
        if (completed && after != null) {
            delayed = after;
            delayedAt = System.currentTimeMillis() + AFTER_DONE_MS;
        } else if (!completed) {
            delayed = null;
        }
    }

    /** In a match we play in: our capitol, else our first building; null otherwise. */
    @Nullable
    private static BuildingPlacement ownBase() {
        if (MC.player == null || !PlayerClientEvents.isRTSPlayer())
            return null;
        String me = MC.player.getName().getString();
        BuildingPlacement base = null;
        for (BuildingPlacement b : BuildingClientEvents.getBuildings())
            if (me.equals(b.ownerName) && (base == null || b.isCapitol && !base.isCapitol))
                base = b;
        return base;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || delayed == null || System.currentTimeMillis() < delayedAt)
            return;
        Runnable run = delayed;
        delayed = null;
        if (MC.level != null)
            run.run();
    }

    @Nullable
    private static MapGrid grid() {
        if (mode == Mode.LOCAL)
            return LoadProgress.map;
        if (mode == Mode.JOIN)
            return MinimapClientEvents.getKnownMap();
        return null;
    }

    // ------------------------------------------------------------------ steps

    /** Where each step really is. */
    private static List<Row> realRows(@Nullable Screen s) {
        List<Row> rows = new ArrayList<>();
        boolean joining = s instanceof ReceivingLevelScreen || s instanceof ProgressScreen;
        boolean ended = endMs != 0;
        if (mode == Mode.SAVING) {
            rows.add(new Row("save", ended ? State.DONE : State.ACTIVE, -1, 0));
            return rows;
        }
        if (mode == Mode.JOIN) {
            boolean connecting = !joining && !ended;
            rows.add(new Row("connect", connecting ? State.ACTIVE : State.DONE, -1, 0));
            // a server on a map that isn't RTS-optimised sends none, so the step just passes
            MapGrid g = grid();
            float got = g == null ? 1 : g.rows / (float) g.size;
            rows.add(new Row("receive", connecting ? State.PENDING : got >= 1 || ended ? State.DONE : State.ACTIVE, got, 0));
            rows.add(new Row("join", ended ? State.DONE : connecting ? State.PENDING : State.ACTIVE, -1, 0));
            return rows;
        }
        LoadProgress.Step step = LoadProgress.step;
        boolean serverDone = !rtsMap || LoadProgress.finished;
        boolean preparing = (!rtsMap || step == null) && !joining && !ended;
        rows.add(new Row("prepare", preparing ? State.ACTIVE : State.DONE, -1, 0));
        if (rtsMap) {
            for (LoadProgress.Step st : LoadProgress.Step.values()) {
                State state = serverDone || ended || step != null && step.ordinal() > st.ordinal() ? State.DONE
                        : step == st ? State.ACTIVE : State.PENDING;
                float frac = state == State.DONE ? 1 : state == State.ACTIVE ? Math.min(1, LoadProgress.done / (float) LoadProgress.total) : 0;
                rows.add(new Row(st.name().toLowerCase(Locale.ROOT), state, frac, step == st ? LoadProgress.total : 0));
            }
        }
        rows.add(new Row("join", ended ? State.DONE : serverDone && !preparing || joining ? State.ACTIVE : State.PENDING, -1, 0));
        return rows;
    }

    /** What is shown: each step starts once the one before has finished showing, and fills no faster than its pace. */
    private static List<Row> shownRows(List<Row> real, float dt, long now) {
        List<Row> out = new ArrayList<>();
        boolean before = true;
        for (int i = 0; i < real.size() && i < shown.length; i++) {
            Row r = real.get(i);
            if (r.total > 0)
                totals[i] = r.total;
            if (!before || r.state == State.PENDING) {
                out.add(new Row(r.key, State.PENDING, 0, totals[i]));
                before = false;
                continue;
            }
            if (since[i] == 0)
                since[i] = now;
            long min = MIN_MS.getOrDefault(r.key, 800L);
            if (NO_END.contains(r.key)) {
                if (r.state == State.DONE && now - since[i] >= min)
                    shown[i] = 1;
            } else {
                shown[i] = Math.min(r.state == State.DONE ? 1 : r.frac, shown[i] + dt / min);
            }
            boolean done = shown[i] >= 1;
            out.add(new Row(r.key, done ? State.DONE : State.ACTIVE, NO_END.contains(r.key) ? -1 : shown[i], totals[i]));
            before = done;
        }
        return out;
    }

    private static float shownOf(List<Row> rows, String key) {
        for (Row r : rows)
            if (r.key.equals(key))
                return r.state == State.DONE ? 1 : r.state == State.ACTIVE ? Math.max(0, r.frac) : 0;
        return 0;
    }

    // ------------------------------------------------------------------ drawing

    private static void draw(GuiGraphics g, @Nullable Screen s) {
        if (mode == null)
            return;
        long now = System.currentTimeMillis();
        float dt = Math.min(100, now - lastFrameMs); // a long frame (the world appearing) doesn't skip a step
        lastFrameMs = now;

        if (mode == Mode.JOIN && !RTSMapInfoClientEvents.mapName.isBlank())
            setMapInfo(RTSMapInfoClientEvents.mapName, RTSMapInfoClientEvents.authors, RTSMapInfoClientEvents.version,
                    RTSMapInfoClientEvents.description);
        MapGrid grid = grid();
        updateTexture(grid);
        List<Row> rows = shownRows(realRows(s), dt, now);
        boolean allShown = rows.stream().allMatch(r -> r.state == State.DONE);

        HudScale scale = HudGeometry.scale();
        int w = scale.width();
        int h = scale.height();
        float k = Math.min(w / (float) VW, h / (float) VH);
        float ox = (w - VW * k) / 2, oy = (h - VH * k) / 2;
        updateEnding(s, grid, now, allShown, k, ox, oy);

        // the slide, then the zoom; with no zoom the map fades into the match setup map as it lands
        // saving has nothing to slide: it just gets out of the way of the title screen, quickly, so its fading
        // background never sits over the menu as a grey veil
        long moveMs = mode == Mode.SAVING ? SAVED_FADE_MS : MOVE_MS;
        float move = morphMs == 0 ? 0 : Math.min(1, (now - morphMs) / (float) moveMs);
        float grow = morphMs == 0 || !zoom ? 0 : Math.min(1, (now - morphMs - MOVE_MS) / (float) ZOOM_MS);
        if (morphMs != 0 && move >= 1 && (!zoom || grow >= 1)) {
            finish(true);
            return;
        }
        float fadeIn = smooth((now - shownMs) / (float) FADE_IN_MS);
        // the zoom lands on the satellite view first, then the map fades out over it, lined up
        float rest = zoom ? fadeIn * (1 - smooth((grow - 0.2f) / 0.6f)) : fadeIn * (1 - smooth((move - 0.6f) / 0.4f));
        float mapAlpha = zoom ? fadeIn * (1 - smooth((grow - 0.65f) / 0.35f)) : fadeIn * (1 - smooth((move - 0.8f) / 0.2f));

        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        g.pose().translate(0, 0, 400); // over the screen below and its tooltips
        Backdrop.website(g, w, h, rest);
        g.pose().translate(ox, oy, 0);
        g.pose().scale(k, k, 1);

        // only an RTS map has a map to show (joining: once the server has sent one); anything else is centred
        boolean showMap = mode != Mode.SAVING && !replay && (mode == Mode.LOCAL ? rtsMap : grid != null);
        if (showMap) {
            Rect home = new Rect(MAP_X, MAP_Y, MAP_SIDE, MAP_SIDE);
            // centre x, centre y, side, turn: the map can end up turned to match the camera
            float[] at = box(home, 0);
            if (morphMs != 0 && target != null) {
                at = mix(at, box(target, 0), Easing.heavy(move));
                if (zoom && grow > 0) {
                    float[] end = grid != null ? satelliteBox(grid, k, ox, oy) : null;
                    at = mix(at, end != null ? end : box(grown(target, 2.4f), 0), Easing.dive(grow / 0.7f));
                }
            }
            if (grid != null && texture != null) {
                g.pose().pushPose();
                g.pose().translate(at[0], at[1], 0);
                g.pose().mulPose(Axis.ZP.rotation(at[3]));
                int side = Math.round(at[2]);
                drawMap(g, grid, new Rect(-side / 2, -side / 2, side, side), rows, mapAlpha, rest, now);
                g.pose().popPose();
            } else { // the glowing frame is there from the start, so nothing shifts when the map arrives
                Backdrop.glow(g, home.x(), home.y(), home.w(), home.h(), rest);
            }
            // the steps are shoved off the right of the screen as the map heads for the middle
            int off = morphMs == 0 ? 0 : Math.round(Easing.heavy(move) * (VW - COL_X + 40 + ox / k));
            drawColumn(g, rows, COL_X + off, MAP_Y + 10, COL_W, fadeIn, now);
        } else {
            // the title and its info sit above the steps, so a loading column starts higher than saving's lone bar
            drawColumn(g, rows, (VW - COL_W) / 2, mode == Mode.SAVING ? VH / 2 - 20 : VH / 2 - 75, COL_W, rest, now);
        }
        g.pose().popPose();
    }

    private static void drawColumn(GuiGraphics g, List<Row> rows, int x, int y, int w, float a, long now) {
        if (a < 0.02f)
            return;
        if (!title.isBlank()) {
            float ts = Text.px(30);
            Text.big(g, Text.fit(title, w), x, y, alpha(a) | Theme.GOLD & 0xFFFFFF, ts);
            y += Text.bigHeight(ts) + 6;
        }
        if (mode != Mode.SAVING) {
            // room kept for the by-line and two lines of description, so nothing moves when they arrive
            if (!byLine.isBlank())
                Text.small(g, Text.fitSmall(byLine.toUpperCase(Locale.ROOT), w), x, y, alpha(a) | Theme.DIM & 0xFFFFFF);
            y += 12;
            List<FormattedCharSequence> lines = MC.font.split(Component.literal(description), w);
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                Text.draw(g, lines.get(i), x, y + i * 10, alpha(a) | Theme.MUTED & 0xFFFFFF);
            y += 2 * 10 + 14;
        }
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int num = r.state == State.ACTIVE ? Theme.ORANGE : Theme.FAINT;
            int name = r.state == State.ACTIVE ? Theme.GOLD : r.state == State.DONE ? Theme.LABEL : Theme.FAINT;
            // a lone step (saving) needs no number
            int indent = rows.size() > 1 ? 14 : 0;
            if (indent > 0)
                Text.draw(g, String.valueOf(i + 1), x, y, alpha(a) | num & 0xFFFFFF);
            Text.draw(g, I18n.get("loading.reignofnether." + r.key), x + indent, y, alpha(a) | name & 0xFFFFFF);
            if (r.state == State.ACTIVE && r.total > 0)
                Text.smallRight(g, I18n.get("loading.reignofnether.count", Math.round(r.frac * r.total), r.total), x + w, y + 1, alpha(a) | Theme.DIM & 0xFFFFFF);
            int bx = x + indent, by = y + 11, bw = w - indent;
            g.fill(bx, by, bx + bw, by + 2, alpha(a) | TRACK);
            if (r.state == State.DONE)
                g.fill(bx, by, bx + bw, by + 2, alpha(a) | Theme.FAINT & 0xFFFFFF);
            else if (r.state == State.ACTIVE && r.frac < 0) {
                // no known end: a segment sweeps across, round and round
                float t = (now % 1200) / 1200f;
                int seg = bw * 3 / 10;
                int sx = bx + (int) ((bw + seg) * t) - seg;
                g.fill(Math.max(bx, sx), by, Math.min(bx + bw, sx + seg), by + 2, alpha(a) | Theme.ORANGE & 0xFFFFFF);
            } else if (r.state == State.ACTIVE)
                g.fill(bx, by, bx + (int) (bw * r.frac), by + 2, alpha(a) | Theme.ORANGE & 0xFFFFFF);
            y += 24;
        }
    }

    /** The map in cells, one a chunk, each step sweeping from the top corner to the bottom one. */
    private static void drawMap(GuiGraphics g, MapGrid grid, Rect r, List<Row> rows, float a, float overlay, long now) {
        if (a < 0.02f)
            return;
        Backdrop.glow(g, r.x(), r.y(), r.w(), r.h(), overlay);
        float paint = shownOf(rows, mode == Mode.JOIN ? "receive" : "map");
        float nav = shownOf(rows, "navmesh");
        float fog = shownOf(rows, "fog");
        int cells = Mth.clamp(grid.size / 16, 1, MAX_CELLS);
        float texCell = grid.size / (float) cells;
        float cell = r.w() / (float) cells;
        int diagonals = Math.max(1, 2 * cells - 2);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1, 1, 1, a);
        if (paint >= 1 && uploadedRows >= grid.size) {
            g.blit(textureLoc, r.x(), r.y(), r.w(), r.h(), 0, 0, textureSize, textureSize, textureSize, textureSize);
        } else {
            for (int cz = 0; cz < cells; cz++) {
                int v = (int) (cz * texCell), vh = (int) ((cz + 1) * texCell) - v;
                if (v + vh > uploadedRows)
                    break; // not drawn yet, so neither is anything below
                for (int cx = 0; cx < cells; cx++) {
                    if ((cx + cz) / (float) diagonals > paint)
                        continue;
                    int u = (int) (cx * texCell), uw = (int) ((cx + 1) * texCell) - u;
                    int x0 = r.x() + Math.round(cx * cell), y0 = r.y() + Math.round(cz * cell);
                    int x1 = r.x() + Math.round((cx + 1) * cell), y1 = r.y() + Math.round((cz + 1) * cell);
                    g.blit(textureLoc, x0, y0, x1 - x0, y1 - y0, u, v, uw, vh, textureSize, textureSize);
                }
            }
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
        if (overlay < 0.02f)
            return;

        // the fog veil lifts once the snapshot is taken
        if (fog >= 1 && veilDoneMs == 0)
            veilDoneMs = now;
        float veil = fog >= 1 ? 1 - smooth((now - veilDoneMs) / (float) VEIL_FADE_MS) : 1;
        for (int cz = 0; cz < cells; cz++) {
            for (int cx = 0; cx < cells; cx++) {
                float t = (cx + cz) / (float) diagonals;
                int x0 = r.x() + Math.round(cx * cell), y0 = r.y() + Math.round(cz * cell);
                int x1 = r.x() + Math.round((cx + 1) * cell), y1 = r.y() + Math.round((cz + 1) * cell);
                // the edge being painted glows
                if (paint < 1 && t <= paint && paint - t < 0.05f)
                    g.fill(x0, y0, x1, y1, alpha(overlay * 0.35f * (1 - (paint - t) / 0.05f)) | Theme.GOLD & 0xFFFFFF);
                // a band of chunks lights up as the navmesh works through them
                if (nav > 0 && nav < 1 && t <= nav && nav - t < 0.12f)
                    g.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, alpha(overlay * 0.45f * (1 - (nav - t) / 0.12f)) | 0xB8F5BB);
                if (fog > 0 && t <= fog && veil > 0)
                    g.fill(x0, y0, x1, y1, alpha(overlay * 0.5f * veil) | 0x05040A);
            }
        }
    }

    private static void updateTexture(@Nullable MapGrid grid) {
        if (grid == null)
            return;
        if (texture == null || textureSize != grid.size) {
            if (textureLoc != null)
                MC.getTextureManager().release(textureLoc);
            texture = new DynamicTexture(grid.size, grid.size, true);
            textureLoc = MC.getTextureManager().register(ReignOfNether.MOD_ID + "_loading_map", texture);
            textureSize = grid.size;
            uploadedRows = 0;
        }
        int rows = Math.min(grid.rows, grid.size);
        if (rows > uploadedRows) {
            NativeImage pixels = texture.getPixels();
            if (pixels != null) {
                for (int z = uploadedRows; z < rows; z++)
                    for (int x = 0; x < grid.size; x++)
                        pixels.setPixelRGBA(x, z, MiscUtil.reverseHexRGB(grid.rgb[z * grid.size + x]) | 0xFF000000);
                texture.upload();
            }
            uploadedRows = rows;
        }
    }

    private static void updateEnding(@Nullable Screen s, @Nullable MapGrid grid, long now, boolean allShown, float k, float ox, float oy) {
        if (isLoading(s)) {
            endMs = 0;
            return;
        }
        if (mode != Mode.SAVING && MC.level == null) {
            // a menu instead of the world (create world's options, a cancelled connection): nothing is coming
            if (s != null && morphMs == 0) {
                target = null;
                zoom = false;
                morphMs = now;
            }
            return; // otherwise between screens, before the world
        }
        if (endMs == 0)
            endMs = now;
        if (morphMs != 0 || now < endMs + SETTLE_MS || !allShown)
            return;
        if (!rtsViewReady(now))
            return;
        target = null;
        zoom = false;
        if (mode != Mode.SAVING && grid != null) {
            if (s instanceof MatchStartScreen) {
                Rect r = MatchStartScreen.mapRect;
                if (r == null)
                    return; // not drawn yet
                target = new Rect(Math.round((r.x() - ox) / k), Math.round((r.y() - oy) / k), Math.round(r.w() / k), Math.round(r.h() / k));
            } else {
                int side = MAP_SIDE + 30;
                target = new Rect((VW - side) / 2, (VH - side) / 2, side, side);
                zoom = true;
                if (s instanceof TopdownGui && OrthoviewClientEvents.isEnabled()) {
                    // behind the backdrop the camera goes over the middle of the map and looks straight down, so
                    // the map zooms into the satellite view; it tilts back once the fade is done
                    // straight down, the middle of the screen is right under the camera: centreCameraOnPos would
                    // offset it for a tilted view, by how high the camera is, and miss on a high map
                    // in a match we play in, over our base, so the view settles there; the zoom lines up wherever
                    // the map ends up on screen
                    BuildingPlacement base = ownBase();
                    double cx = base != null ? base.centrePos.getX() + 0.5 : grid.minX + grid.size / 2.0;
                    double cz = base != null ? base.centrePos.getZ() + 0.5 : grid.minZ + grid.size / 2.0;
                    MinimapClientEvents.setMapCentre(cx, cz);
                    PlayerServerboundPacket.teleportPlayer(cx, MC.player.getY(), cz);
                    StrategicMapClientEvents.open();
                    satellite = StrategicMapClientEvents.isOpen();
                }
            }
        }
        morphMs = now;
    }

    /**
     * On an RTS map the world opens in the RTS view: once the server has made us a spectator, the view is turned on
     * and the transition waits for it to open. Gives up after a few seconds so it never hangs.
     */
    private static boolean rtsViewReady(long now) {
        if (mode == Mode.SAVING || MC.level == null || MC.player == null || MC.gameMode == null
                || MC.level.getWorldBorder().getSize() > WorldBorderServerEvents.RTS_OPTIMIZED_BORDER
                || PlayerRolesClient.myRole() == PlayerRole.SOLDIER || now > endMs + RTS_VIEW_WAIT_MS)
            return true;
        if (MC.screen instanceof TopdownGui || MC.screen instanceof MatchStartScreen)
            return true;
        GameType type = MC.gameMode.getPlayerMode();
        if (!OrthoviewClientEvents.isEnabled() && (type == GameType.SPECTATOR || type == GameType.CREATIVE))
            OrthoviewClientEvents.toggleEnable();
        return false;
    }

    // ------------------------------------------------------------------ helpers

    private static int alpha(float a) {
        return Mth.clamp((int) (a * 255), 0, 255) << 24;
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static float[] box(Rect r, float turn) {
        return new float[] { r.x() + r.w() / 2f, r.y() + r.h() / 2f, r.w(), turn };
    }

    private static float[] mix(float[] a, float[] b, float t) {
        float turn = Mth.wrapDegrees((float) Math.toDegrees(b[3] - a[3])); // the short way round
        return new float[] { a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t,
                a[3] + (float) Math.toRadians(turn) * t };
    }

    /** Where the map's corners are on screen in the satellite view, in canvas units, so the zoom ends lined up. */
    @Nullable
    private static float[] satelliteBox(MapGrid grid, float k, float ox, float oy) {
        if (!satellite || MC.level == null)
            return null;
        double y = MC.level.getSeaLevel();
        Vec2 nw = WorldToScreen.project(new Vec3(grid.minX, y, grid.minZ));
        Vec2 ne = WorldToScreen.project(new Vec3(grid.minX + grid.size, y, grid.minZ));
        Vec2 sw = WorldToScreen.project(new Vec3(grid.minX, y, grid.minZ + grid.size));
        if (nw == null || ne == null || sw == null)
            return null;
        HudScale scale = HudGeometry.scale();
        float[] a = canvas(scale, nw, k, ox, oy), b = canvas(scale, ne, k, ox, oy), c = canvas(scale, sw, k, ox, oy);
        float side = (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
        // the corner opposite nw is ne + sw - nw, so the centre is halfway between ne and sw
        return new float[] { (b[0] + c[0]) / 2, (b[1] + c[1]) / 2, side, (float) Math.atan2(b[1] - a[1], b[0] - a[0]) };
    }

    private static float[] canvas(HudScale scale, Vec2 gui, float k, float ox, float oy) {
        return new float[] { (scale.toUnits(gui.x) - ox) / k, (scale.toUnits(gui.y) - oy) / k };
    }

    // the same rect grown around its centre: the map rushing up as the world takes its place
    private static Rect grown(Rect r, float by) {
        int w = Math.round(r.w() * by), h = Math.round(r.h() * by);
        return new Rect(r.x() + (r.w() - w) / 2, r.y() + (r.h() - h) / 2, w, h);
    }
}
