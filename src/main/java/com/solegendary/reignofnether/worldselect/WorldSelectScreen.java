package com.solegendary.reignofnether.worldselect;

import com.mojang.blaze3d.platform.NativeImage;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.mappack.MapPack;
import com.solegendary.reignofnether.mappack.MapPackScreen;
import com.solegendary.reignofnether.ui.Account;
import com.solegendary.reignofnether.ui.Backdrop;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The singleplayer screen as a map picker: official map pack worlds and the player's own worlds as tiles,
 * filters, and the picked world's details with Open, Edit and Delete. Create world and the saves folder are
 * one click away, so it does everything vanilla's world list does.
 */
public class WorldSelectScreen extends Screen {

    private static final Pattern PLAYERS = Pattern.compile("^\\[(\\d+)p?]", Pattern.CASE_INSENSITIVE);

    private enum Rts { ALL, YES, NO }

    /** One world: its summary plus what the tiles and filters need. */
    private static final class Entry {
        final LevelSummary summary;
        final String name;
        final int players;              // 0 = unknown
        final boolean officialMap;      // from the official map pack (a saved game: its map is)
        final SavedGames.Info save;     // set when the world is a saved game of a map
        final boolean hasMapInfo;       // has an rtsmap.json, so it was made as an RTS map
        volatile Boolean rtsOptimised;  // read from level.dat in the background, null until then
        volatile List<String> modes = List.of(); // the game modes of its rtsmap.json, e.g. "1v1", "ffa_4"
        volatile String author = "", description = ""; // also from rtsmap.json
        FaviconTexture icon;            // the world's own icon.png, when it has one
        final List<Entry> saves = new ArrayList<>(); // a map's saved games, newest first
        Entry mapEntry;                 // a saved game's map, when that map is in the list

        Entry(LevelSummary summary, String name, int players, boolean officialMap, SavedGames.Info save, boolean hasMapInfo) {
            this.summary = summary;
            this.name = name;
            this.players = players;
            this.officialMap = officialMap;
            this.save = save;
            this.hasMapInfo = hasMapInfo;
        }

        boolean official() {
            return officialMap && save == null;
        }

        /** An RTS map: played through a copy so it stays as it was made. */
        boolean isMap() {
            return save == null && (officialMap || hasMapInfo || Boolean.TRUE.equals(rtsOptimised));
        }
    }

