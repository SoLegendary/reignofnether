package com.solegendary.reignofnether.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.commands.rtsapi.RonIds;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.soldier.SoldierHudClientEvents;
import com.solegendary.reignofnether.hud.PortraitRendererUnit;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.SelectionPanel;
import com.solegendary.reignofnether.hud.panels.TopLeftPanel;
import com.solegendary.reignofnether.loading.LoadingScreenClientEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import com.solegendary.reignofnether.util.WorldState;
import com.solegendary.reignofnether.util.WorldToScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Campaign and tutorial guidance sent by map scripts (/rtsapi say, /rtsapi highlight): an NPC dialogue box that
 * takes the place of the recent chat lines, and a spotlight on one part of the HUD or the world.
 */
public class GuideClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // speaker: an entity type drawn like the HUD's unit portrait; head: a head texture when there's none
    // wait: SPACE doesn't close it, the script does (rtsapi say <players> next); callback: id of its "then" file, -1 none
    private record Line(@Nullable EntityType<?> speaker, @Nullable ResourceLocation head, String name, Component text, int length,
                        boolean waits, int callback) {
        boolean hasPortrait() {
            return speaker != null || head != null;
        }
    }

    // the speaker shown in the portrait, made once per type, looking around like the selected unit's
    private static final Map<EntityType<?>, LivingEntity> speakers = WorldState.client(new HashMap<>());
    private static final PortraitRendererUnit<?, ?, ?> portrait = new PortraitRendererUnit<>();

    private static final Deque<Line> lines = WorldState.client(new ArrayDeque<>());
    private static int typed = 0; // characters of the current line shown so far
    @Nullable
    private static Line announced = null; // the line whose callback was sent, so it's sent once

    // input events the map listens for: only these are sent to the server
    private static final Set<String> listened = WorldState.client(new HashSet<>());

    private static String target = "";
    @Nullable
    private static Component callout = null;
    private static BlockPos pos = BlockPos.ZERO;

    static {
        WorldState.onClientReset(GuideClientEvents::clearHighlight);
        WorldState.onClientReset(() -> {
            readySent = false;
            worldTicks = 0;
        });
    }

    private static final int DIALOGUE_W = 260;
    private static final int PAD = 6;
    private static final int HEAD = 32;
    private static final int PORTRAIT_W = 40; // the HUD unit portrait's proportions, a little smaller
    private static final int PORTRAIT_H = 48;
    private static final float NAME_SCALE = Text.px(20);
    private static final int CHARS_PER_TICK = 2;

    private static final Button NEXT_BUTTON = new ButtonBuilder("guide_next").onLeftClick(GuideClientEvents::next).build();

    // text is a lang key or plain text (§ colour codes work); head is a unit or mob (villager, zombie, piglin_brute...),
    // shown like the HUD's unit portrait, else a mob head name or texture location, or none for no portrait;
    // an empty name ("") leaves the name line out
    public static void say(String head, String name, String text, boolean waits, int callback) {
        Component c = text(text);
        EntityType<?> speaker = head.equals("none") ? null : speakerType(head);
        ResourceLocation tex = head.equals("none") || speaker != null ? null
                : head.contains(":") ? ResourceLocation.tryParse(head) : MiscUtil.mobHeadIcon(head);
        lines.add(new Line(speaker, tex, text(name).getString(), c, ChatFormatting.stripFormatting(c.getString()).length(), waits, callback));
    }

    private static Component text(String keyOrText) {
        return Component.literal(RTSMapInfoClientEvents.mapText(keyOrText));
    }

    // "villager" is the villager unit, "minecraft:villager" or a mob without a unit the vanilla one
    @Nullable
    private static EntityType<?> speakerType(String head) {
        ResourceLocation id = head.contains(":") ? ResourceLocation.tryParse(head)
                : RonIds.entityId(ResourceLocation.fromNamespaceAndPath(
                        ReignOfNether.MOD_ID, head));
        return id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id) ? ForgeRegistries.ENTITY_TYPES.getValue(id) : null;
    }

    @Nullable
    private static LivingEntity speaker(EntityType<?> type) {
        if (MC.level == null)
            return null;
        return speakers.computeIfAbsent(type, t -> t.create(MC.level) instanceof LivingEntity le ? le : null);
    }

    public static void clearDialogue() {
        lines.clear();
        typed = 0;
        announced = null;
    }

    public static void listen(String events) {
        listened.clear();
        for (String e : events.split(","))
            if (!e.isBlank())
                listened.add(e);
    }

    /** Tells the server about an input a script listens for: event is e.g. "unit_selected", detail "villager". */
    public static void input(String event, @Nullable String detail) {
        if (listened.contains(event))
            GuideServerboundPacket.input(detail == null || detail.isEmpty() ? event : event + "/" + detail);
    }

    public static void highlight(String target, String text, BlockPos pos) {
        GuideClientEvents.target = target;
        GuideClientEvents.callout = text.isEmpty() ? null : text(text);
        GuideClientEvents.pos = pos;
    }

    public static void clearHighlight() {
        target = "";
        callout = null;
    }

    // a highlighted button clears itself once used
    public static void buttonUsed(Button b) {
        if (target.startsWith("button:")) {
            String name = target.substring("button:".length());
            if (b.name.equalsIgnoreCase(name) || b.name.equals(I18n.get(name)))
                clearHighlight();
        }
    }

    // a highlighted unit or building clears itself once the player selects it
    private static void clearIfSelected() {
        if (target.startsWith("entity:")) {
            int id = Integer.parseInt(target.substring("entity:".length()));
            if (UnitClientEvents.getSelectedUnits().stream().anyMatch(e -> e.getId() == id))
                clearHighlight();
        } else if (target.equals("building")) {
            if (BuildingClientEvents.getSelectedBuildings().stream()
                    .anyMatch(b -> b.isPosInsideBuilding(pos) || b.originPos.equals(pos) || b.centrePos.equals(pos)))
                clearHighlight();
        }
    }

    public static boolean isTalking() {
        return !lines.isEmpty();
    }

    /** Finishes typing the current line, or moves to the next one; the last one tells the server it was read. */
    private static void next() {
        Line line = lines.peek();
        if (line == null)
            return;
        if (typed < line.length) {
            typed = line.length;
            return;
        }
        if (line.waits) // until its condition, which the script checks
            return;
        advance();
    }

    // the script's rtsapi say <players> next: past the current line, waiting or not
    public static void forceNext() {
        if (!lines.isEmpty())
            advance();
    }

    private static void advance() {
        lines.poll();
        typed = 0;
        if (lines.isEmpty())
            GuideServerboundPacket.dialogueEnded();
    }

    private static int worldTicks = 0; // 0 once READY was sent, reset with the world
    private static boolean readySent = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END && !readySent && MC.player != null && MC.level != null
                && ++worldTicks > 20 && LoadingScreenClientEvents.isDone()) {
            readySent = true;
            GuideServerboundPacket.ready();
        }
        Line line = lines.peek();
        if (evt.phase == TickEvent.Phase.END && !target.isEmpty())
            clearIfSelected();
        if (evt.phase == TickEvent.Phase.END)
            sendInputs();
        if (evt.phase == TickEvent.Phase.END && line != null && line != announced) {
            announced = line;
            if (line.callback >= 0)
                GuideServerboundPacket.lineShown(line.callback);
        }
        if (evt.phase == TickEvent.Phase.END && line != null && typed < line.length)
            typed = Math.min(line.length, typed + CHARS_PER_TICK);
        if (evt.phase == TickEvent.Phase.END && line != null && line.speaker != null)
            portrait.tickAnimation();
    }

    // what was selected last tick and where the camera was last reported, for the input events
    private static Set<Integer> lastUnits = new HashSet<>();
    private static Set<BlockPos> lastBuildings = new HashSet<>();
    @Nullable
    private static Vec3 lastCamera = null;
    private static long lastCameraMs = 0;

    // unit_selected/<unit>, box_selected (several at once), building_selected/<building>, camera_moved
    private static void sendInputs() {
        if (listened.isEmpty() || MC.player == null)
            return;
        var units = UnitClientEvents.getSelectedUnits();
        Set<Integer> unitIds = new HashSet<>();
        Set<String> newTypes = new LinkedHashSet<>();
        int added = 0;
        for (var e : units) {
            unitIds.add(e.getId());
            if (!lastUnits.contains(e.getId())) {
                added++;
                ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
                if (key != null)
                    newTypes.add(RonIds.prodId(key).getPath());
            }
        }
        lastUnits = unitIds;
        newTypes.forEach(type -> input("unit_selected", type));
        if (added > 1)
            input("box_selected", null);

        Set<BlockPos> buildings = new HashSet<>();
        for (var b : BuildingClientEvents.getSelectedBuildings()) {
            buildings.add(b.originPos);
            if (!lastBuildings.contains(b.originPos)) {
                ResourceLocation id = RonIds.of(b.getBuilding());
                input("building_selected", id.getNamespace().equals(RonIds.CUSTOM)
                        ? "custom_" + id.getPath() : id.getPath());
            }
        }
        lastBuildings = buildings;

        // camera_moved: at most once a second, after it moved 8 blocks
        Vec3 cam = MC.player.position();
        long now = System.currentTimeMillis();
        if (lastCamera == null)
            lastCamera = cam;
        else if (now - lastCameraMs > 1000 && cam.distanceToSqr(lastCamera) > 64) {
            lastCamera = cam;
            lastCameraMs = now;
            input("camera_moved", null);
        }
    }

    // key_pressed/<key>: the key's name, e.g. key_pressed/1 (control group), key_pressed/grave_accent (deselect)
    @SubscribeEvent
    public static void onAnyKey(ScreenEvent.KeyPressed.Post evt) {
        if (listened.contains("key_pressed") && evt.getScreen() instanceof TopdownGui) {
            String name = InputConstants.getKey(evt.getKeyCode(), evt.getScanCode()).getName();
            input("key_pressed", name.replaceFirst("^key\\.(keyboard|mouse)\\.", "").replace('.', '_').toLowerCase(Locale.ROOT));
        }
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (isTalking() && evt.getKeyCode() == GLFW.GLFW_KEY_SPACE && MC.screen instanceof TopdownGui
                && !TextInputClientEvents.isAnyInputFocused()) {
            next();
            evt.setCanceled(true);
        }
    }

    // first person (a soldier) has no screen and SPACE jumps: ENTER reads on there
    @SubscribeEvent
    public static void onWorldKey(InputEvent.Key evt) {
        if (isTalking() && MC.screen == null && evt.getAction() == GLFW.GLFW_PRESS && evt.getKey() == GLFW.GLFW_KEY_ENTER
                && SoldierHudClientEvents.active())
            next();
    }

    private static String nextKey() {
        return MC.screen == null && SoldierHudClientEvents.active() ? "ENTER" : "SPACE";
    }

    // ------------------------------------------------------------------ dialogue

    /** The current line in place of the recent chat lines, bottom edge at bottom. False when nobody is talking. */
    public static boolean renderDialogue(HudFrame f, int x, int bottom) {
        Line line = lines.peek();
        if (line == null)
            return false;
        LivingEntity speaker = line.speaker != null ? speaker(line.speaker) : null;
        int portraitW = speaker != null ? PORTRAIT_W : HEAD;
        int portraitH = speaker != null ? PORTRAIT_H : HEAD;
        int headW = line.hasPortrait() ? portraitW + PAD : 0;
        int textX = x + PAD + headW;
        int textW = DIALOGUE_W - headW - PAD * 2;
        List<FormattedCharSequence> wrapped = Text.wrap(line.text, textW);
        int nameH = line.name.isEmpty() ? 0 : Text.titleHeight(NAME_SCALE) + 3;
        int h = Math.max(line.hasPortrait() ? portraitH : 0, nameH + wrapped.size() * Text.lineHeight() + 10) + PAD * 2;
        int y = bottom - h;
        Rect r = new Rect(x, y, DIALOGUE_W, h);

        floatingPopup(f, r);
        if (speaker != null) {
            SelectionPanel.drawPortrait(f, speaker, portrait, x + PAD, y + PAD, portraitW, portraitH);
        } else if (line.head != null) {
            Theme.outline(f.g, x + PAD, y + PAD, HEAD, HEAD, 1, Theme.INK);
            MyRenderer.renderIcon(f.g, line.head, x + PAD, y + PAD, HEAD);
        }
        if (!line.name.isEmpty())
            Text.heading(f.g, line.name, textX, y + PAD, Theme.GOLD, NAME_SCALE);

        int left = typed;
        for (int i = 0; i < wrapped.size() && left > 0; i++) {
            Text.draw(f.g, firstChars(wrapped.get(i), left), textX, y + PAD + nameH + i * Text.lineHeight(), Theme.BODY);
            left -= length(wrapped.get(i));
        }

        int hintY = r.bottom() - PAD - 7;
        if (line.waits && typed >= line.length) { // done when the player does what it says
            Text.smallRight(f.g, I18n.get("hud.reignofnether.guide.waiting"), r.right() - PAD, hintY - 1, Theme.GOLD);
        } else {
            String hint = I18n.get(lines.size() > 1 ? "hud.reignofnether.guide.next" : "hud.reignofnether.guide.close");
            String key = nextKey();
            int keyW = Text.smallWidth(key) + 3;
            if (typed >= line.length || MiscUtil.getOscillatingFloat(0, 1) > 0.5f)
                Theme.keyCap(f.g, key, r.right() - PAD - keyW, hintY, Theme.TEXT);
            Text.smallRight(f.g, hint, r.right() - PAD - keyW - 4, hintY - 1, Theme.LABEL);
        }
        if (lines.size() > 1)
            Text.small(f.g, "+" + (lines.size() - 1), textX, hintY - 1, Theme.DIM);

        f.register(NEXT_BUTTON, r, null, null);
        return true;
    }

    // Theme.popup's orange top edge on the see-through fill of the HUD's cards (solid when the player turned that off)
    private static void floatingPopup(HudFrame f, Rect r) {
        Theme.bevel(f.g, r.x(), r.y(), r.w(), r.h(), Theme.floating(), Theme.ORANGE, 0, 1, Theme.INK);
    }

    private static int length(FormattedCharSequence s) {
        int[] n = { 0 };
        s.accept((i, style, cp) -> { n[0]++; return true; });
        return n[0];
    }

    private static FormattedCharSequence firstChars(FormattedCharSequence s, int count) {
        return sink -> {
            int[] n = { 0 };
            return s.accept((i, style, cp) -> n[0]++ < count && sink.accept(i, style, cp));
        };
    }

    // ------------------------------------------------------------------ highlight

    /** Dims the screen around the target, rings it and shows the callout. Drawn in HUD units after the panels. */
    public static void renderHighlight(HudFrame f) {
        Rect r = targetRect(f);
        if (r == null || r.isEmpty())
            return;
        int pad = 3;
        Rect hole = new Rect(r.x() - pad, r.y() - pad, r.w() + pad * 2, r.h() + pad * 2);
        int w = f.layout.width();
        int h = f.layout.height();
        int shade = Theme.withAlpha(Theme.INK, 0.55f);
        f.g.fill(0, 0, w, hole.y(), shade);
        f.g.fill(0, hole.bottom(), w, h, shade);
        f.g.fill(0, hole.y(), hole.x(), hole.bottom(), shade);
        f.g.fill(hole.right(), hole.y(), w, hole.bottom(), shade);

        Theme.outline(f.g, hole.x(), hole.y(), hole.w(), hole.h(), 1, Theme.ON_ORANGE);
        Theme.outline(f.g, hole.x() - 1, hole.y() - 1, hole.w() + 2, hole.h() + 2, 2, Theme.ORANGE);
        // a gold ring that grows out and fades, once a second
        float t = MiscUtil.getOscillatingFloat(0, 1);
        int grow = 4 + Math.round(t * 6);
        Theme.outline(f.g, hole.x() - grow, hole.y() - grow, hole.w() + grow * 2, hole.h() + grow * 2, 1,
                Theme.withAlpha(Theme.GOLD, 1 - t));

        if (callout == null)
            return;
        int cw = 150;
        List<FormattedCharSequence> wrapped = Text.wrap(callout, cw - PAD * 2);
        int ch = wrapped.size() * Text.lineHeight() + PAD * 2;
        boolean below = r.centerY() < h / 2;
        int cx = Math.max(4, Math.min(hole.x(), w - cw - 4));
        int cy = below ? hole.bottom() + 12 : hole.y() - 12 - ch;
        floatingPopup(f, new Rect(cx, cy, cw, ch));
        for (int i = 0; i < wrapped.size(); i++)
            Text.draw(f.g, wrapped.get(i), cx + PAD, cy + PAD + i * Text.lineHeight(), Theme.TEXT);
    }

    @Nullable
    private static Rect targetRect(HudFrame f) {
        if (target.isEmpty())
            return null;
        if (target.startsWith("button:")) {
            String name = target.substring("button:".length());
            String translated = I18n.get(name);
            for (HudFrame.Placed p : f.placed())
                if (!p.button().isHidden.get() && (p.button().name.equalsIgnoreCase(name) || p.button().name.equals(translated)))
                    return p.rect();
            return null;
        }
        if (target.startsWith("entity:")) { // follows the unit while it moves
            var e = MC.level == null ? null : MC.level.getEntity(Integer.parseInt(target.substring("entity:".length())));
            return e == null ? null : worldRect(f, e.position().add(0, e.getBbHeight() / 2, 0), 20);
        }
        return switch (target) {
            case "resources" -> TopLeftPanel.resourcesRect;
            case "command_card" -> f.layout.commandCard();
            case "selection" -> SelectionPanel.isEmpty() ? null
                    : new Rect(f.layout.selection().x(), f.layout.selection().y(), SelectionPanel.width(f), f.layout.selection().h());
            case "minimap" -> f.layout.minimap();
            case "tools" -> f.layout.tools();
            case "console" -> f.layout.console();
            case "building" -> worldRect(f, Vec3.atCenterOf(pos), 32);
            case "pos" -> {
                Vec2 p = WorldToScreen.project(Vec3.atCenterOf(pos));
                if (p == null)
                    yield null;
                int size = 24;
                yield new Rect(f.scale.toUnits(p.x) - size / 2, f.scale.toUnits(p.y) - size / 2, size, size);
            }
            default -> null;
        };
    }

    @Nullable
    private static Rect worldRect(HudFrame f, Vec3 at, int size) {
        Vec2 p = WorldToScreen.project(at);
        return p == null ? null : new Rect(f.scale.toUnits(p.x) - size / 2, f.scale.toUnits(p.y) - size / 2, size, size);
    }
}
