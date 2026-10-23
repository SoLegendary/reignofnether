package com.solegendary.reignofnether.hud;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.hud.panels.ButtonCell;
import com.solegendary.reignofnether.hud.panels.CommandCard;
import com.solegendary.reignofnether.chat.ChatLines;
import com.solegendary.reignofnether.chat.ChatPanel;
import com.solegendary.reignofnether.hud.panels.HudFrame;
import com.solegendary.reignofnether.hud.panels.MinimapTools;
import com.solegendary.reignofnether.hud.panels.SelectionPanel;
import com.solegendary.reignofnether.hud.panels.SpectatorHud;
import com.solegendary.reignofnether.chat.SpectatorFeed;
import com.solegendary.reignofnether.hud.panels.TopLeftPanel;
import com.solegendary.reignofnether.hud.panels.TopRightPanel;
import com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents;
import com.solegendary.reignofnether.items.ItemClientEvents;
import com.solegendary.reignofnether.items.ItemShopMenu;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.scenario.ScenarioClientEvents;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.Text;
import com.solegendary.reignofnether.hud.buttons.HelperButtons;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Toast;
import com.solegendary.reignofnether.ui.Tooltip;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.*;
import com.solegendary.reignofnether.unit.units.piglins.HeadhunterUnit;
import com.solegendary.reignofnether.unit.units.piglins.HoglinUnit;
import com.solegendary.reignofnether.unit.units.villagers.MilitiaUnit;
import com.solegendary.reignofnether.unit.units.villagers.PillagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.solegendary.reignofnether.unit.UnitClientEvents.getSelectedUnits;

/**
 * The RTS HUD. Each frame it lays out the screen ({@link HudLayout}), draws the panels in order and
 * keeps what it drew in a {@link HudFrame}, which then answers clicks, hotkeys and "is the mouse over
 * the HUD" for the rest of the mod.
 */
