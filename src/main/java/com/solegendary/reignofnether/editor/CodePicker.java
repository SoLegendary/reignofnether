package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.TopLeftPanel;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Picks something in the world or on the HUD for the code editor and types it in as plain command text: the popup
 * hides, the next left click picks (a zone takes two), right click or Esc cancels. Typing it by hand works the same.
 * What a click would pick is outlined while picking: the block, the zone so far, the unit, building or HUD element.
 */
public final class CodePicker {

    public enum Kind { ZONE, POS, UNIT, BUILDING, HUD, CAMERA }

    private static final String KEY = "editor.reignofnether.pick.";

    @Nullable private static Kind kind = null;
    @Nullable private static BlockPos corner = null; // a zone's first corner
    @Nullable private static Consumer<String> done = null; // null text: cancelled
    @Nullable private static HudFrame frame = null; // the last HUD frame, for what a click on it hits
    private static int zones = 0;
    private static boolean raw = false; // for a block's field: a zone gives just its two corners
    private static boolean swallowRelease = false;

    static {
        WorldState.onClientReset(() -> {
            kind = null;
            done = null;
            frame = null;
        });
    }

    private CodePicker() { }

    static void start(Kind k, Consumer<String> onDone) {
        start(k, false, onDone);
    }

    static void start(Kind k, boolean rawText, Consumer<String> onDone) {
        raw = rawText;
        kind = k;
        corner = null;
        done = onDone;
    }

    public static boolean isPicking() {
        return kind != null;
    }

    private static void finish(@Nullable String text) {
        Consumer<String> d = done;
        kind = null;
        done = null;
        if (d != null)
            d.accept(text);
    }

    /** A strip at the top saying what to click, drawn last in the HUD frame (it also keeps the frame for HUD picks). */
    public static void render(HudFrame f) {
        frame = f;
        if (kind == null)
            return;
        BlockPos pos = CursorClientEvents.getPreselectedBlockPos();
        String text = corner != null ? I18n.get(KEY + "zone_second", corner.getX(), corner.getY(), corner.getZ())
                : I18n.get(KEY + kind.name().toLowerCase(Locale.ROOT));
        String under = hovered(f, pos);
        if (under != null)
            text += "  ·  " + under;
        text += "  ·  " + I18n.get(KEY + "cancel");
        int w = Text.width(text) + 16, x = (f.layout.width() - w) / 2, y = 6;
        Theme.popup(f.g, x, y, w, 18, Theme.ORANGE);
        Text.draw(f.g, text, x + 8, y + 5, Theme.TEXT);
        if (kind == Kind.HUD) { // the element a click would pick, outlined
            Rect r = hudRect(f.mouseX, f.mouseY);
            if (r != null) {
                Theme.outline(f.g, r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, 1, Theme.ORANGE);
                f.g.fill(r.x(), r.y(), r.right(), r.bottom(), 0x30F19A0F);
            }
        }
    }

    // what a click would pick now, for the strip: the block, the zone's size, the unit or building, the HUD element
    @Nullable
    private static String hovered(HudFrame f, BlockPos pos) {
        return switch (kind) {
            case POS, CAMERA -> xyz(pos);
            case ZONE -> corner == null ? xyz(pos) : I18n.get(KEY + "zone_size", Math.abs(pos.getX() - corner.getX()) + 1,
                    Math.abs(pos.getY() - corner.getY()) + 1, Math.abs(pos.getZ() - corner.getZ()) + 1);
            case UNIT -> {
                List<LivingEntity> units = UnitClientEvents.getPreselectedUnits();
                yield units.isEmpty() ? null : units.get(0).getName().getString();
            }
            case BUILDING -> {
                BuildingPlacement b = BuildingClientEvents.getPreselectedBuilding();
                yield b == null ? null : b.getBuilding().name;
            }
            case HUD -> hudTarget(f.mouseX, f.mouseY);
        };
    }

