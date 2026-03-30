package com.solegendary.reignofnether.hud.playerdisplay;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AllianceAction;
import com.solegendary.reignofnether.alliance.AllianceServerboundPacket;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.hud.TextInputClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerDisplayClientEvents {

    private enum DisplayType {
        OBSERVER,
        DIPLOMACY,
        NONE
    }
    private static final Minecraft MC = Minecraft.getInstance();

    private static ArrayList<ObserverPlayerDisplay> observerPlayerDisplays = new ArrayList<>();
    private static ArrayList<DiplomacyPlayerDisplay> rtsDiplomacyPlayerDisplays = new ArrayList<>();
    private static ArrayList<DiplomacyPlayerDisplay> fpvDiplomacyPlayerDisplays = new ArrayList<>();
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();
    private static final ArrayList<RectZone> hudZones = new ArrayList<>();
    private static DisplayType displayType = DisplayType.NONE;
    private static final int BG_BORDER_WIDTH = 7;

    public static void resetDisplay() {
        displayType = DisplayType.NONE;
    }

    public static final Button observerButton = new Button(
            "Toggle Observer Displays",
            Button.DEFAULT_ICON_SIZE,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/observer.png"),
            Keybindings.keyZ,
            () -> displayType == DisplayType.OBSERVER,
            () -> PlayerClientEvents.isRTSPlayer() || PlayerClientEvents.rtsPlayers.isEmpty(),
            () -> true,
            () -> {
                if (displayType != DisplayType.NONE)
                    displayType = DisplayType.NONE;
                else
                    displayType = DisplayType.OBSERVER;
            },
            null,
            List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.toggle_observer_player_displays"), Style.EMPTY)
    ));

    public static final Button diplomacyButton = new Button(
            "Toggle Diplomacy Displays",
            Button.DEFAULT_ICON_SIZE,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/items/sweet_berries.png"),
            Keybindings.keyZ,
            () -> displayType == DisplayType.DIPLOMACY,
            () -> !PlayerClientEvents.isRTSPlayer() || (PlayerClientEvents.rtsPlayers.size() + getNumFpvPlayers()) <= 1,
            () -> true,
            () -> {
                if (displayType != DisplayType.NONE)
                    displayType = DisplayType.NONE;
                else
                    displayType = DisplayType.DIPLOMACY;
            },
            null,
            List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.toggle_diplomacy_player_displays"), Style.EMPTY)
    ));

    private static int getNumFpvPlayers() {
        if (MC.level == null)
            return 0;
        var size = 0;
        for (AbstractClientPlayer p : MC.level.players()) {
            if (!p.isSpectator() && !p.isCreative()) {
                size++;
            }
        }
        return size;
    }

    public static final Button shareUnitControlButton = new Button(
            "Toggle Share Unit Control",
            Button.DEFAULT_ICON_SIZE,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_back.png"),
            (Keybinding) null,
            AlliancesClient::sharingAllyControl,
            () -> !PlayerClientEvents.isRTSPlayer() || PlayerClientEvents.rtsPlayers.size() <= 1,
            () -> true,
            () -> AllianceServerboundPacket.doAllianceAction(
                AllianceAction.SET_ALLY_CONTROL,
                !AlliancesClient.sharingAllyControl()
            ),
            null,
            List.of(FormattedCharSequence.forward(I18n.get("alliances.reignofnether.tooltip.toggle_shared_unit_control"), Style.EMPTY)
    ));

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() ||
            !(evt.getScreen() instanceof TopdownGui) ||
            MC.level == null) {
            return;
        }
        renderedButtons.clear();
        hudZones.clear();

        if (displayType == DisplayType.DIPLOMACY) {
            renderDiplomacyPlayerDisplays(evt);
        } else if (displayType == DisplayType.OBSERVER) {
            renderObserverPlayerDisplays(evt);
        }
        for (Button button : renderedButtons)
            if (button.isMouseOver(evt.getMouseX(), evt.getMouseY()))
                button.renderTooltip(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            } else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
            }
        }
    }

    @SubscribeEvent
    public static void onKeyRelease(ScreenEvent.KeyReleased.KeyReleased.Post evt) {
        if (TextInputClientEvents.isAnyInputFocused())
            return;
        if (MC.screen == null || !MC.screen.getTitle().getString().contains("topdowngui_container"))
            return;
        for (Button button : renderedButtons)
            button.checkPressed(evt.getKeyCode());
    }

    private static void renderObserverPlayerDisplays(ScreenEvent.Render.Post evt) {
        int screenWidth = MC.getWindow().getGuiScaledWidth();

        int blitX = (screenWidth / 2 - ObserverPlayerDisplay.DISPLAY_WIDTH / 2) + 10;
        int blitY = 70;

        GuiGraphics guiGraphics = evt.getGuiGraphics();

        List<String> list = new ArrayList<>();
        for (RTSPlayer rtsp : PlayerClientEvents.rtsPlayers) {
            String name = rtsp.name;
            list.add(name);
        }
        observerPlayerDisplays.removeIf(r -> !list.contains(r.playerName));

        List<String> trackedPlayers = new ArrayList<>();
        for (ObserverPlayerDisplay d : observerPlayerDisplays) {
            String playerName = d.playerName;
            trackedPlayers.add(playerName);
        }
        for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers) {
            if (!trackedPlayers.contains(rtsPlayer.name)) {
                observerPlayerDisplays.add(new ObserverPlayerDisplay(rtsPlayer));
            }
        }
        guiGraphics.fill(
                blitX - BG_BORDER_WIDTH, blitY - BG_BORDER_WIDTH,
                blitX + ObserverPlayerDisplay.DISPLAY_WIDTH + BG_BORDER_WIDTH,
                (blitY + Button.DEFAULT_ICON_FRAME_SIZE + BG_BORDER_WIDTH) * observerPlayerDisplays.size(),
                0x99000000
        );

        for (ObserverPlayerDisplay playerDisplay : observerPlayerDisplays) {
            hudZones.add(playerDisplay.getRectZone(blitX, blitY, BG_BORDER_WIDTH));
            playerDisplay.render(guiGraphics, blitX, blitY);
            blitY += Button.DEFAULT_ICON_FRAME_SIZE;
        }
    }

    private static void renderDiplomacyPlayerDisplays(ScreenEvent.Render.Post evt) {
        if (MC.level == null || MC.player == null)
            return;

        int screenWidth = MC.getWindow().getGuiScaledWidth();

        int blitX = (screenWidth / 2 - DiplomacyPlayerDisplay.DISPLAY_WIDTH / 2) + 10;
        int blitY = 37;

        GuiGraphics guiGraphics = evt.getGuiGraphics();
        List<String> list = new ArrayList<>();
        for (RTSPlayer rtsp : PlayerClientEvents.rtsPlayers) {
            String name = rtsp.name;
            list.add(name);
        }
        rtsDiplomacyPlayerDisplays.removeIf(d -> !list.contains(d.playerName));
        List<String> result = new ArrayList<>();
        for (AbstractClientPlayer p : MC.level.players()) {
            String string = p.getName().getString();
            result.add(string);
        }
        fpvDiplomacyPlayerDisplays.removeIf(d -> !result.contains(d.playerName));

        Set<String> trackedRtsPlayers = new HashSet<>();
        for (DiplomacyPlayerDisplay rtsDiplomacyPlayerDisplay : rtsDiplomacyPlayerDisplays) {
            String playerName = rtsDiplomacyPlayerDisplay.playerName;
            trackedRtsPlayers.add(playerName);
        }
        Set<String> trackedFpvPlayers = new HashSet<>();
        for (DiplomacyPlayerDisplay fpvDiplomacyPlayerDisplay : fpvDiplomacyPlayerDisplays) {
            String playerName = fpvDiplomacyPlayerDisplay.playerName;
            trackedFpvPlayers.add(playerName);
        }
        for (AbstractClientPlayer player : MC.level.players()) {
            if (player != MC.player) {
                RTSPlayer rtsPlayer = PlayerClientEvents.getRTSPlayer(player.getName().getString());
                if (rtsPlayer == null && !trackedFpvPlayers.contains(player.getName().getString()) &&
                        !player.isSpectator() && !player.isCreative()) {
                    fpvDiplomacyPlayerDisplays.add(new DiplomacyPlayerDisplay(player));
                    rtsDiplomacyPlayerDisplays.removeIf(d -> d.playerName.equals(player.getName().getString()));
                }
            }
        }
        for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers) {
            if (!rtsPlayer.name.equals(MC.player.getName().getString())) {
                if (!trackedRtsPlayers.contains(rtsPlayer.name)) {
                    rtsDiplomacyPlayerDisplays.add(new DiplomacyPlayerDisplay(rtsPlayer));
                    fpvDiplomacyPlayerDisplays.removeIf(d -> d.playerName.equals(rtsPlayer.name));
                }
            }
        }
        boolean canShareUnitControl = !rtsDiplomacyPlayerDisplays.isEmpty() && !shareUnitControlButton.isHidden.get();
        int x1 = blitX - BG_BORDER_WIDTH;
        int y1 = blitY - BG_BORDER_WIDTH;
        int x2 = blitX + DiplomacyPlayerDisplay.DISPLAY_WIDTH + BG_BORDER_WIDTH;
        int y2 = blitY + ((Button.DEFAULT_ICON_FRAME_SIZE + BG_BORDER_WIDTH) *
                        (rtsDiplomacyPlayerDisplays.size() + fpvDiplomacyPlayerDisplays.size() )) +
                        (canShareUnitControl ? (int) (Button.DEFAULT_ICON_FRAME_SIZE * 1.5f) : 0);
        guiGraphics.fill(x1, y1, x2, y2, 0x99000000);
        hudZones.add(new RectZone(x1, y1, x2, y2));

        if (!rtsDiplomacyPlayerDisplays.isEmpty() && !shareUnitControlButton.isHidden.get()) {
            shareUnitControlButton.render(guiGraphics, blitX, blitY, evt.getMouseX(), evt.getMouseY());
            MyRenderer.renderFrameWithBg(
                    guiGraphics,
                    blitX + Button.DEFAULT_ICON_FRAME_SIZE,
                    blitY,
                    102 + (AlliancesClient.sharingAllyControl() ? 10 : 15),
                    Button.DEFAULT_ICON_FRAME_SIZE,
                    0xA0000000
            );
            guiGraphics.drawString(
                    MC.font,
                    "Shared Control: " + (AlliancesClient.sharingAllyControl() ? "ON" : "OFF"),
                    blitX + (Button.DEFAULT_ICON_SIZE / 2) + 1 + Button.DEFAULT_ICON_FRAME_SIZE,
                    blitY + (Button.DEFAULT_ICON_SIZE / 2) + 1,
                    0xFFFFFF
            );
            blitY += (Button.DEFAULT_ICON_FRAME_SIZE * 1.5f);
            renderedButtons.add(shareUnitControlButton);
        }
        for (DiplomacyPlayerDisplay playerDisplay : rtsDiplomacyPlayerDisplays) {
            hudZones.add(playerDisplay.getRectZone(blitX, blitY, BG_BORDER_WIDTH));
            renderedButtons.addAll(playerDisplay.render(guiGraphics, blitX, blitY, evt.getMouseX(), evt.getMouseY()));
            blitY += Button.DEFAULT_ICON_FRAME_SIZE;
        }
        for (DiplomacyPlayerDisplay playerDisplay : fpvDiplomacyPlayerDisplays) {
            hudZones.add(playerDisplay.getRectZone(blitX, blitY, BG_BORDER_WIDTH));
            renderedButtons.addAll(playerDisplay.render(guiGraphics, blitX, blitY, evt.getMouseX(), evt.getMouseY()));
            blitY += Button.DEFAULT_ICON_FRAME_SIZE;
        }
    }

    public static boolean isMouseOverHud(int mouseX, int mouseY) {
        for (RectZone hudZone : hudZones)
            if (hudZone.isMouseOver(mouseX, mouseY))
                return true;
        return false;
    }
}