public class HudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean enabled = true;

    public static final ArrayList<ControlGroup> controlGroups = new ArrayList<>(10);

    // unit that is selected in the list of unit icons
    public static LivingEntity hudSelectedEntity = null;
    // building that is selected in the list of unit icons
    public static BuildingPlacement hudSelectedPlacement = null;
    // 3D model of the selected unit for its portrait
    public static PortraitRendererUnit portraitRendererUnit = new PortraitRendererUnit();

    // mouse position in GUI coordinates (other systems use these)
    public static int mouseX = 0;
    public static int mouseY = 0;
    private static int leftDownHudX = 0;
    private static int leftDownHudY = 0;

    // what the HUD drew last frame, used to answer input between frames
    @Nullable
    private static HudFrame lastFrame = null;

    private static boolean showPreselectedBlockInfo = true;

    public static void setLowestCdHudEntity() {
        if (UnitClientEvents.getSelectedUnits().isEmpty() || hudSelectedEntity == null) {
            return;
        }

        List<Pair<LivingEntity, Float>> pairs = new ArrayList<>();
        for (LivingEntity livingEntity : getSelectedUnits()) {
            float totalCd = 0;
            if (livingEntity instanceof Unit unit) {
                for (Ability ability : unit.getAbilities().get()) {
                    totalCd += ability.getCooldown(unit);
                    if (ability.isCasting(unit))
                        totalCd += 10;
                }
            }
            Pair<LivingEntity, Float> apply = new Pair<>(livingEntity, totalCd);
            String str1 = getModifiedEntityName(apply.getFirst());
            String str2 = getModifiedEntityName(hudSelectedEntity);
            if (str1.equals(str2)) {
                pairs.add(apply);
            }
        }
        pairs.sort(Comparator.comparing(Pair::getSecond));

        if (!pairs.isEmpty())
            setHudSelectedEntity(pairs.get(0).getFirst());
    }

    public static void setHudSelectedEntity(LivingEntity entity) {
        if (entity != hudSelectedEntity) {
            CursorClientEvents.setLeftClickAction(null);
            ItemClientEvents.resetActions();
        }
        hudSelectedEntity = entity;

        // if in range of an itemshop, switch that shop to serve this unit
        if (ItemClientEvents.ENABLED &&
                ItemClientEvents.openItemShop != null &&
                !ItemClientEvents.openItemShop.isDestroyedServerside &&
                hudSelectedEntity instanceof HeroUnit heroUnit)
            for (BuildingPlacement bpl : BuildingClientEvents.getBuildings())
                if (bpl instanceof ItemShopPlacement shopBpl)
                    if (shopBpl.setServedUnit(heroUnit))
                        break;
    }

    // not to be used for resource paths
    public static String getModifiedEntityName(LivingEntity entity) {
        if (entity == null)
            return "";

        String name = MiscUtil.getSimpleEntityName(entity);

        if (entity.isBaby())
            name = I18n.get("entity.reignofnether.reignofnether.baby") + " " + name;

        if (!(entity instanceof Unit))
            return name.toLowerCase();

        if (entity instanceof MilitiaUnit militiaUnit && militiaUnit.isUsingBow()) {
            name = I18n.get("entity.reignofnether.militia_archer_unit");
        }
        ItemStack itemStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (itemStack.getItem() instanceof BannerItem) {
            name += " " + I18n.get("entity.reignofnether.captain");
        }
        if (entity.getPassengers().size() == 1) {
            Entity passenger = entity.getPassengers().get(0);
            if (entity instanceof RavagerUnit && passenger instanceof PillagerUnit) {
                name = I18n.get("entity.reignofnether.ravager_artillery");
            } else if (entity instanceof PoisonSpiderUnit && (
                    passenger instanceof SkeletonUnit || passenger instanceof StrayUnit
            )) {
                name = I18n.get("entity.reignofnether.poison_spider_jockey");
            } else if (entity instanceof SpiderUnit && (
                passenger instanceof SkeletonUnit || passenger instanceof StrayUnit
            )) {
                name = I18n.get("entity.reignofnether.spider_jockey");
            }else if (entity instanceof HoglinUnit && passenger instanceof HeadhunterUnit) {
                name = I18n.get("entity.reignofnether.hoglin_rider");
            } else {
                String pName = MiscUtil.getSimpleEntityName(entity.getPassengers().get(0)).replace("_", " ");
                String nameCap = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                name += " & " + nameCap;
            }
        }
        if (entity instanceof VillagerUnit vUnit) {
            switch (vUnit.getUnitProfession()) {
                case FARMER -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_farmer");
                    else
                        name = I18n.get("units.reignofnether.farmer");
                }
                case LUMBERJACK -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_lumberjack");
                    else
                        name = I18n.get("units.reignofnether.lumberjack");
                }
                case MINER -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_miner");
                    else
                        name = I18n.get("units.reignofnether.miner");
                }
                case MASON -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_mason");
                    else
                        name = I18n.get("units.reignofnether.mason");
                }
                case HUNTER -> {
                    if (vUnit.isVeteran())
                        name = I18n.get("units.reignofnether.veteran_hunter");
                    else
                        name = I18n.get("units.reignofnether.hunter");
                }
                default -> name = I18n.get("entity.reignofnether.villager_unit");
            }
        }
        if (entity instanceof CreeperUnit cUnit && cUnit.isPowered()) {
            name = I18n.get("entity.reignofnether.charged_creeper");
        }
        return name;
    }

    public static void showTempMessageI18n(String msgKey) {
        Toast.show(I18n.get(msgKey), Toast.DEFAULT_TICKS);
    }

    public static void showTempMessageI18n(String msgKey, int ticks) {
        Toast.show(I18n.get(msgKey), ticks);
    }

    public static void showTemporaryMessage(String msg) {
        Toast.show(msg, Toast.DEFAULT_TICKS);
    }

    public static void showTemporaryMessage(String msg, int ticks) {
        Toast.show(msg, ticks);
    }

    public static void removeFromControlGroups(int entityId) {
        for (ControlGroup controlGroup : controlGroups)
            controlGroup.entityIds.removeIf(id -> id == entityId);
    }

    @SubscribeEvent
    // can't use ScreenEvent.KeyboardKeyPressedEvent as that only happens when a screen is up
    public static void onInput(InputEvent.Key evt) {
        if (OrthoviewClientEvents.isEnabled() && evt.getAction() == GLFW.GLFW_PRESS) {
            if (evt.getKey() == Keybindings.getFnum(1).getKey())
                enabled = !enabled;
        }
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || !(evt.getScreen() instanceof TopdownGui) || MC.level == null) {
            lastFrame = null;
            return;
        }
        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();
        lastFrame = render(evt.getGuiGraphics(), mouseX, mouseY, true);
    }

    /** Draws the HUD under the chat panel, which replaces the RTS screen while open; not interactive. */
    public static void renderUnderChat(GuiGraphics g) {
        if (OrthoviewClientEvents.isEnabled() && MC.level != null)
            render(g, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, false);
    }

    private static HudFrame render(GuiGraphics g, int mouseX, int mouseY, boolean showChat) {
        HudScale scale = HudGeometry.scale();
        HudLayout layout = HudGeometry.layout();

        if (!enabled) {
            renderHiddenPill(g, scale, layout);
            return null;
        }
        updateSelectedPlacement();

        HudFrame f = new HudFrame(g, scale, layout, scale.toUnits(mouseX), scale.toUnits(mouseY));

        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        renderMapFrame(f, layout);
        g.pose().popPose();

        MinimapClientEvents.render(g);

        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        // the command card goes first so its keys win over anything else bound to the same key
        CommandCard.render(f);
        SelectionPanel.render(f);
        MinimapTools.render(f);
        if (SpectatorHud.isActive())
            SpectatorHud.render(f);
        else
            TopLeftPanel.render(f);
        TopRightPanel.render(f);
        PlayerDisplayClientEvents.render(f);
        renderItemShop(f);
        renderOverlays(f);
        if (showChat)
            renderChatButtonAndRecent(f);
        g.pose().popPose();

        renderStartPositions(f, mouseX, mouseY);

        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        f.showHoveredTooltip();
        Tooltip.renderPending(g, layout.width(), layout.height());
        TutorialClientEvents.checkAndRenderNextAction(g, placedButtons(f));
        g.pose().popPose();
        return f;
    }

    /** Chat button at the bottom-left, where chat shows; recent messages stack up from above it. */
    private static void renderChatButtonAndRecent(HudFrame f) {
        int size = 18;
        int x = HudLayout.MARGIN;
        int y = f.layout.console().y() - HudLayout.GAP * 2 - size;
        Button chat = HelperButtons.chatButton;
        if (!chat.isHidden.get()) {
            // the key is only in the tooltip
            Rect cell = new Rect(x, y, size, size);
            String key = MC.options.keyChat.getTranslatedKeyMessage().getString();
            ButtonCell.render(f.g, chat, x, y, size, null, f.hovered(cell));
            f.register(chat, cell, null, () -> ButtonCell.tooltipFor(chat, key));
            y -= HudLayout.GAP;
        }
        ChatLines.renderRecent(f.g, x, y, 300);
    }

    // assign hudSelectedPlacement like hudSelectedEntity in onRenderLiving
    private static void updateSelectedPlacement() {
        List<BuildingPlacement> selBuildings = BuildingClientEvents.getSelectedBuildings();
        if (selBuildings.isEmpty())
            hudSelectedPlacement = null;
        else if (hudSelectedPlacement == null || selBuildings.size() == 1 || !selBuildings.contains(hudSelectedPlacement))
            hudSelectedPlacement = selBuildings.get(0);
    }

    /** Diamond frame: the whole map turned 45 degrees like the camera, so no corner is cut off. */
    private static void renderMapFrame(HudFrame f, HudLayout l) {
        // one floating card holds the tools, their label and the diamond, like the other bottom cards
        Rect map = l.minimap();
        int pad = HudLayout.CONSOLE_PAD;
        int top = Math.min(map.y(), l.tools().y()) - pad;
        Rect card = new Rect(l.tools().x() - pad, top, map.right() + pad - (l.tools().x() - pad), l.height() - top);
        Theme.panel(f.g, card.x(), card.y(), card.w(), card.h(), Theme.FLOATING);
        f.zone(card);
        if (MinimapClientEvents.isSquare()) {
            Theme.outline(f.g, map.x() - 2, map.y() - 2, map.w() + 4, map.h() + 4, 1, Theme.INK);
            Theme.outline(f.g, map.x(), map.y(), map.w(), map.h(), 2, Theme.CONSOLE_RIM);
            f.g.fill(map.x(), map.y(), map.right(), map.bottom(), Theme.INK);
            return;
        }
        int side = Math.round(map.w() / (float) Math.sqrt(2));
        f.g.pose().pushPose();
        f.g.pose().translate(map.x() + map.w() / 2f, map.y() + map.h() / 2f, 0);
        f.g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45));
        Theme.outline(f.g, -side / 2 - 2, -side / 2 - 2, side + 4, side + 4, 1, Theme.INK);
        Theme.outline(f.g, -side / 2, -side / 2, side, side, 2, Theme.CONSOLE_RIM);
        f.g.fill(-side / 2, -side / 2, side / 2, side / 2, Theme.INK);
        f.g.pose().popPose();
    }


    private static void renderItemShop(HudFrame f) {
        ItemShopPlacement shop = ItemClientEvents.openItemShop;
        if (!ItemClientEvents.ENABLED || shop == null || shop.isDestroyedServerside)
            return;
        boolean shopSelected = hudSelectedPlacement == shop;
        boolean servedSelected = hudSelectedEntity != null && shop.getServedUnit() == hudSelectedEntity;
        if (shopSelected || servedSelected)
            ItemShopMenu.render(f, shop, f.layout.selection().x(), f.layout.console().y() - HudLayout.GAP);
    }

    /** Toast and the "what is the next click going to do" hint, centred above the console. */
    private static void renderOverlays(HudFrame f) {
        int cx = f.layout.width() / 2;
        int bottom = f.layout.toastBottom();
        int toastH = Toast.render(f.g, cx, bottom);
        String[] hint = targetingHint();
        if (hint == null)
            return;
        int y = bottom - toastH - (toastH > 0 ? HudLayout.GAP : 0);
        float titleScale = Text.px(17);
        int w = Math.max(Text.bigWidth(hint[0], titleScale), Text.tinyWidth(hint[1])) + 16;
        int h = 23;
        int x = cx - w / 2;
        Theme.outline(f.g, x, y - h, w, h, 1, Theme.INK);
        f.g.fill(x, y - h, x + w, y, Theme.HINT_BG);
        Theme.ring(f.g, x, y - h, w, h, 1, Theme.ORANGE);
        Text.bigCentered(f.g, hint[0], cx, y - h + 4, Theme.GOLD, titleScale);
        Text.tiny(f.g, hint[1], cx - Text.tinyWidth(hint[1]) / 2, y - h + 12, Theme.HINT);
    }

    @Nullable
    private static String[] targetingHint() {
        if (BuildingClientEvents.getBuildingToPlace() != null)
            return new String[] { I18n.get("hud.reignofnether.hint.place"), I18n.get("hud.reignofnether.hint.place_sub") };
        var action = CursorClientEvents.getLeftClickAction();
        if (action == null)
            return null;
        String key = "hud.reignofnether.hint." + action.name().toLowerCase(Locale.ROOT);
        String title = I18n.exists(key) ? I18n.get(key) : MiscUtil.capitaliseAndSpace(action.name().toLowerCase(Locale.ROOT));
        return new String[] { title.toUpperCase(Locale.ROOT), I18n.get("hud.reignofnether.hint.cancel") };
    }

    private static void renderHiddenPill(GuiGraphics g, HudScale scale, HudLayout layout) {
        g.pose().pushPose();
        g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
        String s = I18n.get("hud.reignofnether.hud_hidden", Keybindings.getFnum(1).getCurrentLabel());
        int w = Text.smallWidth(s) + 12;
        int x = (layout.width() - w) / 2;
        Theme.panel(g, x, HudLayout.MARGIN, w, 14);
        Text.small(g, s, x + 6, HudLayout.MARGIN + 2, Theme.LABEL);
        g.pose().popPose();
    }

    /** Pre-match seat markers on the minimap; drawn in GUI coordinates like the map itself. */
    private static void renderStartPositions(HudFrame f, int mouseX, int mouseY) {
        if (MC.player == null || !StartPosClientEvents.isEnabled() || StartPosClientEvents.isStarting
                || PlayerClientEvents.rtsLocked || !PlayerClientEvents.rtsPlayers.isEmpty())
            return;
        // only seats inside the map square, clipped to it
        float[] map = HudGeometry.toGui(f.layout.minimap());
        f.g.enableScissor(Math.round(map[0]), Math.round(map[1]), Math.round(map[0] + map[2]), Math.round(map[1] + map[3]));
        for (StartPos startPos : StartPosClientEvents.startPoses) {
            int xc = startPos.pos.getX();
            int zc = startPos.pos.getZ();
            if (!MinimapClientEvents.isWorldXZinsideMap(xc, zc))
                continue;
            Vec2 screenPos = MinimapClientEvents.worldPosToMinimapScreen(xc, zc);
            if (!MinimapClientEvents.isPointInsideMinimap(screenPos.x, screenPos.y))
                continue;
            Button button = startPos.getButton(MC.player.getName().getString(), MC.player.hasPermissions(4));
            int btnX = (int) screenPos.x - Button.DEFAULT_ICON_FRAME_SIZE / 2;
            int btnY = (int) screenPos.y - Button.DEFAULT_ICON_FRAME_SIZE / 2;
            button.render(f.g, btnX, btnY, mouseX, mouseY);
            HudScale s = f.scale;
            Rect r = new Rect(s.toUnits(btnX), s.toUnits(btnY),
                    s.toUnits(Button.DEFAULT_ICON_FRAME_SIZE), s.toUnits(Button.DEFAULT_ICON_FRAME_SIZE));
            f.register(button, r, null, () -> Tooltip.fromLines(button.tooltipLines));
        }
        f.g.disableScissor();
    }

    private static ArrayList<Button> placedButtons(HudFrame f) {
        ArrayList<Button> list = new ArrayList<>();
        for (HudFrame.Placed p : f.placed())
            list.add(p.button());
        return list;
    }

    // ------------------------------------------------------------------ queries from the rest of the mod

    private static int hudMouseX() {
        return HudGeometry.scale().toUnits(mouseX);
    }

    private static int hudMouseY() {
        return HudGeometry.scale().toUnits(mouseY);
    }

    public static boolean isMouseOverAnyButton() {
        return getMousedOverButton() != null;
    }

    @Nullable
    public static Button getMousedOverStartPosButton() {
        Button b = getMousedOverButton();
        return b != null && b.name.equals(StartPos.BUTTON_NAME) ? b : null;
    }

    @Nullable
    public static Button getMousedOverButton() {
        if (lastFrame == null)
            return null;
        HudFrame.Placed p = lastFrame.buttonAt(hudMouseX(), hudMouseY());
        return p != null ? p.button() : null;
    }

    public static boolean isMouseOverAnyButtonOrHud() {
        if (lastFrame != null && lastFrame.isOverHud(hudMouseX(), hudMouseY()))
            return true;
        if (MinimapClientEvents.isPointInsideMinimap(mouseX, mouseY))
            return true;
        if (CustomBuildingClientEvents.isMouseOverHud(mouseX, mouseY))
            return true;
        return ScenarioClientEvents.isMouseOverHud(mouseX, mouseY);
    }

    /** Left button released: lets the button under the mouse finish its action (item slots use this). */
    public static void releaseMouse() {
        if (lastFrame != null)
            lastFrame.release(hudMouseX(), hudMouseY());
    }

    // ------------------------------------------------------------------ input

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        if (!(MC.screen instanceof TopdownGui) || lastFrame == null)
            return;
        HudScale s = HudGeometry.scale();
        int ux = s.toUnits(evt.getMouseX());
        int uy = s.toUnits(evt.getMouseY());
        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            lastFrame.click(ux, uy, true);
            leftDownHudX = ux;
            leftDownHudY = uy;
        } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            lastFrame.click(ux, uy, false);
        }
    }

    // some vanilla-bound keys like Q and E never fire KeyPressed in this screen but do fire KeyReleased
    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.Post evt) {
        if (TextInputClientEvents.isAnyInputFocused())
            return;
        if (!(MC.screen instanceof TopdownGui))
            return;
        if (evt.getKeyCode() == GLFW.GLFW_KEY_F3)
            showPreselectedBlockInfo = !showPreselectedBlockInfo;
        if (lastFrame != null)
            lastFrame.press(evt.getKeyCode(), Keybindings.altMod.isDown());
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        Toast.tick();
        SpectatorHud.tick();
        SpectatorFeed.tick();
        if (OrthoviewClientEvents.isEnabled())
            portraitRendererUnit.tickAnimation();

        // holding the mouse on the portrait keeps the camera on the selected unit or building
        Rect portrait = SelectionPanel.portraitRect();
        if (MiscUtil.isLeftClickDown(MC) && MC.player != null && portrait.contains(hudMouseX(), hudMouseY())
                && portrait.contains(leftDownHudX, leftDownHudY)) {
            if (hudSelectedPlacement != null)
                OrthoviewClientEvents.centreCameraOnPos(hudSelectedPlacement.centrePos);
            else if (hudSelectedEntity != null)
                OrthoviewClientEvents.centreCameraOnPos(hudSelectedEntity.position());
        }
    }

    @SubscribeEvent
    // hudSelectedEntity and portraitRendererUnit should be assigned in the same event to avoid desyncs
    public static void onRenderLivingEntity(RenderLivingEvent.Post<? extends LivingEntity, ? extends Model> evt) {
        if (hudSelectedEntity != null && hudSelectedEntity.isRemoved())
            setHudSelectedEntity(null);

        ArrayList<LivingEntity> units = UnitClientEvents.getSortedSelectedUnits();

        if (units.isEmpty()) {
            HudClientEvents.setHudSelectedEntity(null);
        } else if (hudSelectedEntity == null || units.size() == 1 || !units.contains(hudSelectedEntity)) {
            HudClientEvents.setHudSelectedEntity(units.get(0));
        }

        if (hudSelectedEntity == null) {
            portraitRendererUnit.model = null;
            portraitRendererUnit.renderer = null;
        } else if (evt.getEntity() == hudSelectedEntity) {
            portraitRendererUnit.model = evt.getRenderer().getModel();
            portraitRendererUnit.renderer = evt.getRenderer();
        }
    }

    // control groups, deselect, chat and Tab
    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (TextInputClientEvents.isAnyInputFocused())
            return;
        if (!(MC.screen instanceof TopdownGui))
            return;

        // Prevent spectator mode options from showing up
        if (OrthoviewClientEvents.isEnabled()) {
            for (Keybinding numKey : Keybindings.nums)
                if (numKey.getKey() == evt.getKeyCode())
                    evt.setCanceled(true);
        }

        if (evt.getKeyCode() == Keybindings.deselect.getKey()) {
            UnitClientEvents.clearSelectedUnits();
            BuildingClientEvents.clearSelectedBuildings();
            BuildingClientEvents.setBuildingToPlace(null);
        }

        if (controlGroups.size() < Keybindings.nums.length) {
            controlGroups.clear();
            for (Keybinding ignored : Keybindings.nums)
                controlGroups.add(new ControlGroup());
        }

        for (Keybinding keybinding : Keybindings.nums) {
            int index = Integer.parseInt(keybinding.buttonLabel);
            if (index < 0 || index >= controlGroups.size() || evt.getKeyCode() != keybinding.getKey())
                continue;
            if (Keybindings.ctrlMod.isDown()) {
                controlGroups.get(index).saveFromSelected(keybinding, true);
            } else if (Keybindings.shiftMod.isDown()) {
                controlGroups.get(index).saveFromSelected(keybinding, false);
            } else if (Keybindings.altMod.isDown()) {
                for (ControlGroup controlGroup : controlGroups) {
                    for (LivingEntity le : getSelectedUnits())
                        controlGroup.entityIds.removeIf(id -> id == le.getId());
                    for (BuildingPlacement bpl : BuildingClientEvents.getSelectedBuildings())
                        controlGroup.buildingBps.removeIf(bp -> bp.equals(bpl.originPos));
                }
                controlGroups.get(index).saveFromSelected(keybinding, true);
            }
        }

        if (OrthoviewClientEvents.isEnabled() && evt.getKeyCode() == Keybindings.chat.getKey())
            MC.setScreen(new ChatPanel());

        if (evt.getKeyCode() == Keybindings.tab.getKey()) {
            cycleUnitSubgroups();
            cycleBuildingSubgroups();
        }
    }

    private static void cycleUnitSubgroups() {
        List<LivingEntity> selUnits = UnitClientEvents.getSortedSelectedUnits();
        var unitNameSet = new LinkedHashSet<String>();
        Set<String> uniqueValues = new HashSet<>();
        for (LivingEntity selUnit : selUnits) {
            String modifiedEntityName = getModifiedEntityName(selUnit);
            if (uniqueValues.add(modifiedEntityName)) {
                unitNameSet.add(modifiedEntityName);
            }
        }
        var unitNames = new LinkedList<>(unitNameSet);

        if (unitNames.size() <= 1 || hudSelectedEntity == null)
            return;

        boolean reversed = Keybindings.shiftMod.isDown();
        String selUnitName = getModifiedEntityName(hudSelectedEntity);
        if (reversed)
            Collections.reverse(unitNames);

        // find the next unit name
        String newUnitName = "";
        boolean foundSelected = false;
        for (String uname : unitNames) {
            if (foundSelected) {
                newUnitName = uname;
                break;
            } else if (uname.equals(selUnitName)) {
                foundSelected = true;
            }
        }
        if (newUnitName.isBlank() && !selUnits.isEmpty()) {
            if (reversed) {
                setHudSelectedEntity(selUnits.get(selUnits.size() - 1));
            } else {
                setHudSelectedEntity(selUnits.get(0));
            }
        } else {
            for (LivingEntity le : selUnits) {
                String bplName = getModifiedEntityName(le);
                if (bplName.equals(newUnitName)) {
                    setHudSelectedEntity(le);
                    break;
                }
            }
        }
    }

    private static void cycleBuildingSubgroups() {
        List<BuildingPlacement> selBuildings = BuildingClientEvents.getSelectedBuildings();
        List<String> buildingNames = selBuildings
                .stream().map(b -> {
                    if (b.getBuilding() instanceof CustomBuilding cb) {
                        return cb.name;
                    }
                    return ReignOfNetherRegistries.BUILDING.getKey(b.getBuilding()).toString();
                })
                .distinct().sorted(Comparator.comparing(b -> b))
                .collect(Collectors.toList());

        if (buildingNames.size() <= 1 || hudSelectedPlacement == null)
            return;

        boolean reversed = Keybindings.shiftMod.isDown();
        String selBuildingName = ReignOfNetherRegistries.BUILDING.getKey(hudSelectedPlacement.getBuilding()).toString();

        if (reversed)
            Collections.reverse(buildingNames);

        // find the next building name
        String newBuildingName = "";
        boolean foundSelected = false;
        for (String bname : buildingNames) {
            if (foundSelected) {
                newBuildingName = bname;
                break;
            } else if (bname.equals(selBuildingName)) {
                foundSelected = true;
            }
        }
        if (newBuildingName.isBlank() && !selBuildings.isEmpty()) {
            if (reversed) {
                hudSelectedPlacement = selBuildings.get(selBuildings.size() - 1);
            } else {
                hudSelectedPlacement = selBuildings.get(0);
            }
        } else {
            for (BuildingPlacement bpl : selBuildings) {
                String bplName = ReignOfNetherRegistries.BUILDING.getKey(bpl.getBuilding()).toString();
                if (bplName.equals(newBuildingName)) {
                    hudSelectedPlacement = bpl;
                    break;
                }
            }
        }
    }

    // newUnitIds are replacing oldUnitIds - replace them in every control group while retaining their index
    public static void convertControlGroups(int[] oldUnitIds, int[] newUnitIds) {
        if (MC.level == null) {
            return;
        }
        for (ControlGroup group : controlGroups) {
            for (int i = 0; i < oldUnitIds.length; i++) {
                for (int j = 0; j < group.entityIds.size(); j++) {
                    if (group.entityIds.get(j) == oldUnitIds[i]) {
                        group.entityIds.add(j, newUnitIds[i]);
                        break;
                    }
                }
            }
            for (int old : oldUnitIds)
                group.entityIds.removeIf(id -> id == old);
        }
    }


    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        // no vanilla player list in the RTS view: Tab cycles unit types there, and diplomacy lists the players
        if (evt.getOverlay() == VanillaGuiOverlay.PLAYER_LIST.type() && OrthoviewClientEvents.isEnabled()) {
            evt.setCanceled(true);
            return;
        }
        if (MC.screen != null && MC.level != null && SandboxClientEvents.isSandboxPlayer() && showPreselectedBlockInfo) {
            int y = MC.getWindow().getGuiScaledHeight() / 3;
            BlockPos bp = CursorClientEvents.getPreselectedBlockPos();
            evt.getGuiGraphics().drawString(MC.font, I18n.get("hud.reignofnether.block_pos", bp.toShortString()), 5, y, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, MC.level.getBlockState(bp).getBlock().toString().replaceFirst("Block", ""), 5, y + 10, 0xFFFFFF);
            evt.getGuiGraphics().drawString(MC.font, "Chunk: " + MC.level.getChunkAt(bp).getPos(), 5, y + 20, 0xFFFFFF);
        }
    }
}