    private record Hit(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private Entry selected;
    private String error;
    private boolean loading;
    private Rts rtsFilter = Rts.ALL;
    private int sizeFilter; // 0 = any, 6 = 6 and up
    private double scroll;
    private int contentHeight, gridTop, gridBottom;
    private long lastClick;
    private boolean cloning;

    public WorldSelectScreen(Screen parent) {
        super(Component.translatable("selectWorld.title"));
        this.parent = parent;
    }

    // ---- loading ----

    @Override
    protected void init() {
        // runs again after Create, Edit and Delete, so the list is always fresh
        reload();
    }

    private void reload() {
        loading = true;
        error = null;
        LevelStorageSource source = minecraft.getLevelSource();
        try {
            source.loadLevelSummaries(source.findLevelCandidates()).thenAcceptAsync(this::setSummaries, minecraft);
        } catch (Exception e) {
            loading = false;
            error = e.getMessage();
        }
    }

    private void setSummaries(List<LevelSummary> summaries) {
        String keep = selected != null ? selected.summary.getLevelId() : null;
        closeIcons();
        entries.clear();
        selected = null;
        Path saves = minecraft.getLevelSource().getBaseDir();
        Set<String> pack = MapPack.installedWorlds();
        for (LevelSummary s : summaries) {
            String id = s.getLevelId();
            SavedGames.Info save = SavedGames.read(saves.resolve(id));
            // a saved game shows its map's player count; its icon.png came with the copy
            String source = save != null && !save.map().isEmpty() ? save.map() : id;
            Matcher m = PLAYERS.matcher(source);
            boolean hasPlayers = m.find();
            // official: installed by the map pack download, or named like the pack's maps ("[2p] Oceanborn") with an
            // rtsmap.json, for packs unzipped by hand
            boolean official = pack.contains(source)
                    || hasPlayers && Files.isRegularFile(saves.resolve(source).resolve("rtsmap.json"));
            String name = save == null && official ? prettyName(id) : s.getLevelName();
            Entry e = new Entry(s, name, hasPlayers ? Integer.parseInt(m.group(1)) : 0, official, save,
                    Files.isRegularFile(saves.resolve(id).resolve("rtsmap.json")));
            if (s.getIcon() != null && Files.isRegularFile(s.getIcon())) {
                try (InputStream in = Files.newInputStream(s.getIcon())) {
                    e.icon = FaviconTexture.forWorld(minecraft.getTextureManager(), id);
                    e.icon.upload(NativeImage.read(in));
                } catch (Exception ignored) {
                    e.icon = null;
                }
            }
            // RTS-optimised = a world border small enough to be built for RoN, as the server decides it
            Util.backgroundExecutor().execute(() -> {
                try {
                    CompoundTag data = NbtIo.readCompressed(saves.resolve(id).resolve("level.dat").toFile()).getCompound("Data");
                    e.rtsOptimised = data.contains("BorderSize") && data.getDouble("BorderSize") <= WorldBorderServerEvents.RTS_OPTIMIZED_BORDER;
                } catch (Exception ignored) {
                    e.rtsOptimised = false;
                }
                try {
                    Path json = saves.resolve(id).resolve("rtsmap.json");
                    if (Files.isRegularFile(json)) {
                        var info = com.google.gson.JsonParser.parseString(Files.readString(json)).getAsJsonObject();
                        if (info.has("description"))
                            e.description = info.get("description").getAsString();
                        if (info.has("author")) { // a name or a list of names
                            var a = info.get("author");
                            List<String> names = new ArrayList<>();
                            if (a.isJsonArray())
                                a.getAsJsonArray().forEach(n -> names.add(n.getAsString()));
                            else
                                names.add(a.getAsString());
                            e.author = String.join(", ", names);
                        }
                        if (info.has("modes"))
                            e.modes = List.copyOf(info.getAsJsonObject("modes").keySet());
                    }
                } catch (Exception ignored) { } // no modes shown

            });
            entries.add(e);
            if (id.equals(keep))
                selected = e;
        }
        // saved games are listed under their map, when the map is still there
        for (Entry e : entries)
            if (e.save != null)
                for (Entry m : entries)
                    if (m.save == null && m.summary.getLevelId().equals(e.save.map())) {
                        e.mapEntry = m;
                        m.saves.add(e);
                    }
        for (Entry e : entries)
            e.saves.sort((a, b) -> Long.compare(b.summary.getLastPlayed(), a.summary.getLastPlayed()));
        if (selected != null && selected.mapEntry != null)
            selected = selected.mapEntry;
        // official maps by player count then name, everything else most recent first (as loaded)
        entries.sort((a, b) -> a.official() && b.official() ? a.players != b.players ? a.players - b.players : a.name.compareToIgnoreCase(b.name)
                : Boolean.compare(!a.official(), !b.official()));
        if (selected == null)
            selected = entries.stream().filter(e -> e.mapEntry == null).findFirst().orElse(null);
        loading = false;
    }

    private static String prettyName(String folder) {
        return folder.replaceFirst("^\\[[^]]*]\\s*", "").replaceFirst("\\s+\\d+(\\.\\d+)*$", "").replace('_', ' ');
    }

    // each icon is freed once: the screen is removed and re-initialised around Edit, Delete and Create
    private void closeIcons() {
        for (Entry e : entries)
            if (e.icon != null) {
                e.icon.close();
                e.icon = null;
            }
    }

    @Override
    public void removed() {
        closeIcons();
        entries.clear(); // init reloads them
    }

    // ---- actions ----

    /** The big button: a map plays a fresh copy, anything else opens as it is. */
    private void primary(Entry e) {
        if (e.isMap())
            playMap(e);
        else
            open(e);
    }

    private void playMap(Entry e) {
        if (cloning || e.summary.isLocked() || e.summary.isDisabled())
            return;
        cloning = true;
        error = null;
        Path saves = minecraft.getLevelSource().getBaseDir();
        String mapId = e.summary.getLevelId();
        long now = System.currentTimeMillis();
        CompletableFuture.supplyAsync(() -> {
            try {
                return SavedGames.cloneMap(saves, mapId, e.name, now);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }, Util.backgroundExecutor()).whenCompleteAsync((saveId, ex) -> {
            cloning = false;
            if (ex != null) {
                error = "Couldn't copy the map: " + (ex.getCause() != null ? ex.getCause() : ex).getMessage();
                return;
            }
            try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(saveId)) {
                access.renameLevel(SavedGames.saveName(e.name, now));
            } catch (Exception ignored) { } // keeps the map's name, still playable
            minecraft.createWorldOpenFlows().loadLevel(this, saveId);
        }, minecraft);
    }

    private void open(Entry e) {
        LevelSummary s = e.summary;
        if (s.isDisabled() || s.isLocked())
            return;
        String id = s.getLevelId();
        if (s.backupStatus().shouldBackup()) {
            // same prompt as vanilla before opening a world from another version
            String key = s.backupStatus().getTranslationKey();
            minecraft.setScreen(new BackupConfirmScreen(this, (backup, eraseCache) -> {
                if (backup)
                    EditWorldScreen.makeBackupAndShowToast(minecraft.getLevelSource(), id);
                minecraft.createWorldOpenFlows().loadLevel(this, id);
            }, Component.translatable("selectWorld.backupQuestion." + key),
                    Component.translatable("selectWorld.backupWarning." + key, s.getWorldVersionName(),
                            Component.literal(SharedConstants.getCurrentVersion().getName())),
                    s.backupStatus().isSevere()));
            return;
        }
        minecraft.createWorldOpenFlows().loadLevel(this, id);
    }

    private void edit(Entry e) {
        String id = e.summary.getLevelId();
        try {
            LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().validateAndCreateAccess(id);
            minecraft.setScreen(new EditWorldScreen(changed -> {
                try {
                    access.close();
                } catch (Exception ignored) { }
                minecraft.setScreen(this);
            }, access));
        } catch (Exception ex) {
            SystemToast.onWorldAccessFailure(minecraft, id);
        }
    }

    private void delete(Entry e) {
        String id = e.summary.getLevelId();
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                try (LevelStorageSource.LevelStorageAccess access = minecraft.getLevelSource().createAccess(id)) {
                    access.deleteLevel();
                } catch (Exception ex) {
                    SystemToast.onWorldDeleteFailure(minecraft, id);
                }
                if (selected == e)
                    selected = null;
            }
            minecraft.setScreen(this);
        }, Component.translatable("selectWorld.deleteQuestion"),
                Component.translatable("selectWorld.deleteWarning", e.summary.getLevelName()),
                Component.translatable("selectWorld.deleteButton"), CommonComponents.GUI_CANCEL));
    }

    private boolean shown(Entry e) {
        if (e.mapEntry != null) // shown in its map's save list
            return false;
        if (rtsFilter != Rts.ALL && (e.rtsOptimised == null || e.rtsOptimised != (rtsFilter == Rts.YES)))
            return false;
        return sizeFilter == 0 || (sizeFilter == 6 ? e.players >= 6 : e.players == sizeFilter);
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // hits are in HUD units
        double mx = mouseX / Text.guiPerUnit(), my = mouseY / Text.guiPerUnit();
        if (button == 0)
            for (Hit h : List.copyOf(hits))
                if (h.contains(mx, my)) {
                    h.action.run();
                    return true;
                }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scroll = Math.max(0, Math.min(scroll - delta * 40, Math.max(0, contentHeight - (gridBottom - gridTop))));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && selected != null) {
            primary(selected);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ---- drawing: HUD units like the rest of the RoN UI (the design's sizes at any GUI scale), the vanilla
    // font for text and Pixelify only for titles ----

    private static final int INK = 0xFF0B0A0F, RING = 0xFF14131A, CARD = 0xFF1C1B21, MUTED = 0xFF80849F, GREY = 0xFF575A82;
    private static final int[] WHITE = { Theme.TEXT, Theme.TEXT, Theme.TEXT, Theme.TEXT };
    private static final int[] DARK = { Theme.ON_ORANGE, Theme.ON_ORANGE, Theme.ON_ORANGE, Theme.ON_ORANGE };

    private boolean hovered(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Pixelify title h units tall at (x, y); bands null = the gold gradient. */
    private static void pix(GuiGraphics g, String s, int x, int y, int h, int[] bands, int shadow) {
        if (bands == null)
            Text.title(g, s, x, y, h / 8f);
        else
            Text.title(g, s, x, y, h / 8f, bands, shadow);
    }

    private static int pixWidth(String s, int h) {
        return Text.titleWidth(s, h / 8f);
    }

    /** Pixelify text split over up to two lines at a space; the second is cut to fit. [1] is null for one line. */
    private static String[] twoLines(String s, int h, int maxW) {
        if (pixWidth(s, h) <= maxW)
            return new String[]{ s, null };
        int cut = -1;
        for (int i = s.indexOf(' '); i > 0; i = s.indexOf(' ', i + 1))
            if (pixWidth(s.substring(0, i), h) <= maxW)
                cut = i;
        if (cut < 0)
            return new String[]{ fitPix(s, h, maxW), null };
        return new String[]{ s.substring(0, cut), fitPix(s.substring(cut + 1), h, maxW) };
    }

    private static String fitPix(String s, int h, int maxW) {
        if (pixWidth(s, h) <= maxW)
            return s;
        while (s.length() > 1 && pixWidth(s + "…", h) > maxW)
            s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private void text(GuiGraphics g, String s, int x, int y, int colour) {
        text(g, s, x, y, colour, false);
    }

    /**
     * Labels in the design's label font (Silkscreen capitals) at full size, so crisp and readable; the caps sit
     * in the 8-unit line at y. Names and values use body() in the vanilla font.
     */
    private void text(GuiGraphics g, String s, int x, int y, int colour, boolean shadow) {
        g.drawString(font, label(s), x, y, colour, shadow);
    }

    private static Component label(String s) {
        return Component.literal(s.toUpperCase(java.util.Locale.ROOT)).withStyle(Text.SMALL_STYLE);
    }

    private int tw(String s) {
        return font.width(label(s));
    }

    private String fit(String s, int maxW) {
        if (tw(s) <= maxW)
            return s;
        while (s.length() > 1 && tw(s + "…") > maxW)
            s = s.substring(0, s.length() - 1);
        return s + "…";
    }

    private void body(GuiGraphics g, String s, int x, int y, int colour, boolean shadow) {
        g.drawString(font, s, x, y, colour, shadow);
    }

    private String fitBody(String s, int maxW) {
        return font.width(s) <= maxW ? s : font.plainSubstrByWidth(s, Math.max(0, maxW - font.width("…"))) + "…";
    }

    /** The design's flat button: fill, a top highlight and bottom shade, a dark ring. */
    private static void flat(GuiGraphics g, int x, int y, int w, int h, int fill) {
        Theme.outline(g, x, y, w, h, 1, RING);
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, 0x17FFFFFF);
        g.fill(x, y + h - 1, x + w, y + h, 0x8C000000);
    }

    /** Text button; registers its click. */
    private void button(GuiGraphics g, String label, int x, int y, int w, int h, int mx, int my, Runnable action) {
        boolean hov = hovered(x, y, w, h, mx, my);
        flat(g, x, y, w, h, hov ? Theme.BUTTON_HOVER : Theme.SECONDARY);
        text(g, label, x + (w - tw(label)) / 2, y + (h - 8) / 2, hov ? Theme.GOLD : Theme.BODY);
        hits.add(new Hit(x, y, w, h, action));
    }

    /** "Today", "3 days ago"... for when a world was last played. */
    private static String ago(long time) {
        if (time <= 0)
            return "NEVER PLAYED";
        long days = (System.currentTimeMillis() - time) / 86_400_000L;
        if (days <= 0) return "TODAY";
        if (days == 1) return "YESTERDAY";
        if (days < 7) return days + " DAYS AGO";
        if (days < 14) return "LAST WEEK";
        if (days < 60) return days / 7 + " WEEKS AGO";
        return days / 30 + " MONTHS AGO";
    }

    /** When a map was last played: its newest saved game, or the world itself. */
    private static long lastPlayed(Entry e) {
        return e.isMap() ? e.saves.isEmpty() ? 0 : e.saves.get(0).summary.getLastPlayed() : e.summary.getLastPlayed();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        hits.clear();
        Backdrop.render(g, this.width, this.height, 0.75f);
        float k = Text.guiPerUnit();
        int width = Math.round(this.width / k), height = Math.round(this.height / k);
        int mx = Math.round(mouseX / k), my = Math.round(mouseY / k);
        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        int pad = 10;

        // header: back, kicker and title; the account chip on the right
        int backW = tw("← BACK") + 16;
        button(g, "← BACK", pad, 8, backW, 26, mx, my, this::onClose);
        text(g, "SINGLEPLAYER · HOST A MATCH", pad + backW + 10, 8, Theme.ORANGE);
        pix(g, "CHOOSE A MAP", pad + backW + 10, 20, 12, null, 0);
        renderAccount(g, width - pad, 8);
        int top = 42;

        int sideW = Math.max(180, Math.min(280, width * 27 / 100));
        int sideX = width - pad - sideW;
        int mainX = pad, mainW = sideX - 8 - mainX, bottom = height - pad;

        // main panel: the filter bar, then the scrolling map cards
        g.fill(mainX, top, mainX + mainW, bottom, Theme.PANEL);
        Theme.outline(g, mainX, top, mainW, bottom - top, 1, INK);
        int fx = mainX + 8, fy = top + 7;
        fx = filters(g, fx, fy, mx, my, new String[]{ "ALL", "RTS OPTIMIZED", "NOT RTS OPTIMIZED" }, rtsFilter.ordinal(),
                i -> rtsFilter = Rts.values()[i]);
        int[] sizes = { 0, 2, 3, 4, 6 };
        int sizeIndex = 0;
        for (int i = 0; i < sizes.length; i++)
            if (sizes[i] == sizeFilter)
                sizeIndex = i;
        filters(g, fx + 8, fy, mx, my, new String[]{ "ANY SIZE", "2P", "3P", "4P", "6P+" }, sizeIndex, i -> sizeFilter = sizes[i]);
        g.fill(mainX, top + 31, mainX + mainW, top + 32, INK);

        gridTop = top + 32;
        gridBottom = bottom - 1;
        int gx = mainX + 8, gw = mainW - 16, gap = 5;
        int cols = Math.max(1, Math.min(5, (gw + gap) / (150 + gap)));
        int cardW = (gw - (cols - 1) * gap) / cols, cardH = 52;
        List<Entry> official = new ArrayList<>(), folder = new ArrayList<>();
        for (Entry e : entries)
            if (shown(e))
                (e.official() ? official : folder).add(e);

        // the scissor is in GUI coordinates
        g.enableScissor(Math.round(mainX * k), Math.round(gridTop * k), Math.round((mainX + mainW) * k), Math.round(gridBottom * k));
        int y = gridTop + 8 - (int) scroll;
        int startY = y;
        List<Hit> gridHits = new ArrayList<>();
        y = section(g, "OFFICIAL MAP PACK", official.size() + " MAPS", gx, y, gw, mx, my, false, official.isEmpty() && !loading, gridHits);
        y = cards(g, official, gx, y, cols, cardW, cardH, gap, mx, my, gridHits);
        y = section(g, "YOUR MAPS FOLDER", folder.size() + " MAPS", gx, y + 6, gw, mx, my, true, false, gridHits);
        y = cards(g, folder, gx, y, cols, cardW, cardH, gap, mx, my, gridHits);
        if (loading)
            text(g, "LOADING WORLDS…", gx, y, Theme.LABEL);
        if (error != null)
            body(g, fitBody(error, gw), gx, y + 12, Theme.ERROR, false);
        g.disableScissor();
        contentHeight = y + 24 - startY;
        // clicks in the list only count where it is visible
        for (Hit h : gridHits)
            if (h.y + h.h > gridTop && h.y < gridBottom)
                hits.add(h);

        renderSide(g, sideX, top, sideW, bottom - top, mx, my);
        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }

    /** A group of filter tabs on a dark strip; the picked one has an orange underline. Returns the x after it. */
    private int filters(GuiGraphics g, int x, int y, int mx, int my, String[] labels, int on, java.util.function.IntConsumer pick) {
        int w = 2;
        for (String l : labels)
            w += tw(l) + 14 + 2;
        g.fill(x, y, x + w, y + 18, INK);
        int bx = x + 2;
        for (int i = 0; i < labels.length; i++) {
            int bw = tw(labels[i]) + 14;
            boolean hov = hovered(bx, y + 2, bw, 14, mx, my);
            if (i == on) {
                g.fill(bx, y + 2, bx + bw, y + 16, Theme.SECONDARY);
                g.fill(bx, y + 14, bx + bw, y + 16, Theme.ORANGE);
            } else if (hov)
                g.fill(bx, y + 2, bx + bw, y + 16, CARD);
            text(g, labels[i], bx + 7, y + 5, i == on ? Theme.GOLD : hov ? Theme.TEXT : MUTED);
            int index = i;
            hits.add(new Hit(bx, y + 2, bw, 14, () -> { pick.accept(index); scroll = 0; }));
            bx += bw + 2;
        }
        return x + w;
    }

    /** Section title, count and rule; the folder also gets Create world and Open folder. Returns the y below. */
    private int section(GuiGraphics g, String title, String count, int x, int y, int w, int mx, int my,
                        boolean folder, boolean getPack, List<Hit> gridHits) {
        text(g, title, x, y + 2, folder ? Theme.BODY : Theme.ORANGE);
        int cx = x + tw(title) + 8;
        text(g, count, cx, y + 2, GREY);
        int ruleX = cx + tw(count) + 8, right = x + w;
        int hitsBefore = hits.size();
        if (folder) {
            String open = "OPEN FOLDER", create = "+ CREATE WORLD";
            int ow = tw(open) + 12, cw = tw(create) + 12;
            button(g, open, right - ow, y - 4, ow, 16, mx, my, () -> Util.getPlatform().openFile(minecraft.getLevelSource().getBaseDir().toFile()));
            int bx = right - ow - 4 - cw;
            boolean hc = hovered(bx, y - 4, cw, 16, mx, my);
            Theme.outline(g, bx, y - 4, cw, 16, 1, RING);
            g.fill(bx, y - 4, bx + cw, y + 12, hc ? 0xFF3A2C1C : Theme.ACTIVE);
            g.fill(bx, y + 10, bx + cw, y + 12, Theme.ORANGE);
            text(g, create, bx + 6, y, Theme.GOLD);
            hits.add(new Hit(bx, y - 4, cw, 16, () -> CreateWorldScreen.openFresh(minecraft, this)));
            right -= ow + cw + 12;
        } else if (getPack) {
            String get = "GET MAP PACK";
            int bw = tw(get) + 12;
            button(g, get, right - bw, y - 2, bw, 14, mx, my, () -> minecraft.setScreen(new MapPackScreen(this)));
            right -= bw + 8;
        }
        while (hits.size() > hitsBefore)
            gridHits.add(hits.remove(hitsBefore));
        if (right > ruleX)
            g.fill(ruleX, y + 5, right, y + 6, 0xFF24232C);
        return y + 18;
    }

    private int cards(GuiGraphics g, List<Entry> list, int x, int y, int cols, int w, int h, int gap, int mx, int my, List<Hit> gridHits) {
        for (int i = 0; i < list.size(); i++) {
            Entry e = list.get(i);
            int cx = x + (i % cols) * (w + gap), cy = y + (i / cols) * (h + gap);
            boolean sel = e == selected;
            boolean hov = hovered(cx, cy, w, h, mx, my) && my >= gridTop && my < gridBottom;
            g.fill(cx, cy, cx + w, cy + h, hov ? 0xFF24232C : CARD);
            g.fill(cx, cy + h - 1, cx + w, cy + h, 0x80000000);
            Theme.outline(g, cx, cy, w, h, 1, sel ? Theme.ORANGE : INK);
            int img = h - 8;
            thumbnail(g, e, cx + 4, cy + 4, img);
            if (e.players > 0) { // player count as a corner tag on the thumbnail
                String p = e.players + "P";
                g.fill(cx + 5, cy + 5, cx + 9 + tw(p), cy + 15, 0xD90B0A0F);
                text(g, p, cx + 7, cy + 6, Theme.TEXT);
            }
            int tx = cx + img + 10, tw = w - img - 14;
            String[] name = twoLines(e.name, 8, tw);
            pix(g, name[0], tx, cy + 5, 8, WHITE, 0xFF000000);
            if (name[1] != null)
                pix(g, name[1], tx, cy + 15, 8, WHITE, 0xFF000000);
            // tags: saves, not RTS, then when it was last played
            int tagX = tx;
            if (!e.saves.isEmpty())
                tagX = tag(g, e.saves.size() + (e.saves.size() == 1 ? " SAVE" : " SAVES"), tagX, cy + 28, 0xEB2F2418, Theme.GOLD);
            if (e.save != null)
                tagX = tag(g, "SAVED GAME", tagX, cy + 28, 0xEB2F2418, Theme.GOLD);
            if (Boolean.FALSE.equals(e.rtsOptimised) && !e.isMap())
                tag(g, "NOT RTS", tagX, cy + 28, 0xD90B0A0F, Theme.ORANGE);
            long last = lastPlayed(e);
            if (last > 0)
                text(g, fit("PLAYED " + ago(last), tw), tx, cy + h - 12, MUTED);
            gridHits.add(new Hit(cx, cy, w, h, () -> {
                // a second click on the same card plays it
                long now = Util.getMillis();
                if (selected == e && now - lastClick < 400)
                    primary(e);
                selected = e;
                lastClick = now;
            }));
        }
        int rows = (list.size() + cols - 1) / cols;
        return y + rows * (h + gap) + 4;
    }

    private int tag(GuiGraphics g, String label, int x, int y, int bg, int colour) {
        int w = tw(label) + 6;
        g.fill(x, y, x + w, y + 10, bg);
        text(g, label, x + 3, y + 1, colour);
        return x + w + 3;
    }

    private void thumbnail(GuiGraphics g, Entry e, int x, int y, int size) {
        Theme.outline(g, x, y, size, size, 1, INK);
        if (e.icon != null)
            g.blit(e.icon.textureLocation(), x, y, size, size, 0, 0, 64, 64, 64, 64);
        else {
            g.fill(x, y, x + size, y + size, Theme.EMPTY);
            text(g, "?", x + (size - tw("?")) / 2, y + size / 2 - 4, Theme.DIM);
        }
    }

    /** Top right: the signed-in player's head, name and account type. */
    private void renderAccount(GuiGraphics g, int right, int y) {
        String name = Account.name(), kind = Account.kind().toUpperCase();
        int head = 20, w = 3 + head + 6 + Math.max(font.width(name), tw(kind)) + 6;
        int x = right - w;
        g.fill(x, y, x + w, y + head + 6, 0xB30D0305);
        Theme.outline(g, x + 3, y + 3, head, head, 1, INK);
        PlayerFaceRenderer.draw(g, Account.skin(), x + 3, y + 3, head);
        body(g, name, x + head + 9, y + 4, Theme.TEXT, true);
        text(g, kind, x + head + 9, y + 15, GREY);
    }

    private void renderSide(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.fill(x, y, x + w, y + h, Theme.PANEL);
        Theme.outline(g, x, y, w, h, 1, INK);
        g.fill(x, y, x + w, y + 1, Theme.ORANGE);
        if (selected == null) {
            text(g, loading ? "LOADING…" : "NO WORLDS YET", x + 10, y + 12, Theme.LABEL);
            return;
        }
        Entry e = selected;
        LevelSummary s = e.summary;
        int in = 10, iw = w - 2 * in;

        // the buttons at the bottom: Play, then Edit and Delete
        int playH = 34, rowH = 18;
        int playY = y + h - in - rowH - 6 - playH;

        // name and kind (no big preview: the worlds' 64px icons are too blocky at that size)
        int ty = y + 12;
        String[] name = twoLines(e.name, 14, iw);
        int[] gold = { Theme.GOLD, Theme.GOLD, Theme.GOLD, Theme.GOLD };
        pix(g, name[0], x + in, ty, 14, gold, Theme.ON_ORANGE);
        if (name[1] != null) {
            ty += 17;
            pix(g, name[1], x + in, ty, 14, gold, Theme.ON_ORANGE);
        }
        String kind = e.save != null ? "SAVED GAME" : e.official() ? "OFFICIAL MAP PACK" : e.isMap() ? "YOUR MAP" : "YOUR WORLD";
        if (!e.author.isEmpty())
            kind += " \u00b7 BY " + e.author.toUpperCase();
        text(g, fit(kind, iw), x + in, ty + 18, MUTED);

        int ly = ty + 32;
        // one row of chips: player count, RTS optimised (green, with a check), the game modes of its rtsmap.json
        List<String> chips = new ArrayList<>();
        if (e.players > 0)
            chips.add(e.players + "P");
        boolean rts = Boolean.TRUE.equals(e.rtsOptimised);
        if (rts)
            chips.add("RTS OPTIMIZED");
        for (String mode : e.modes)
            chips.add(mode.replace('_', ' ').toUpperCase());
        int chipX = x + in;
        for (String c : chips) {
            boolean green = rts && c.equals("RTS OPTIMIZED");
            int chipW = tw(c) + 10 + (green ? 12 : 0);
            if (chipX + chipW > x + in + iw && chipX > x + in) {
                chipX = x + in;
                ly += 18;
            }
            g.fill(chipX, ly, chipX + chipW, ly + 14, green ? 0xFF16301A : CARD);
            Theme.outline(g, chipX, ly, chipW, 14, 1, INK);
            if (green) {
                Icon.CHECK.draw(g, chipX + 3, ly + 1, 12, Theme.OK);
                text(g, c, chipX + 15, ly + 3, Theme.OK);
            } else
                text(g, c, chipX + 5, ly + 3, Theme.BODY);
            chipX += chipW + 4;
        }
        if (!chips.isEmpty())
            ly += 22;
        // the map's description, up to four lines
        if (!e.description.isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(e.description), iw);
            for (int i = 0; i < Math.min(4, lines.size()); i++)
                g.drawString(font, lines.get(i), x + in, ly + i * 10, Theme.DESC, false);
            ly += Math.min(4, lines.size()) * 10 + 8;
        }

        if (s.isLocked() || s.isDisabled()) {
            body(g, fitBody(s.getInfo().getString(), iw), x + in, ly, Theme.ERROR, false);
            ly += 14;
        }
        if (Boolean.FALSE.equals(e.rtsOptimised) && !e.isMap()) {
            // the design's warning: an orange edge and the reason
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(
                    "Terrain, resources and start positions weren't planned for Reign of Nether, so matches may feel unbalanced."),
                    iw - 10);
            int wh = lines.size() * 10 + 8;
            g.fill(x + in, ly, x + in + iw, ly + wh, 0x0FF19A0F);
            g.fill(x + in, ly, x + in + 2, ly + wh, Theme.ORANGE_DARK);
            for (int i = 0; i < lines.size(); i++)
                g.drawString(font, lines.get(i), x + in + 7, ly + 4 + i * 10, Theme.DESC, false);
            ly += wh + 8;
        }

        // saved games (of a map, or copies of a world): Continue or Delete each
        if (e.isMap() || !e.saves.isEmpty()) {
            text(g, "SAVES", x + in, ly, MUTED);
            String n = String.valueOf(e.saves.size());
            text(g, n, x + in + tw("SAVES") + 6, ly, GREY);
            g.fill(x + in + tw("SAVES") + tw(n) + 12, ly + 4, x + in + iw, ly + 5, 0xFF24232C);
            ly += 14;
            if (e.saves.isEmpty()) {
                Theme.outline(g, x + in + 1, ly + 1, iw - 2, 18, 1, 0xFF1C1B22);
                text(g, fit("No saves yet. Press Play to start one.", iw - 10), x + in + 5, ly + 6, GREY);
            }
            int rh = 26;
            for (int i = 0; i < e.saves.size() && ly + rh <= playY - 6; i++, ly += rh + 3) {
                Entry sv = e.saves.get(i);
                if (ly + 2 * rh + 3 > playY - 6 && i < e.saves.size() - 1) { // no room for the rest
                    text(g, "+" + (e.saves.size() - i) + " MORE", x + in, ly + 4, GREY);
                    break;
                }
                g.fill(x + in, ly, x + in + iw, ly + rh, CARD);
                g.fill(x + in, ly + rh - 1, x + in + iw, ly + rh, 0x80000000);
                String del = "DELETE", cont = "CONTINUE";
                int dw = tw(del) + 8, cw2 = tw(cont) + 8;
                int bx = x + in + iw - 4 - dw - 3 - cw2;
                boolean hc = hovered(bx, ly + 5, cw2, 16, mx, my), hd = hovered(bx + cw2 + 3, ly + 5, dw, 16, mx, my);
                g.fill(bx, ly + 5, bx + cw2, ly + 21, hc ? 0xFF3A2C1C : Theme.ACTIVE);
                g.fill(bx, ly + 19, bx + cw2, ly + 21, Theme.ORANGE);
                text(g, cont, bx + 4, ly + 9, Theme.GOLD);
                if (hd)
                    g.fill(bx + cw2 + 3, ly + 5, bx + cw2 + 3 + dw, ly + 21, Theme.DANGER_BG);
                text(g, del, bx + cw2 + 7, ly + 9, hd ? Theme.ERROR : GREY);
                int nw = bx - x - in - 8;
                body(g, fitBody(sv.summary.getLevelName(), nw), x + in + 5, ly + 3, Theme.TEXT, false);
                text(g, fit(ago(sv.summary.getLastPlayed()), nw), x + in + 5, ly + 15, GREY);
                hits.add(new Hit(bx, ly + 5, cw2, 16, () -> open(sv)));
                hits.add(new Hit(bx + cw2 + 3, ly + 5, dw, 16, () -> delete(sv)));
            }
        }

        // Play: a map plays a new save (a copy), anything else opens as it is
        boolean canPlay = !s.isLocked() && !s.isDisabled() && !cloning;
        boolean hov = canPlay && hovered(x + in, playY, iw, playH, mx, my);
        if (canPlay) {
            g.fill(x + in - 1, playY + playH + 1, x + in + iw + 1, playY + playH + 3, 0xFF7A2C06);
            Theme.outline(g, x + in, playY, iw, playH, 1, Theme.ON_ORANGE);
            g.fill(x + in, playY, x + in + iw, playY + playH, Theme.ORANGE);
            g.fill(x + in, playY, x + in + iw, playY + 3, Theme.GOLD);
            g.fill(x + in, playY + playH - 3, x + in + iw, playY + playH, Theme.ORANGE_DARK);
            if (hov)
                g.fill(x + in, playY, x + in + iw, playY + playH, Theme.HOVER);
        } else
            Theme.readyButton(g, x + in, playY, iw, playH, Theme.ReadyState.DISABLED, false);
        String label = cloning ? "COPYING MAP…" : e.isMap() ? "PLAY" : e.save != null ? "RESUME" : "OPEN";
        String sub = e.isMap() ? "NEW SAVE · THE MAP STAYS UNTOUCHED" : e.save != null ? "CONTINUE WHERE YOU LEFT OFF" : "OPENS THIS WORLD";
        int lh = 12;
        String l = fitPix(label, lh, iw - 8);
        pix(g, l, x + in + (iw - pixWidth(l, lh)) / 2, playY + 5, lh, canPlay ? DARK : new int[]{ Theme.DIM, Theme.DIM, Theme.DIM, Theme.DIM }, 0);
        String sb = fit(sub, iw - 8);
        text(g, sb, x + in + (iw - tw(sb)) / 2, playY + 21, canPlay ? Theme.ON_ORANGE : Theme.DIM);
        if (canPlay)
            hits.add(new Hit(x + in, playY, iw, playH, () -> primary(e)));

        // Continue the latest save, or New save when the big button doesn't make one, Edit (a map opens its original) and Delete
        int by = playY + playH + 6;
        boolean cont = e.isMap() && !e.saves.isEmpty();
        boolean newSave = !e.isMap() && e.save == null;
        int n = cont || newSave ? 3 : 2;
        int bw = (iw - 4 * (n - 1)) / n, bx = x + in;
        if (cont || newSave) {
            button(g, cont ? "CONTINUE" : "NEW SAVE", bx, by, bw, rowH, mx, my, () -> { if (cont) open(e.saves.get(0)); else playMap(e); });
            bx += bw + 4;
        }
        button(g, e.isMap() ? "EDIT MAP" : "EDIT", bx, by, bw, rowH, mx, my, () -> { if (e.isMap()) open(e); else edit(e); });
        bx += bw + 4;
        button(g, e.isMap() ? "DELETE MAP" : "DELETE", bx, by, x + in + iw - bx, rowH, mx, my, () -> delete(e));
    }
}
