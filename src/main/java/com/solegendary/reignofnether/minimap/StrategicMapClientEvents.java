package com.solegendary.reignofnether.minimap;

import com.mojang.math.Axis;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import com.solegendary.reignofnether.util.WorldToScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * What zooming out past the camera's limit, or the satellite key, shows: the camera tilts to look straight down,
 * keeping its orientation, and zooms out as far as the world is drawn, with unit icons and building rectangles over the
 * real view. The camera stays a normal RTS camera, so selecting, turning it and giving orders work as usual.
 */
public final class StrategicMapClientEvents {

    private StrategicMapClientEvents() { }

    private static final Minecraft MC = Minecraft.getInstance();
    private static final float TURN_MS = 400;
    private static final float ICONS_FROM = 0.6f;
    private static final int UNIT_ICON = 8;
    // the furthest zoom is a wall: scrolling stops there, and only separate pushes after a pause open this view.
    // Counting scroll events isn't enough, one flick of a smooth wheel or trackpad sends dozens of them
    private static final int PUSHES_TO_OPEN = 2;
    private static final long PUSH_PAUSE_MS = 350;

    private static boolean open = false;
    private static float blendFrom = 0;
    private static long changedMs = 0;
    private static int pushes = 0;
    private static long lastScrollMs = 0;

    public static boolean isOpen() {
        return open;
    }

    /** A scroll out at the furthest zoom. */
    public static void scrollPast() {
        long now = System.currentTimeMillis();
        boolean newPush = now - lastScrollMs > PUSH_PAUSE_MS;
        lastScrollMs = now;
        if (newPush && ++pushes >= PUSHES_TO_OPEN)
            open();
    }

    /** A scroll that zoomed the camera: whatever comes right after it is the same scroll running on. */
    public static void cancelScroll() {
        pushes = 0;
        lastScrollMs = System.currentTimeMillis();
    }

    private static void set(boolean value) {
        pushes = 0;
        blendFrom = blend();
        changedMs = System.currentTimeMillis();
        open = value;
    }

    public static void open() {
        if (MC.screen instanceof TopdownGui)
            set(true);
    }

    public static void close() {
        set(false);
    }

    public static void toggle() {
        if (open)
            close();
        else
            open();
    }

    public static void reset() {
        open = false;
        pushes = 0;
        lastScrollMs = 0;
        blendFrom = 0;
        changedMs = 0;
    }

    /** 0 is the normal camera, 1 is looking straight down. */
    public static float blend() {
        float t = Math.min(1, (System.currentTimeMillis() - changedMs) / TURN_MS);
        return Mth.lerp(t, blendFrom, open ? 1 : 0);
    }

    /** Draws under the rest of the HUD, in HUD units. */
    public static void render(HudFrame f) {
        if (blend() < ICONS_FROM)
            return;
        for (BuildingPlacement b : BuildingClientEvents.getBuildings())
            if (b.isExploredClientside)
                footprint(f, b);
        for (LivingEntity e : UnitClientEvents.getAllUnits())
            if (FogOfWarClientEvents.isInBrightChunk(e))
                icon(f, e);
    }

    /** How far around a unit, in blocks, its icon reaches: units this zoomed out are clicked by their icons. */
    public static float iconReach() {
        return (UNIT_ICON / 2f + 1) * OrthoviewClientEvents.getZoom() / HudGeometry.layout().height();
    }

    /** A see-through rectangle of its owner's colour over the building's whole footprint. */
    private static void footprint(HudFrame f, BuildingPlacement b) {
        double y = b.minCorner.getY();
        double x0 = b.minCorner.getX(), z0 = b.minCorner.getZ();
        double x1 = b.maxCorner.getX() + 1, z1 = b.maxCorner.getZ() + 1;
        Vec2 corner = WorldToScreen.project(new Vec3(x0, y, z0));
        Vec2 alongX = WorldToScreen.project(new Vec3(x1, y, z0));
        Vec2 alongZ = WorldToScreen.project(new Vec3(x0, y, z1));
        Vec2 centre = WorldToScreen.project(new Vec3((x0 + x1) / 2, y, (z0 + z1) / 2));
        if (corner == null || alongX == null || alongZ == null || centre == null)
            return;
        int w = f.scale.toUnits(Mth.sqrt(corner.distanceToSqr(alongX)));
        int h = f.scale.toUnits(Mth.sqrt(corner.distanceToSqr(alongZ)));
        // the camera can face any way, so the rectangle turns with the building
        f.g.pose().pushPose();
        f.g.pose().translate(f.scale.toUnits(centre.x), f.scale.toUnits(centre.y), 0);
        f.g.pose().mulPose(Axis.ZP.rotation((float) Math.atan2(alongX.y - corner.y, alongX.x - corner.x)));
        square(f, -w / 2, -h / 2, w, h, b.ownerName, BuildingClientEvents.getPreselectedBuilding() == b,
                BuildingClientEvents.getSelectedBuildings().contains(b));
        f.g.pose().popPose();
    }

    /** The unit's head on a see-through square of its owner's colour. */
    private static void icon(HudFrame f, LivingEntity e) {
        Vec2 gui = WorldToScreen.project(e.position());
        if (gui == null)
            return;
        int x = f.scale.toUnits(gui.x) - UNIT_ICON / 2;
        int y = f.scale.toUnits(gui.y) - UNIT_ICON / 2;
        square(f, x, y, UNIT_ICON, UNIT_ICON, e instanceof Unit u ? u.getOwnerName() : null,
                UnitClientEvents.getPreselectedUnits().contains(e), UnitClientEvents.getSelectedUnits().contains(e));
        MyRenderer.renderIcon(f.g, MiscUtil.mobHeadIcon(e), x, y, UNIT_ICON);
    }

    /** Stronger on hover and when selected. */
    private static void square(HudFrame f, int x, int y, int w, int h, String owner, boolean hovered, boolean selected) {
        int rgb = PlayerColors.getPlayerDisplayColorHex(owner) & 0xFFFFFF;
        f.g.fill(x, y, x + w, y + h, (hovered ? 0x50 : selected ? 0x30 : 0x18) << 24 | rgb);
        Theme.outline(f.g, x, y, w, h, 1, hovered ? 0xFFFFFFFF : selected ? Theme.GOLD : 0x60000000 | rgb);
    }
}
