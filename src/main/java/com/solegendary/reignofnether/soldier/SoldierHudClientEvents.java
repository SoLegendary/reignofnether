package com.solegendary.reignofnether.soldier;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.chat.ChatLines;
import com.solegendary.reignofnether.chat.ChatPanel;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.ping.PingClientEvents;
import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.util.MyRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector2f;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Set;

// The first-person soldier's HUD, after the "RoN FPV HUD" prototype (1920x1080 design, drawn here in HUD
// units at half that): minimap with shared vision, the commander's current order, a team bar, what the
// crosshair is on, the respawn screen, and the hotbar with hearts and hunger in place of vanilla's.
public class SoldierHudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final int BORDER = 0xFF2C2A33;
    private static final int MAP = 140;
    private static final int FULL_MAP = 380;

    private static boolean fullMap = false;
    private static boolean mapKeyWasDown = false;

    public static boolean active() {
        return MC.player != null && PlayerRolesClient.myRole() == PlayerRole.SOLDIER && !MC.options.hideGui;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        boolean down = Keybindings.soldierMap.isDown() && MC.screen == null;
        if (down && !mapKeyWasDown)
            fullMap = !fullMap;
        mapKeyWasDown = down;
        if (!active() || !SoldierClient.dead)
            fullMap = false;
    }

    // vanilla parts the prototype replaces (our crosshair, hotbar, hearts and hunger) or drops (armour: a fixed kit, xp)
    private static final Set<ResourceLocation> REPLACED = Set.of(VanillaGuiOverlay.CROSSHAIR.id(), VanillaGuiOverlay.HOTBAR.id(), VanillaGuiOverlay.PLAYER_HEALTH.id(),
            VanillaGuiOverlay.FOOD_LEVEL.id(), VanillaGuiOverlay.ARMOR_LEVEL.id(), VanillaGuiOverlay.EXPERIENCE_BAR.id());

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre evt) {
        if (!active())
            return;
        if (REPLACED.contains(evt.getOverlay().id()))
            evt.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post evt) {
        if (!active())
            return;
        GuiGraphics g = evt.getGuiGraphics();
        HudScale scale = HudGeometry.scale();
        int w = scale.width();
        int h = scale.height();
        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);

        String commander = PlayerRolesClient.myCommander();
        if (SoldierClient.dead) {
            g.fill(0, 0, w, h, 0x99140508);
            if (fullMap)
                renderFullMap(g, w, h);
            else
                renderRespawn(g, w, h);
        } else {
            renderMinimap(g, w - 10 - MAP, 10);
            renderOrder(g, w - 10 - MAP, 10 + MAP + 8);
            renderCrosshair(g, w, h);
            renderLookTarget(g, w, h);
            renderHotbar(g, w, h);
        }
        renderTeamBar(g, w, commander);
        if (!(MC.screen instanceof ChatPanel))
            ChatLines.renderRecent(g, 10, chatBottom(h), 300);
        g.pose().popPose();

    }

    // ---------------------------------------------------------------- minimap

    private static void renderMinimap(GuiGraphics g, int x, int y) {
        Theme.outline(g, x, y, MAP, MAP, 3, BORDER);
        Theme.outline(g, x - 3, y - 3, MAP + 6, MAP + 6, 1, Theme.INK);
        g.fill(x, y, x + MAP, y + MAP, Theme.INK);
        MinimapClientEvents.renderMapInto(g, x, y, MAP, MAP);
        renderMapMarks(g, x, y, MAP, 1);
        String tag = I18n.get("soldier.reignofnether.hud.shared_vision");
        g.fill(x + 3, y + MAP - 13, x + 9 + Text.smallWidth(tag), y + MAP - 3, 0xD90B0A0F);
        Text.small(g, tag, x + 6, y + MAP - 12, Theme.MUTED);
    }

    // pings, the current order and you, on a map drawn in the square (x, y, size)
    private static void renderMapMarks(GuiGraphics g, int x, int y, int size, int k) {
        for (MinimapClientEvents.MapMarker m : MinimapClientEvents.mapMarkers) {
            Vector2f p = MinimapClientEvents.worldToRect(m.x, m.z, x, y, size, size);
            float t = (m.ageTicks % 20) / 20f;
            int s = Math.round(k * 4 * (0.4f + 1.8f * t));
            Theme.outline(g, Math.round(p.x) - s, Math.round(p.y) - s, s * 2, s * 2, 1, Theme.withAlpha(0xFF000000 | m.type.rgb, 1 - t));
        }
        if (SoldierClient.order != null) {
            Vector2f p = MinimapClientEvents.worldToRect(SoldierClient.orderPos.getX(), SoldierClient.orderPos.getZ(), x, y, size, size);
            PingClientEvents.diamond(g, p.x, p.y, 5 * k, Theme.INK);
            PingClientEvents.diamond(g, p.x, p.y, 4 * k, 0xFF000000 | SoldierClient.order.rgb);
        }
        if (MC.player != null) {
            Vector2f p = MinimapClientEvents.worldToRect(MC.player.getBlockX(), MC.player.getBlockZ(), x, y, size, size);
            PingClientEvents.diamond(g, p.x, p.y, 4 * k, Theme.INK);
            PingClientEvents.diamond(g, p.x, p.y, 3 * k, Theme.GOLD);
        }
    }

    private static void renderFullMap(GuiGraphics g, int w, int h) {
        int x = (w - FULL_MAP) / 2;
        int y = 30;
        Theme.outline(g, x, y, FULL_MAP, FULL_MAP, 3, BORDER);
        g.fill(x, y, x + FULL_MAP, y + FULL_MAP, Theme.INK);
        MinimapClientEvents.renderMapInto(g, x, y, FULL_MAP, FULL_MAP);
        renderMapMarks(g, x, y, FULL_MAP, 2);
        String tag = I18n.get("soldier.reignofnether.hud.full_map_close", Math.round(Math.ceil(SoldierClient.respawnSecondsLeft())));
        g.fill(x + 4, y + FULL_MAP - 15, x + 12 + Text.smallWidth(tag), y + FULL_MAP - 4, 0xD90B0A0F);
        Text.small(g, tag, x + 8, y + FULL_MAP - 13, Theme.MUTED);
    }

    // ---------------------------------------------------------------- order

    private static void renderOrder(GuiGraphics g, int x, int y) {
        PingType order = SoldierClient.order;
        if (order == null || MC.player == null)
            return;
        int colour = 0xFF000000 | order.rgb;
        int ph = 40;
        Theme.panel(g, x, y, MAP, ph);
        boolean fresh = SoldierClient.orderAge() < 3;
        g.fill(x, y, x + MAP, y + Theme.BEVEL, fresh && (System.currentTimeMillis() / 250) % 2 == 0 ? Theme.GOLD : Theme.ORANGE);
        String from = I18n.get("soldier.reignofnether.hud.from_commander", PlayerRolesClient.myCommander());
        Text.small(g, Text.fitSmall(from, MAP - 40), x + 7, y + 6, Theme.LABEL);
        String age = fresh ? I18n.get("soldier.reignofnether.hud.new") : I18n.get("soldier.reignofnether.hud.ago", SoldierClient.orderAge());
        Text.smallRight(g, age, x + MAP - 7, y + 6, Theme.DIM);

        int ty = y + 18;
        g.fill(x + 7, ty, x + 21, ty + 14, colour);
        Theme.outline(g, x + 7, ty, 14, 14, 1, Theme.INK);
        g.drawCenteredString(MC.font, order.symbol, x + 14, ty + 3, 0xFF380000);
        String name = I18n.get("ping.reignofnether." + order.name().toLowerCase(Locale.ROOT));
        Text.big(g, name, x + 26, ty + 3, Theme.TEXT, Text.px(18));
        String dist = Math.round(Math.sqrt(MC.player.blockPosition().distSqr(SoldierClient.orderPos))) + " m";
        Text.bigRight(g, dist, x + MAP - 7, ty + 3, colour, Text.px(18));
    }

    // the order's waypoint over its spot, like a ping that doesn't fade
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS && active() && !SoldierClient.dead && SoldierClient.order != null)
            PingClientEvents.drawWaypointInWorld(evt.getPoseStack(), Vec3.atBottomCenterOf(SoldierClient.orderPos).add(0, 1, 0),
                    SoldierClient.order, System.currentTimeMillis() / 50f);
    }

    // soldiers chat in the RTS chat panel too; the command key still opens vanilla's for commands
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening evt) {
        if (active() && evt.getNewScreen() instanceof ChatScreen && !MC.options.keyCommand.isDown())
            evt.setNewScreen(new ChatPanel());
    }

    /** Bottom of the chat (panel and recent lines): above the hearts row. */
    public static int chatBottom(int h) {
        return h - 10 - 32 - 4 - 11 - 6;
    }

    // ---------------------------------------------------------------- hotbar

    private static final ResourceLocation HEART = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/heart.png");
    private static final ResourceLocation WHEAT = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");

    // 9 slots of 28 with 2 between and 2 padding, 10 above the bottom; hearts left and hunger right above it.
    // Shapes are drawn in design pixels (half a unit) where the prototype has odd sizes.
    private static void renderHotbar(GuiGraphics g, int w, int h) {
        Player p = MC.player;
        int barW = 2 + 9 * 28 + 8 * 2 + 2;
        int x = (w - barW) / 2, y = h - 10 - 32;
        g.pose().pushPose();
        g.pose().scale(0.5f, 0.5f, 1);
        g.fill(x * 2 - 2, y * 2 - 2, (x + barW) * 2 + 2, (y + 32) * 2 + 2, BORDER);
        g.fill(x * 2, y * 2, (x + barW) * 2, (y + 32) * 2, 0xD90B0A0F);
        for (int i = 0; i < 9; i++) {
            int sx = (x + 2 + i * 30) * 2, sy = (y + 2) * 2;
            if (i == p.getInventory().selected) {
                for (int glow = 3; glow >= 1; glow--)
                    g.fill(sx - glow * 4, sy - glow * 4, sx + 56 + glow * 4, sy + 56 + glow * 4, (0x2A >> (glow - 1)) << 24 | (Theme.ORANGE & 0xFFFFFF));
                g.fill(sx, sy, sx + 56, sy + 56, Theme.ACTIVE);
                Theme.outline(g, sx, sy, 56, 56, 3, Theme.ORANGE);
            } else {
                g.fill(sx, sy, sx + 56, sy + 56, Theme.RAISED);
                g.fill(sx, sy, sx + 56, sy + 3, 0x0FFFFFFF);
                g.fill(sx, sy + 53, sx + 56, sy + 56, 0x80000000);
            }
        }
        g.pose().popPose();

        for (int i = 0; i < 9; i++) {
            int sx = x + 2 + i * 30, sy = y + 2;
            var stack = p.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                g.renderItem(stack, sx + 6, sy + 6);
                g.renderItemDecorations(MC.font, stack, sx + 6, sy + 6, ""); // durability and cooldown, not the count
                if (stack.getCount() > 1)
                    Text.bigRight(g, String.valueOf(stack.getCount()), sx + 26, sy + 28 - 1 - Text.bigHeight(Text.px(14)), 0xFFFFFFFF, Text.px(14));
            }
            Text.small(g, String.valueOf(i + 1), sx + 2, sy + 1, Theme.DIM);
        }

        // 10 icons of 11 with 1 between, 4 above the bar; an icon is full from 1 point of its 2
        int iy = y - 4 - 11;
        int hearts = (int) Math.ceil(p.getHealth() / 2), food = (int) Math.ceil(p.getFoodData().getFoodLevel() / 2f);
        for (int i = 0; i < 10; i++) {
            icon(g, HEART, x + 1 + i * 12, iy, i < hearts);
            icon(g, WHEAT, x + barW - 1 - 11 - (9 - i) * 12, iy, i < food);
        }
    }

    // ponytail: an empty icon is the full one darkened, not greyscale like the prototype; a grey texture if it matters
    private static void icon(GuiGraphics g, ResourceLocation texture, int x, int y, boolean full) {
        if (!full)
            RenderSystem.setShaderColor(0.3f, 0.3f, 0.3f, 1);
        g.blit(texture, x, y, 11, 11, 0, 0, 32, 32, 32, 32);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ---------------------------------------------------------------- team bar

    private static void renderTeamBar(GuiGraphics g, int w, String commander) {
        int colour = 0xFF000000 | PlayerColors.getPlayerDisplayColorHex(commander);
        String role = I18n.get("soldier.reignofnether.hud.commander", MiscUtil.getColorName(PlayerColors.getPlayerDisplayColorHex(commander), false))
                .toUpperCase(Locale.ROOT);
        Resources res = ResourcesClientEvents.getResources(commander);
        String[] values = res == null ? new String[] { "-", "-", "-" }
                : new String[] { String.valueOf(res.food), String.valueOf(res.wood), String.valueOf(res.ore) };
        String pop = UnitClientEvents.getCurrentPopulation(commander) + "/" + BuildingClientEvents.getTotalPopulationSupply(commander);
        long seconds = PlayerClientEvents.rtsGameTicks / 20;
        String clock = (seconds / 60) + ":" + String.format("%02d", seconds % 60);
        boolean night = MC.level != null && MC.level.isNight();

        float big = Text.px(16);
        int commanderW = 3 + 6 + 14 + 6 + Math.max(Text.smallWidth(role), Text.width(commander)) + 8;
        int stockW = 6;
        for (String v : values)
            stockW += 9 + 3 + Text.bigWidth(v, big) + 8;
        int popW = 6 + 9 + 3 + Text.bigWidth(pop, big) + 8;
        String dayNight = I18n.get(night ? "soldier.reignofnether.hud.night" : "soldier.reignofnether.hud.day");
        int clockW = 6 + 9 + 3 + Math.max(Text.smallWidth(dayNight), Text.bigWidth(clock, big)) + 8;
        int bw = commanderW + 1 + stockW + 1 + popW + 1 + clockW;
        int x = (w - bw) / 2;
        int y = 8;
        int bh = 22;
        Theme.panel(g, x, y, bw, bh);

        g.fill(x, y, x + 3, y + bh, colour);
        MyRenderer.renderPlayerHead(g, commander, x + 9, y + 4, 14, CHESTPLATE_ICON);
        Text.small(g, role, x + 29, y + 3, colour);
        Text.draw(g, Text.fit(commander, commanderW - 30), x + 29, y + 12, Theme.TEXT);
        x += commanderW;
        g.fill(x, y, x + 1, y + bh, Theme.INK);
        x += 1;

        int sx = x + 6;
        for (int i = 0; i < values.length; i++) {
            MyRenderer.renderIcon(g, STOCK_ICONS[i], sx, y + 6, 9);
            Text.big(g, values[i], sx + 12, y + 7, Theme.BODY, big);
            sx += 9 + 3 + Text.bigWidth(values[i], big) + 8;
        }
        x += stockW;
        g.fill(x, y, x + 1, y + bh, Theme.INK);
        x += 1;

        MyRenderer.renderIcon(g, BED_ICON, x + 6, y + 6, 9);
        Text.big(g, pop, x + 18, y + 7, Theme.BODY, big);
        x += popW;
        g.fill(x, y, x + 1, y + bh, Theme.INK);
        x += 1;

        MyRenderer.renderIcon(g, CLOCK_ICON, x + 6, y + 6, 9);
        Text.small(g, dayNight, x + 18, y + 3, Theme.GOLD);
        Text.big(g, clock, x + 18, y + 11, Theme.BODY, Text.px(14));
    }

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/" + name + ".png");
    }

    private static final ResourceLocation[] STOCK_ICONS = { icon("wheat"), icon("wood"), icon("iron_ore") };
    private static final ResourceLocation CHESTPLATE_ICON = icon("chestplate");
    private static final ResourceLocation BED_ICON = icon("bed");
    private static final ResourceLocation CLOCK_ICON = icon("clock");

    // ---------------------------------------------------------------- look target

    private record Target(String name, String owner, float hp, float maxHp) { }

    // what the crosshair is on; only things the server sent, so nothing hidden by fog
    @Nullable
    private static Target lookTarget() {
        HitResult hit = MC.hitResult;
        if (hit instanceof EntityHitResult eh && eh.getEntity() instanceof LivingEntity le) {
            String owner = le instanceof Unit unit ? unit.getOwnerName()
                    : le instanceof Player p ? PlayerRolesClient.commanderOf(p.getName().getString()) : "";
            return new Target(le.getDisplayName().getString(), owner, le.getHealth(), le.getMaxHealth());
        }
        if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BuildingPlacement b = BuildingUtils.findBuilding(true, bh.getBlockPos());
            if (b != null)
                return new Target(b.getBuilding().name, b.ownerName, b.getHealth(), b.getMaxHealth());
        }
        return null;
    }

    // a 20x2 design-pixel cross with a half-black outline, white or in the colour of what it's on
    private static void renderCrosshair(GuiGraphics g, int w, int h) {
        Target t = lookTarget();
        int colour = t == null ? 0xFFFFFFFF : t.owner.isBlank() ? Theme.MUTED : 0xFF000000 | PlayerColors.getPlayerDisplayColorHex(t.owner);
        g.pose().pushPose();
        g.pose().scale(0.5f, 0.5f, 1);
        int cx = w, cy = h; // the centre, in design pixels
        g.fill(cx - 11, cy - 2, cx + 11, cy + 2, 0x80000000);
        g.fill(cx - 2, cy - 11, cx - 2 + 4, cy - 2, 0x80000000);
        g.fill(cx - 2, cy + 2, cx + 2, cy + 11, 0x80000000);
        g.fill(cx - 10, cy - 1, cx + 10, cy + 1, colour);
        g.fill(cx - 1, cy - 10, cx + 1, cy + 10, colour);
        g.pose().popPose();
    }

    private static void renderLookTarget(GuiGraphics g, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;
        Target t = lookTarget();
        if (t == null)
            return;
        int colour = t.owner.isBlank() ? Theme.MUTED : 0xFF000000 | PlayerColors.getPlayerDisplayColorHex(t.owner);
        int pw = 115;
        int px = cx - pw / 2;
        int py = cy + 18;
        int ph = 28;
        Theme.panel(g, px, py, pw, ph);
        g.fill(px, py, px + pw, py + Theme.BEVEL, colour);
        g.fill(px + 6, py + 6, px + 11, py + 11, colour);
        String owner = t.owner.isBlank() ? I18n.get("soldier.reignofnether.hud.neutral") : t.owner;
        int ownerW = Text.smallWidth(owner);
        Text.draw(g, Text.fit(t.name, pw - 22 - ownerW), px + 14, py + 5, Theme.TEXT);
        Text.smallRight(g, owner, px + pw - 6, py + 5, colour);

        float frac = t.maxHp <= 0 ? 0 : Math.max(0, Math.min(1, t.hp / t.maxHp));
        int bx = px + 6, by = py + 16, bwid = pw - 12;
        g.fill(bx, by, bx + bwid, by + 7, Theme.INK);
        g.fill(bx, by, bx + Math.round(bwid * frac), by + 7, Theme.hpColor(frac));
        String hp = Math.round(t.hp) + " / " + Math.round(t.maxHp);
        Text.smallCentered(g, hp, bx + bwid / 2, by, Theme.TEXT);
    }

    // ---------------------------------------------------------------- dead

    private static void renderRespawn(GuiGraphics g, int w, int h) {
        int cx = w / 2;
        int y = h / 2 - 70;
        String slain = SoldierClient.killer.isBlank() ? I18n.get("soldier.reignofnether.hud.slain")
                : I18n.get("soldier.reignofnether.hud.slain_by", SoldierClient.killer);
        Text.smallCentered(g, slain.toUpperCase(Locale.ROOT), cx, y, Theme.ERROR);

        float left = SoldierClient.respawnSecondsLeft();
        Text.bigCentered(g, String.valueOf((int) Math.ceil(left)), cx, y + 14, Theme.TEXT, Text.px(96));

        String at = SoldierClient.respawnBuilding.isBlank() ? I18n.get("soldier.reignofnether.hud.no_building")
                : I18n.get("soldier.reignofnether.hud.respawning_at", SoldierClient.respawnBuilding);
        Text.bigCentered(g, at.toUpperCase(Locale.ROOT), cx, y + 70, Theme.GOLD, Text.px(22));

        int bw = 260;
        int bx = cx - bw / 2;
        int by = y + 90;
        float done = 1 - left / Math.max(1, SoldierClient.respawnTotalSeconds);
        g.fill(bx, by, bx + bw, by + 5, 0xFF14131A);
        g.fill(bx, by, bx + Math.round(bw * done), by + 5, Theme.ORANGE);
        g.fill(bx, by, bx + Math.round(bw * done), by + 1, Theme.GOLD);

        int kx = cx - 40;
        int ky = by + 14;
        int kw = Theme.keyCap(g, Keybindings.soldierMap.getCurrentLabel(), kx, ky, Theme.GOLD);
        Text.draw(g, I18n.get("soldier.reignofnether.hud.full_map"), kx + kw + 6, ky + 3, Theme.TEXT);
    }
}
