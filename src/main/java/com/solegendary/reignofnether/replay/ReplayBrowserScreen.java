package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.ui.Backdrop;
import com.solegendary.reignofnether.ui.Clip;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Stream;

/**
 * Title screen popup listing the saved replays (newest first) with Watch and Delete, and a button that opens the
 * replays folder so they can be shared. Laid out in HUD units like MapPackScreen.
 */
public class ReplayBrowserScreen extends Screen {

    private static final int PAD = 16, ROW_H = 48, BUTTON_H = 28, SMALL_BUTTON_H = 18;

    private record Hit(int x, int y, int w, int h, Runnable action) { }

    // a header, or just the file when it can't be read
    private record Entry(Path file, @Nullable ReplayHeader header) { }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final List<Entry> entries = new ArrayList<>();
    @Nullable private Path confirmDelete = null;
    private int scroll = 0, maxScroll = 0;

    public ReplayBrowserScreen(Screen parent) {
        super(Component.translatable("replay.reignofnether.title"));
        this.parent = parent;
        load();
    }

    private void load() {
        entries.clear();
        if (!Files.isDirectory(ReplayHeader.dir()))
            return;
        try (Stream<Path> files = Files.list(ReplayHeader.dir())) {
            files.filter(p -> p.getFileName().toString().endsWith(ReplayHeader.EXTENSION))
                    .sorted(Comparator.comparingLong(ReplayBrowserScreen::modified).reversed())
                    .forEach(p -> {
                        ReplayHeader h = null;
                        try {
                            h = ReplayHeader.read(p);
                        } catch (IOException | RuntimeException e) {
                            ReignOfNether.LOGGER.warn("[Replay] can't read {}", p, e);
                        }
                        entries.add(new Entry(p, h));
                    });
        } catch (IOException e) {
            ReignOfNether.LOGGER.warn("[Replay] can't list {}", ReplayHeader.dir(), e);
        }
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Backdrop.render(g, width, height, 0.6f);
        float k = Text.guiPerUnit();
        int screenW = Math.round(width / k), screenH = Math.round(height / k);
        int mx = Math.round(mouseX / k), my = Math.round(mouseY / k);
        hits.clear();

        int w = Math.min(640, screenW - 32);
        int listH = Math.max(ROW_H, Math.min(Math.max(1, entries.size()) * ROW_H, screenH - 150));
        int h = PAD + 12 + 24 + 8 + listH + PAD + BUTTON_H + PAD;
        int x = (screenW - w) / 2, y = Math.max(8, (screenH - h) / 2);

        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        Theme.popup(g, x, y, w, h, Theme.ORANGE);
        int tx = x + PAD, ty = y + PAD;
        Text.small(g, tr("replay.reignofnether.kicker", entries.size()), tx, ty, Theme.ORANGE);
        Text.title(g, title.getString().toUpperCase(), tx, ty + 12, 2f);

        int listY = ty + 12 + 24 + 8, rowW = w - 2 * PAD;
        maxScroll = Math.max(0, entries.size() * ROW_H - listH);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (entries.isEmpty())
            Text.draw(g, tr("replay.reignofnether.empty"), tx, listY + 4, Theme.DESC);
        Clip.push(g, tx, listY, rowW, listH);
        for (int i = 0; i < entries.size(); i++) {
            int ry = listY + i * ROW_H - scroll;
            if (ry + ROW_H <= listY || ry >= listY + listH)
                continue;
            row(g, entries.get(i), tx, ry, rowW, mx, my, my >= listY && my < listY + listH);
        }
        Clip.pop(g);

        int by = y + h - PAD - BUTTON_H;
        int closeW = Math.max(100, Text.mediumWidth(tr("replay.reignofnether.close")) + 24);
        button(g, tr("replay.reignofnether.open_folder"), tx, by, rowW - closeW - 8, BUTTON_H, false, mx, my, true,
                ReplayClientEvents::openReplaysFolder);
        button(g, tr("replay.reignofnether.close"), tx + rowW - closeW, by, closeW, BUTTON_H, false, mx, my, true, this::onClose);
        g.pose().popPose();
    }

