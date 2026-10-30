package com.solegendary.reignofnether.ping;

import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MapMarkerServerboundPacket;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.WorldToScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

// Hold the ping key (Caps Lock by default) to open a wheel around the cursor, move towards an option and
// release. Releasing without picking cancels. Pings show for allies on the minimap, in team chat and in the
// world as the FPV HUD waypoint (drawWaypoint).
public class PingClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final int WHEEL_R = 26;   // up/down distance from the centre
    private static final int SIDE_GAP = 18;  // left/right: gap between the centre and the label
    private static final int DEAD_ZONE = 12;
    // up, right, down, left
    private static final PingType[] WHEEL = { PingType.HELP, PingType.ATTACK, PingType.ENEMY, PingType.DEFEND };

    private static boolean open = false;
    private static double cx, cy;
    private static BlockPos pingPos = null;

    private static double mouseX() {
        return MC.mouseHandler.xpos() * MC.getWindow().getGuiScaledWidth() / MC.getWindow().getScreenWidth();
    }

    private static double mouseY() {
        return MC.mouseHandler.ypos() * MC.getWindow().getGuiScaledHeight() / MC.getWindow().getScreenHeight();
    }

    @Nullable
    static PingType pick(double dx, double dy) {
        if (dx * dx + dy * dy < DEAD_ZONE * DEAD_ZONE)
            return null;
        if (Math.abs(dy) >= Math.abs(dx))
            return dy < 0 ? WHEEL[0] : WHEEL[2];
        return dx > 0 ? WHEEL[1] : WHEEL[3];
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        // RTS view keeps RoN's TopdownGui screen open; any other screen (chat, menus) closes the wheel
        if (MC.player == null || (MC.screen != null && !(MC.screen instanceof TopdownGui)) || !OrthoviewClientEvents.isEnabled()) {
            open = false;
            return;
        }
        boolean down = Keybindings.ping.isDown();
        if (down && !open) {
            cx = mouseX();
            cy = mouseY();
            pingPos = MinimapClientEvents.isPointInsideMinimap(cx, cy)
                    ? MinimapClientEvents.getWorldPosOnMinimap((float) cx, (float) cy, false)
                    : CursorClientEvents.getPreselectedBlockPos();
            open = pingPos != null;
        } else if (!down && open) {
            open = false;
            PingType type = pick(mouseX() - cx, mouseY() - cy);
            if (type != null) // released without picking: cancelled
                PacketHandler.INSTANCE.sendToServer(new MapMarkerServerboundPacket(pingPos.getX(), pingPos.getY(), pingPos.getZ(), type));
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post evt) {
        GuiGraphics g = evt.getGuiGraphics();
        if (OrthoviewClientEvents.isEnabled()) // first person draws them in the world (onRenderLevel)
            for (MinimapClientEvents.MapMarker m : MinimapClientEvents.mapMarkers)
                renderWorldMarker(g, m, evt.getPartialTick());
        if (!open)
            return;
        PingType hovered = pick(mouseX() - cx, mouseY() - cy);

        // labels are ink chips in HUD units: a square in the ping's colour, then the name
        var scale = HudGeometry.scale();
        float k = scale.poseScale();
        int ucx = scale.toUnits(cx), ucy = scale.toUnits(cy);
        int r = scale.toUnits(WHEEL_R), gap = scale.toUnits(SIDE_GAP);
        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        for (int i = 0; i < WHEEL.length; i++) {
            String name = Component.translatable(WHEEL[i].key()).getString();
            int colour = 0xFF000000 | WHEEL[i].rgb;
            boolean on = WHEEL[i] == hovered;
            int w = 4 + 5 + 3 + Text.smallWidth(name) + 4, h = 11;
            // up/down are centred above/below; left/right start a gap away from the centre so long labels
            // grow outwards instead of into each other
            int x = switch (i) {
                case 1 -> ucx + gap;
                case 3 -> ucx - gap - w;
                default -> ucx - w / 2;
            };
            int y = ucy - h / 2 + (i == 0 ? -r : i == 2 ? r : 0);
            Theme.outline(g, x, y, w, h, 1, Theme.INK);
            g.fill(x, y, x + w, y + h, 0xD90B0A0F);
            if (on)
                Theme.ring(g, x, y, w, h, 1, colour);
            g.fill(x + 4, y + 3, x + 9, y + 8, colour);
            Text.small(g, name, x + 12, y + 2, on ? Theme.GOLD : Theme.TEXT);
        }
        g.pose().popPose();
        g.fill((int) cx - 1, (int) cy - 1, (int) cx + 2, (int) cy + 2, 0xFFFFFFFF);
    }

    // the ping's waypoint over its spot
    private static void renderWorldMarker(GuiGraphics g, MinimapClientEvents.MapMarker m, float partialTick) {
        drawWaypoint(g, new Vec3(m.x + 0.5, m.y + 3, m.z + 0.5), m.type, m.ageTicks + partialTick);
    }

    /**
     * The RTS view's waypoint: a beam fading downwards, a square ring pulsing out from the spot, a bobbing
     * bevelled diamond with the symbol and a chip with the name above (no distance: it means nothing from the
     * RTS camera). Shapes are drawn in the 1920x1080 design's pixels (half a HUD unit), text in HUD units.
     * Call with the GUI's own pose.
     */
    public static void drawWaypoint(GuiGraphics g, Vec3 world, PingType type, float ageTicks) {
        Vec2 p = WorldToScreen.project(world);
        if (p == null)
            return;
        float k = HudGeometry.scale().poseScale();
        int colour = 0xFF000000 | type.rgb;
        float x = p.x * 2 / k, y = p.y * 2 / k;
        float bob = -8 * (1 - (float) Math.cos(ageTicks / 32f * Math.PI * 2)) / 2; // 1.6 s

        g.pose().pushPose();
        g.pose().scale(k / 2, k / 2, 1);
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().pushPose();
        g.pose().scale(1.5f, 1, 1);
        g.fillGradient(-1, 0, 1, 220, 0xB3000000 | type.rgb, type.rgb);
        g.pose().popPose();
        // the ring: 0.4x to 2.4x, fading, every 1.4 s
        float t = ageTicks % 28 / 28f;
        float ring = 0.4f + 2 * (1 - (1 - t) * (1 - t));
        g.pose().pushPose();
        g.pose().scale(ring, ring, 1);
        Theme.outline(g, -9, -9, 18, 18, 2, Theme.withAlpha(colour, 1 - t));
        g.pose().popPose();

        g.pose().translate(0, bob, 0);
        g.pose().pushPose();
        g.pose().mulPose(Axis.ZP.rotationDegrees(45));
        for (int glow = 3; glow >= 1; glow--)
            g.fill(-14 - glow * 6, -14 - glow * 6, 14 + glow * 6, 14 + glow * 6, (0x30 >> (glow - 1)) << 24 | type.rgb);
        g.fill(-17, -17, 17, 17, Theme.INK);
        g.fill(-14, -14, 14, 14, colour);
        g.fill(-14, -14, 14, -11, 0x73FFFFFF);
        g.fill(-14, 11, 14, 14, 0x59000000);
        g.pose().popPose();
        for (int[] r : symbol(type))
            g.fill(r[0], r[1], r[2], r[3], SYMBOL);
        g.pose().popPose();
        g.pose().popPose();

        // the chip, its top 58 design pixels above the spot (29 units)
        String label = Component.translatable(type.key()).getString();
        int h = 13;
        int w = 4 + Text.smallWidth(label) + 4;
        int cx = Math.round(x / 2), top = Math.round(y / 2) - 29;
        int lx = cx - w / 2;
        g.pose().pushPose();
        g.pose().scale(k, k, 1);
        g.fill(lx, top, lx + w, top + h, 0xD90B0A0F);
        // centred on the chip: small caps centre at y + 4.5
        Text.small(g, label, lx + 4, top + Math.round(h / 2f - 4.5f), colour);
        g.pose().popPose();
    }

    // a filled diamond (a square turned 45°) of half-diagonal r
    public static void diamond(GuiGraphics g, float cx, float cy, float r, int colour) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().mulPose(Axis.ZP.rotationDegrees(45));
        float s = r / (float) Math.sqrt(2);
        g.pose().scale(s, s, 1);
        g.fill(-1, -1, 1, 1, colour);
        g.pose().popPose();
    }

    // first person: the same waypoint built in the world, so it stands on its spot with perspective
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || OrthoviewClientEvents.isEnabled())
            return;
        for (MinimapClientEvents.MapMarker m : MinimapClientEvents.mapMarkers)
            drawWaypointInWorld(evt.getPoseStack(), new Vec3(m.x + 0.5, m.y + 1, m.z + 0.5), m.type, m.ageTicks + evt.getPartialTick());
    }

    /**
     * drawWaypoint in the world: a beam up from the ground, a ring pulsing out on the ground, and the diamond and
     * chip facing the camera 2.5 blocks up. Sizes are the prototype's design pixels, at least real size up close
     * and growing with distance so a far ping stays readable. Visible through terrain.
     */
    public static void drawWaypointInWorld(PoseStack pose, Vec3 ground, PingType type, float ageTicks) {
        Camera cam = MC.gameRenderer.getMainCamera();
        Vec3 c = cam.getPosition();
        double d = c.distanceTo(ground);
        float px = (float) Math.max(0.012, d * 0.0011); // blocks per design pixel
        int colour = 0xFF000000 | type.rgb;
        float bob = -8 * (1 - (float) Math.cos(ageTicks / 32f * Math.PI * 2)) / 2;
        float height = 2.5f;
        MultiBufferSource.BufferSource buffers = MC.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(MyRenderer.QUADS_NO_DEPTH_TEST);

        pose.pushPose();
        pose.translate(ground.x - c.x, ground.y - c.y, ground.z - c.z);
        // ring on the ground: 0.4x to 2.4x, fading, every 1.4 s
        float t = ageTicks % 28 / 28f;
        float ring = 0.4f + 2 * (1 - (1 - t) * (1 - t));
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90));
        float r = 0.5f * ring, w = 0.08f * ring;
        int ringColour = Theme.withAlpha(colour, 1 - t);
        Matrix4f m = pose.last().pose();
        quad(vc, m, -r, -r, r, -r + w, ringColour, ringColour);
        quad(vc, m, -r, r - w, r, r, ringColour, ringColour);
        quad(vc, m, -r, -r + w, -r + w, r - w, ringColour, ringColour);
        quad(vc, m, r - w, -r + w, r, r - w, ringColour, ringColour);
        pose.popPose();
        // beam: turned to face the camera around the vertical only, fading towards the ground
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-cam.getYRot()));
        float bw = 1.5f * px;
        quad(vc, pose.last().pose(), -bw, height, bw, 0, 0xB3000000 | type.rgb, type.rgb);
        pose.popPose();

        // diamond and chip: a camera-facing card in design pixels, y down like the GUI
        pose.translate(0, height, 0);
        pose.mulPose(cam.rotation());
        pose.scale(-px, -px, px);
        pose.pushPose();
        pose.translate(0, bob, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(45));
        m = pose.last().pose();
        for (int glow = 3; glow >= 1; glow--) {
            float gr = 14 + glow * 6;
            quad(vc, m, -gr, -gr, gr, gr, (0x30 >> (glow - 1)) << 24 | type.rgb, (0x30 >> (glow - 1)) << 24 | type.rgb);
        }
        quad(vc, m, -17, -17, 17, 17, Theme.INK, Theme.INK);
        quad(vc, m, -14, -14, 14, 14, colour, colour);
        quad(vc, m, -14, -14, 14, -11, 0x73FFFFFF, 0x73FFFFFF);
        quad(vc, m, -14, 11, 14, 14, 0x59000000, 0x59000000);
        pose.popPose();
        pose.pushPose();
        pose.translate(0, bob, 0);
        for (int[] s : symbol(type))
            quad(vc, pose.last().pose(), s[0], s[1], s[2], s[3], SYMBOL, SYMBOL);
        pose.popPose();

        Font font = MC.font;
        Component label = Component.literal(Component.translatable(type.key())
                .getString().toUpperCase(Locale.ROOT)).withStyle(Text.SMALL_STYLE.withColor(type.rgb));
        Component dist = Component.literal(Math.round(d) + " m").withStyle(s -> s.withColor(Theme.TEXT & 0xFFFFFF));
        float k = 1.5f; // both fonts at the prototype's 10/15 px
        float lw = font.width(label) * k, dw = font.width(dist) * k;
        float chipW = 8 + lw + 8 + dw + 8, chipH = 27, top = -58;
        quad(vc, pose.last().pose(), -chipW / 2, top, chipW / 2, top + chipH, 0xD90B0A0F, 0xD90B0A0F);
        buffers.endBatch(MyRenderer.QUADS_NO_DEPTH_TEST);

        int light = LightTexture.FULL_BRIGHT;
        pose.pushPose();
        pose.translate(-chipW / 2 + 8, top + (chipH - 8 * k) / 2, -0.1f);
        pose.scale(k, k, 1);
        // caps centres line up: Silkscreen's at 4.5 font pixels down, the vanilla font's at 3.5
        font.drawInBatch(label, 0, -0.5f, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, light);
        font.drawInBatch(dist, (lw + 8) / k, 0.5f, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, light);
        pose.popPose();
        buffers.endBatch();
        pose.popPose();
    }

    private static final int SYMBOL = 0xFF380000;

    /**
     * The ping's symbol in the diamond as pixel rectangles {x0, y0, x1, y1}, design pixels around the centre:
     * the font's ◎ □ ⚔ ◇ come from the unicode fallback and smear.
     */
    private static java.util.List<int[]> symbol(PingType type) {
        return SYMBOLS.computeIfAbsent(type, PingClientEvents::buildSymbol);
    }

    private static final java.util.Map<PingType, java.util.List<int[]>> SYMBOLS = new java.util.EnumMap<>(PingType.class);

    private static java.util.List<int[]> buildSymbol(PingType type) {
        java.util.List<int[]> r = new java.util.ArrayList<>();
        switch (type) {
            case HELP -> { // a ring with a dot
                r.add(new int[]{-4, -7, 4, -5}); r.add(new int[]{-4, 5, 4, 7});
                r.add(new int[]{-7, -4, -5, 4}); r.add(new int[]{5, -4, 7, 4});
                r.add(new int[]{-6, -6, -4, -4}); r.add(new int[]{4, -6, 6, -4});
                r.add(new int[]{-6, 4, -4, 6}); r.add(new int[]{4, 4, 6, 6});
                r.add(new int[]{-2, -2, 2, 2});
            }
            case DEFEND -> { // a square
                r.add(new int[]{-6, -6, 6, -4}); r.add(new int[]{-6, 4, 6, 6});
                r.add(new int[]{-6, -4, -4, 4}); r.add(new int[]{4, -4, 6, 4});
            }
            case ATTACK -> { // crossed blades
                for (int i = -7; i <= 5; i += 2) {
                    r.add(new int[]{i, i, i + 2, i + 2});
                    r.add(new int[]{i, -i - 2, i + 2, -i});
                }
            }
            case ENEMY -> { // a diamond outline
                for (int x = -6; x <= 6; x += 2) {
                    int y = 6 - Math.abs(x);
                    r.add(new int[]{x - 1, y - 1, x + 1, y + 1});
                    r.add(new int[]{x - 1, -y - 1, x + 1, -y + 1});
                }
            }
        }
        return r;
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, int top, int bottom) {
        vc.vertex(m, x0, y0, 0).color(top).endVertex();
        vc.vertex(m, x0, y1, 0).color(bottom).endVertex();
        vc.vertex(m, x1, y1, 0).color(bottom).endVertex();
        vc.vertex(m, x1, y0, 0).color(top).endVertex();
    }
}
