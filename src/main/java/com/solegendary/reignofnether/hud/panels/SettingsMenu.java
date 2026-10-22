package com.solegendary.reignofnether.hud.panels;

import com.solegendary.reignofnether.blocks.BlockClientEvents;
import com.solegendary.reignofnether.config.ReignOfNetherClientConfigs;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.layout.HudLayout;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.ui.HudScale;
import com.solegendary.reignofnether.ui.Icon;
import com.solegendary.reignofnether.ui.Rect;
import com.solegendary.reignofnether.ui.SettingRow;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.ui.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * RTS settings (camera and map display options) and the game rules, opened from icon buttons beside
 * the pause menu. Each opens its own screen; Esc goes back to the pause menu.
 */
public final class SettingsMenu {

    private SettingsMenu() { }


    /** Same rule as the old HUD button: sandbox players and spectators, outside the tutorial and scenarios. */
    public static boolean canOpenGamerules() {
        return (SandboxClientEvents.isSandboxPlayer() || !PlayerClientEvents.isRTSPlayer())
                && !TutorialClientEvents.isEnabled() && !GameruleClient.scenarioMode && !StartPosClientEvents.isStarting;
    }

    /** Two square icon buttons left of the pause menu's second row, like mods add on the right. */
    @SubscribeEvent
    public static void onPauseInit(ScreenEvent.Init.Post evt) {
        if (!(evt.getScreen() instanceof PauseScreen pause) || !OrthoviewClientEvents.isEnabled())
            return;
        AbstractWidget back = null;
        for (var l : evt.getListenersList())
            if (l instanceof AbstractWidget w && w.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                    && t.getKey().equals("menu.returnToGame"))
                back = w;
        if (back == null)
            return;
        int x = back.getX() - 24;
        int y = back.getY() + 24;
        evt.addListener(new IconButton(x, y, Icon.SETTINGS, Component.translatable("hud.reignofnether.settings"),
                () -> Minecraft.getInstance().setScreen(settingsScreen(pause))));
        if (canOpenGamerules())
            evt.addListener(new IconButton(x, y + 24, Icon.LIST, Component.translatable("hud.gamerule.reignofnether.title"),
                    () -> Minecraft.getInstance().setScreen(new Panel(pause, "hud.gamerule.reignofnether.title", GameruleClient::ruleRows, 190))));
    }

    public static Screen settingsScreen(Screen parent) {
        return new Panel(parent, "hud.reignofnether.settings", SettingsMenu::rows, 210);
    }

    private static class IconButton extends AbstractButton {
        private final Icon icon;
        private final Runnable action;

        IconButton(int x, int y, Icon icon, Component name, Runnable action) {
            super(x, y, 20, 20, name);
            this.icon = icon;
            this.action = action;
            setTooltip(net.minecraft.client.gui.components.Tooltip.create(name));
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            Theme.secondaryButton(g, getX(), getY(), width, height, isHoveredOrFocused());
            icon.drawIn(g, getX(), getY(), width, height, isHoveredOrFocused() ? Theme.GOLD : Theme.BODY);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }

    /** One list of setting rows, centred, drawn in HUD units like the HUD popovers. */
    private static class Panel extends Screen {
        private final Screen parent;
        private final String titleKey;
        private final Supplier<List<SettingRow>> rows;
        private final int colW;
        private HudFrame frame;

        private List<SettingRow> list;

        Panel(Screen parent, String titleKey, Supplier<List<SettingRow>> rows, int colW) {
            super(Component.translatable(titleKey));
            this.parent = parent;
            this.titleKey = titleKey;
            this.rows = rows;
            this.colW = colW;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderBackground(g);
            HudScale scale = HudGeometry.scale();
            HudLayout layout = HudGeometry.layout();
            frame = new HudFrame(g, scale, layout, scale.toUnits(mouseX), scale.toUnits(mouseY));
            if (list == null)
                list = rows.get(); // every value is a supplier, so the rows themselves never change
            int margin = HudLayout.MARGIN * 2;
            Rect size = SettingRows.measure(list, colW, layout.height() - margin * 2 - 30);
            int w = size.w() + 14;
            int h = size.h() + 30;
            int top = Math.max(margin, (layout.height() - h) / 2);

            g.pose().pushPose();
            g.pose().scale(scale.poseScale(), scale.poseScale(), 1);
            Rect panel = SettingRows.renderPopover(frame, I18n.get(titleKey).toUpperCase(Locale.ROOT), list,
                    (layout.width() + w) / 2, top, layout.height() - margin, colW);
            Rect x = new Rect(panel.right() - 18, panel.y() + 6, 12, 12);
            Theme.secondaryButton(g, x.x(), x.y(), x.w(), x.h(), frame.hovered(x));
            Theme.cross(g, x.x() + 3, x.y() + 3, 6, frame.hovered(x) ? Theme.GOLD : Theme.BODY);
            frame.register(new ButtonBuilder("close").onLeftClick(this::onClose).build(), x, null, null);
            frame.showHoveredTooltip();
            Tooltip.renderPending(g, layout.width(), layout.height());
            g.pose().popPose();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return frame != null && frame.click(frame.scale.toUnits(mouseX), frame.scale.toUnits(mouseY), button == GLFW.GLFW_MOUSE_BUTTON_1);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }

