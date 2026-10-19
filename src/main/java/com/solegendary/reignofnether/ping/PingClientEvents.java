package com.solegendary.reignofnether.ping;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MapMarkerServerboundPacket;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

// Hold the ping key (Caps Lock by default) to open a wheel around the cursor, move towards an option and
// release. Releasing without picking cancels. Pings show for allies on the minimap, as a beam in
// the world and in team chat.
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
        if (!open)
            return;
        GuiGraphics g = evt.getGuiGraphics();
        PingType hovered = pick(mouseX() - cx, mouseY() - cy);

        for (int i = 0; i < WHEEL.length; i++) {
            String label = I18n.get(WHEEL[i].langKey());
            int w = MC.font.width(label);
            // up/down are centred above/below; left/right start a gap away from the centre so long labels
            // grow outwards instead of into each other
            int x = switch (i) {
                case 1 -> (int) cx + SIDE_GAP;
                case 3 -> (int) cx - SIDE_GAP - w;
                default -> (int) cx - w / 2;
            };
            int y = (int) cy - 4 + (i == 0 ? -WHEEL_R : i == 2 ? WHEEL_R : 0);
            g.fill(x - 4, y - 3, x + w + 4, y + 11, WHEEL[i] == hovered ? 0xC0000000 | WHEEL[i].rgb : 0xA0000000);
            g.drawString(MC.font, label, x, y, 0xFFFFFF, true);
        }
        g.fill((int) cx - 1, (int) cy - 1, (int) cx + 2, (int) cy + 2, 0xFFFFFFFF);
    }

    // a pulsing beam and floor tile in the ping type's colour
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || MinimapClientEvents.mapMarkers.isEmpty())
            return;
        var buffers = MC.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(MyRenderer.LINES_NO_DEPTH_TEST);
        var faces = buffers.getBuffer(RenderType.entityTranslucent(ResourceLocation.parse("forge:textures/white.png")));
        for (MinimapClientEvents.MapMarker m : MinimapClientEvents.mapMarkers) {
            int rgb = m.type.rgb;
            float r = ((rgb >> 16) & 0xFF) / 255f, gr = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
            float a = 0.5f + 0.5f * (float) Math.abs(Math.sin(m.ageTicks / 6.0));
            Vec3 base = new Vec3(m.x + 0.5, m.y + 1, m.z + 0.5);
            MyRenderer.drawLine(evt.getPoseStack(), lines, base, base.add(0, 14, 0), r, gr, b, a);
            MyRenderer.drawBlockFace(evt.getPoseStack(), faces, Direction.UP, new BlockPos(m.x, m.y, m.z), r, gr, b, a * 0.6f);
        }
        buffers.endBatch();
    }
}