    private void row(GuiGraphics g, Entry e, int x, int y, int w, int mx, int my, boolean clickable) {
        Theme.card(g, x, y + 2, w, ROW_H - 4);
        ReplayHeader h = e.header();
        int bw = 64, bx = x + w - 8 - bw;
        boolean confirming = e.file().equals(confirmDelete);
        // buttons on the right: Watch, Delete (asks once)
        if (h != null)
            button(g, tr("replay.reignofnether.watch"), bx - 6 - bw, y + (ROW_H - SMALL_BUTTON_H) / 2, bw, SMALL_BUTTON_H, true, mx, my, clickable,
                    () -> ReplayClientEvents.watch(h));
        button(g, tr(confirming ? "replay.reignofnether.confirm_delete" : "replay.reignofnether.delete"), bx,
                y + (ROW_H - SMALL_BUTTON_H) / 2, bw, SMALL_BUTTON_H, false, mx, my, clickable, () -> delete(e.file()));
        int textW = bx - 6 - bw - 8 - (x + 8);

        if (h == null) {
            Text.draw(g, Text.fit(e.file().getFileName().toString(), textW), x + 8, y + 8, Theme.TEXT);
            Text.small(g, tr("replay.reignofnether.unreadable"), x + 8, y + 22, Theme.ERROR);
            return;
        }
        Text.draw(g, Text.fit(h.map, textW), x + 8, y + 7, Theme.TEXT);
        Text.small(g, Text.fitSmall(matchup(h), textW), x + 8, y + 20, Theme.DESC);
        String when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(h.date));
        String info = when + "  ·  " + TimeUtils.getTimeStrFromTicks(h.durationTicks) + "  ·  ";
        Text.small(g, info, x + 8, y + 32, Theme.LABEL);
        int vx = x + 8 + Text.smallWidth(info);
        if (h.sameVersion()) {
            Text.small(g, "v" + h.modVersion, vx, y + 32, Theme.DIM);
        } else {
            String warn = tr("replay.reignofnether.version_badge", h.modVersion);
            g.fill(vx - 2, y + 30, vx + Text.smallWidth(warn) + 2, y + 40, Theme.ACTIVE);
            Theme.ring(g, vx - 2, y + 30, Text.smallWidth(warn) + 4, 10, 1, Theme.ORANGE);
            Text.small(g, warn, vx, y + 32, Theme.GOLD);
        }
    }

    /** "A, B vs C" by team, the winners named after it. */
    private static String matchup(ReplayHeader h) {
        Map<Integer, List<String>> teams = new LinkedHashMap<>();
        List<String> winners = new ArrayList<>();
        for (ReplayHeader.PlayerRow p : h.players) {
            ResourceLocation key = p.faction == null ? null : ResourceLocation.tryParse(p.faction);
            Faction faction = key == null ? null : Factions.getFaction(key);
            String f = faction != null && faction != Factions.NONE ? " (" + MiscUtil.getFactionName(faction) + ")" : "";
            teams.computeIfAbsent(p.team, t -> new ArrayList<>()).add(p.name + f);
            if (p.winner)
                winners.add(p.name);
        }
        StringBuilder sb = new StringBuilder();
        for (List<String> team : teams.values())
            sb.append(sb.isEmpty() ? "" : "  vs  ").append(String.join(", ", team));
        if (!winners.isEmpty())
            sb.append("  —  ").append(tr("replay.reignofnether.won", String.join(", ", winners)));
        return sb.toString();
    }

    private void delete(Path file) {
        if (!file.equals(confirmDelete)) {
            confirmDelete = file;
            return;
        }
        confirmDelete = null;
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            ReignOfNether.LOGGER.warn("[Replay] can't delete {}", file, e);
        }
        load();
    }

    private void button(GuiGraphics g, String label, int x, int y, int w, int h, boolean primary, int mx, int my,
                        boolean clickable, Runnable action) {
        boolean hov = clickable && mx >= x && mx < x + w && my >= y && my < y + h;
        if (primary)
            Theme.primaryButton(g, x, y, w, h, hov);
        else
            Theme.secondaryButton(g, x, y, w, h, hov);
        Text.mediumCentered(g, label, x + w / 2, y + (h - 8) / 2, primary ? Theme.ON_ORANGE : hov ? Theme.GOLD : Theme.BODY);
        if (clickable)
            hits.add(new Hit(x, y, w, h, action));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float k = Text.guiPerUnit();
        double mx = mouseX / k, my = mouseY / k;
        if (button == 0)
            for (Hit h : List.copyOf(hits))
                if (mx >= h.x && mx < h.x + h.w && my >= h.y && my < h.y + h.h) {
                    h.action.run();
                    return true;
                }
        confirmDelete = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (delta * ROW_H / 2)));
        return true;
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