    // the world side of the outline: the block or zone so far, or the unit or building under the mouse
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (kind == null || evt.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
            return;
        var pose = evt.getPoseStack();
        BlockPos pos = CursorClientEvents.getPreselectedBlockPos();
        for (AABB zone : CodeBlocks.zoneBoxes()) // the file's zones, to line new things up with
            MyRenderer.drawLineBox(pose, zone, 0.36f, 0.78f, 1f, 0.7f);
        switch (kind) {
            case POS, CAMERA -> MyRenderer.drawLineBox(pose, new AABB(pos), 1f, 0.6f, 0.06f, 1f);
            case ZONE -> {
                BlockPos from = corner != null ? corner : pos;
                AABB box = new AABB(from).minmax(new AABB(pos));
                MyRenderer.drawLineBox(pose, box, 1f, 0.6f, 0.06f, 1f);
                MyRenderer.drawBoxBottom(pose, box.inflate(0.02), 1f, 0.85f, 0.4f, 0.6f);
            }
            case UNIT -> UnitClientEvents.getPreselectedUnits().stream().findFirst()
                    .ifPresent(e -> MyRenderer.drawLineBox(pose, e.getBoundingBox().inflate(0.1), 1f, 0.6f, 0.06f, 1f));
            case BUILDING -> {
                BuildingPlacement b = BuildingClientEvents.getPreselectedBuilding();
                if (b != null)
                    MyRenderer.drawLineBox(pose, new AABB(b.minCorner).minmax(new AABB(b.maxCorner)), 1f, 0.6f, 0.06f, 1f);
            }
            default -> { }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (kind == null)
            return;
        evt.setCanceled(true);
        swallowRelease = true;
        if (evt.getButton() != GLFW.GLFW_MOUSE_BUTTON_1)
            finish(null);
        else
            pick(HudGeometry.scale().toUnits(evt.getMouseX()), HudGeometry.scale().toUnits(evt.getMouseY()));
    }

    // the release of a picking click mustn't box-select or place anything either
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre evt) {
        if (swallowRelease) {
            swallowRelease = false;
            evt.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKey(ScreenEvent.KeyPressed.Pre evt) {
        if (kind != null && evt.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
            evt.setCanceled(true);
            finish(null);
        }
    }

    private static void pick(int ux, int uy) {
        BlockPos pos = CursorClientEvents.getPreselectedBlockPos();
        switch (kind) {
            case POS -> finish(xyz(pos));
            case CAMERA -> finish("rtsapi player camera move " + xyz(pos) + " @s 0 20 " + Math.round(OrthoviewClientEvents.getZoom()));
            case ZONE -> {
                if (corner == null) {
                    corner = pos;
                    return;
                }
                finish(raw ? xyz(corner) + " " + xyz(pos) : "rtsapi zone add zone_" + (++zones) + " " + xyz(corner) + " " + xyz(pos));
            }
            case UNIT -> {
                List<LivingEntity> units = UnitClientEvents.getPreselectedUnits();
                if (!units.isEmpty())
                    finish(units.get(0).getStringUUID());
            }
            case BUILDING -> {
                BuildingPlacement b = BuildingClientEvents.getPreselectedBuilding();
                if (b != null)
                    finish("@b[x=" + b.originPos.getX() + ",y=" + b.originPos.getY() + ",z=" + b.originPos.getZ() + ",sort=nearest,limit=1]");
            }
            case HUD -> {
                String target = hudTarget(ux, uy);
                if (target != null)
                    finish(target);
            }
        }
    }

    private static String xyz(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    // what rtsapi highlight takes for what's under the mouse: a button by name, else the panel it's in
    @Nullable
    private static String hudTarget(int ux, int uy) {
        if (frame == null)
            return null;
        HudFrame.Placed placed = frame.buttonAt(ux, uy);
        if (placed != null && !placed.button().name.isBlank())
            return "button \"" + placed.button().name.replace("\"", "") + "\"";
        if (TopLeftPanel.resourcesRect != null && TopLeftPanel.resourcesRect.contains(ux, uy))
            return "resources";
        var l = frame.layout;
        Map<String, Rect> regions = Map.of("command_card", l.commandCard(), "selection", l.selection(), "minimap", l.minimap(),
                "tools", l.tools(), "console", l.console());
        return regions.entrySet().stream().filter(e -> e.getValue().contains(ux, uy)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    // the rect of what hudTarget picks there
    @Nullable
    private static Rect hudRect(int ux, int uy) {
        if (frame == null)
            return null;
        HudFrame.Placed placed = frame.buttonAt(ux, uy);
        if (placed != null && !placed.button().name.isBlank())
            return placed.rect();
        if (TopLeftPanel.resourcesRect != null && TopLeftPanel.resourcesRect.contains(ux, uy))
            return TopLeftPanel.resourcesRect;
        var l = frame.layout;
        return Stream.of(l.commandCard(), l.selection(), l.minimap(), l.tools(), l.console())
                .filter(r -> r.contains(ux, uy)).findFirst().orElse(null);
    }
}