    private static List<SettingRow> rows() {
        Button sensitivity = MinimapClientEvents.getCamSensitivityButton();
        Button nightCircles = MinimapClientEvents.getNightCirclesModeButton();
        Button animals = MinimapClientEvents.getHighlightAnimalsButton();
        Button underlines = MinimapClientEvents.getToggleUnderlinesButton();
        Button leaves = OrthoviewClientEvents.getLeavesHidingButton();
        List<SettingRow> rows = new ArrayList<>(List.of(
                new SettingRow(I18n.get("hud.reignofnether.settings.pan_speed"), I18n.get("hud.reignofnether.settings.pan_speed.desc"), null,
                        () -> String.valueOf(Math.round(OrthoviewClientEvents.getPanSensitivityMult() * 10)), () -> true,
                        sensitivity.onLeftClick, sensitivity.onRightClick, () -> true, ""),
                new SettingRow(I18n.get("hud.reignofnether.settings.night_circles"), I18n.get("hud.reignofnether.settings.night_circles.desc"), null,
                        () -> I18n.get("hud.reignofnether.settings.night_circles." + BlockClientEvents.nightCircleMode.name().toLowerCase(Locale.ROOT)),
                        () -> true, nightCircles.onLeftClick, null, () -> true, ""),
                new SettingRow(I18n.get("hud.reignofnether.settings.underlines"), I18n.get("hud.reignofnether.settings.underlines.desc"), null,
                        () -> onOff(MinimapClientEvents.shouldUnderline()), MinimapClientEvents::shouldUnderline,
                        underlines.onLeftClick, null, () -> true, ""),
                new SettingRow(I18n.get("hud.reignofnether.settings.animals"), I18n.get("hud.reignofnether.settings.animals.desc"), null,
                        () -> onOff(MinimapClientEvents.shouldHighlightAnimals()), MinimapClientEvents::shouldHighlightAnimals,
                        animals.onLeftClick, null, () -> true, ""),
                new SettingRow(I18n.get("hud.reignofnether.settings.leaves"), I18n.get("hud.reignofnether.settings.leaves.desc"), null,
                        () -> I18n.get("hud.reignofnether.settings.leaves." + OrthoviewClientEvents.hideLeavesMethod.name().toLowerCase(Locale.ROOT)),
                        () -> true, leaves.onLeftClick, null, () -> true, ""),
                new SettingRow(I18n.get("hud.reignofnether.settings.square_map"), I18n.get("hud.reignofnether.settings.square_map.desc"), null,
                        () -> onOff(ReignOfNetherClientConfigs.SQUARE_MINIMAP.get()), ReignOfNetherClientConfigs.SQUARE_MINIMAP::get,
                        () -> ReignOfNetherClientConfigs.SQUARE_MINIMAP.set(!ReignOfNetherClientConfigs.SQUARE_MINIMAP.get()), null, () -> true, ""),
                new SettingRow(I18n.get("hud.reignofnether.settings.colours"), I18n.get("hud.reignofnether.settings.colours.desc"), null,
                        () -> I18n.get(ReignOfNetherClientConfigs.USE_PLAYER_COLORS.get() ? "hud.reignofnether.settings.colours.players" : "hud.reignofnether.settings.colours.relations"),
                        () -> true, PlayerColors::toggleColorMode, null, () -> true, "")
        ));
        // the colour used for each relation when colouring by relation
        rows.add(relationColour("self", ReignOfNetherClientConfigs.PLAYER_COLOR_SELF));
        rows.add(relationColour("ally", ReignOfNetherClientConfigs.PLAYER_COLOR_ALLY));
        rows.add(relationColour("neutral", ReignOfNetherClientConfigs.PLAYER_COLOR_NEUTRAL));
        rows.add(relationColour("enemy", ReignOfNetherClientConfigs.PLAYER_COLOR_ENEMY));
        return rows;
    }

    private static SettingRow relationColour(String key, ForgeConfigSpec.ConfigValue<Integer> value) {
        int n = PlayerColors.PLAYER_COLOR_TOTAL_COUNT;
        return new SettingRow(I18n.get("hud.reignofnether.settings.colour_" + key), I18n.get("hud.reignofnether.settings.colour_" + key + ".desc"), null,
                () -> PlayerColors.colors[value.get() % n].name.toUpperCase(Locale.ROOT), () -> true,
                () -> value.set((value.get() + 1) % n), () -> value.set((value.get() + n - 1) % n), () -> true, "");
    }

    private static String onOff(boolean on) {
        return I18n.get(on ? "options.on" : "options.off");
    }
}
