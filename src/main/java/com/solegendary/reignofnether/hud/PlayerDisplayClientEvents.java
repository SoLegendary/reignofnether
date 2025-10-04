package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AllianceAction;
import com.solegendary.reignofnether.alliance.AllianceServerboundPacket;
import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerDisplayClientEvents {

    private enum DisplayType {
        OBSERVER,
        DIPLOMACY,
        NONE
    }
    private static final Minecraft MC = Minecraft.getInstance();

    private static ArrayList<ObserverPlayerDisplay> observerPlayerDisplays = new ArrayList<>();
    private static ArrayList<DiplomacyPlayerDisplay> diplomacyPlayerDisplays = new ArrayList<>();
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();
    private static final ArrayList<RectZone> hudZones = new ArrayList<>();
    private static DisplayType displayType = DisplayType.NONE;

    public static void resetDisplay() {
        displayType = DisplayType.NONE;
    }

    public static final Button observerButton = new Button(
            "Toggle Observer Displays",
            Button.DEFAULT_ICON_SIZE,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/observer.png"),
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
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sweet_berries.png"),
            Keybindings.keyZ,
            () -> displayType == DisplayType.DIPLOMACY,
            () -> !PlayerClientEvents.isRTSPlayer() || PlayerClientEvents.rtsPlayers.size() <= 1,
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

    public static final Button shareUnitControlButton = new Button(
            "Toggle Share Unit Control",
            Button.DEFAULT_ICON_SIZE,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/blocks/chain_command_block_back.png"),
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
        if (MC.screen == null || !MC.screen.getTitle().getString().contains("topdowngui_container")) {
            return;
        }
        for (Button button : renderedButtons)
            button.checkPressed(evt.getKeyCode());
    }

    private static void renderObserverPlayerDisplays(ScreenEvent.Render.Post evt) {
        int screenWidth = MC.getWindow().getGuiScaledWidth();

        int blitX = screenWidth / 2 - ObserverPlayerDisplay.DISPLAY_WIDTH / 2;
        int blitY = 70;

        GuiGraphics guiGraphics = evt.getGuiGraphics();

        observerPlayerDisplays.removeIf(r -> !PlayerClientEvents.rtsPlayers.contains(r.rtsPlayer));

        List<RTSPlayer> trackedPlayers = observerPlayerDisplays.stream().map(d -> d.rtsPlayer).collect(Collectors.toCollection(ArrayList::new));
        for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers) {
            if (!trackedPlayers.contains(rtsPlayer)) {// && !rtsPlayer.name.equals(MC.player.getName().getString())) {
                observerPlayerDisplays.add(new ObserverPlayerDisplay(rtsPlayer));
            }
        }
        for (ObserverPlayerDisplay playerDisplay : observerPlayerDisplays) {
            hudZones.add(new RectZone(
                blitX, blitY,
                blitX + ObserverPlayerDisplay.DISPLAY_WIDTH,
                blitY + Button.DEFAULT_ICON_FRAME_SIZE)
            );
            playerDisplay.render(guiGraphics, blitX, blitY);
            blitY += Button.DEFAULT_ICON_FRAME_SIZE;
        }
    }

    private static void renderDiplomacyPlayerDisplays(ScreenEvent.Render.Post evt) {
        int screenWidth = MC.getWindow().getGuiScaledWidth();

        int blitX = screenWidth / 2 - DiplomacyPlayerDisplay.DISPLAY_WIDTH / 2;
        int blitY = 37;

        GuiGraphics guiGraphics = evt.getGuiGraphics();

        diplomacyPlayerDisplays.removeIf(r -> !PlayerClientEvents.rtsPlayers.contains(r.rtsPlayer));

        List<RTSPlayer> trackedPlayers = diplomacyPlayerDisplays.stream().map(d -> d.rtsPlayer).collect(Collectors.toCollection(ArrayList::new));
        for (RTSPlayer rtsPlayer : PlayerClientEvents.rtsPlayers) {
            if (!trackedPlayers.contains(rtsPlayer) && MC.player != null && !rtsPlayer.name.equals(MC.player.getName().getString())) {
                diplomacyPlayerDisplays.add(new DiplomacyPlayerDisplay(rtsPlayer));
            }
        }
        if (!diplomacyPlayerDisplays.isEmpty()) {
            if (!shareUnitControlButton.isHidden.get()) {
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
        }
        for (DiplomacyPlayerDisplay playerDisplay : diplomacyPlayerDisplays) {
            hudZones.add(new RectZone(
                blitX, blitY,
                blitX + DiplomacyPlayerDisplay.DISPLAY_WIDTH,
                blitY + Button.DEFAULT_ICON_FRAME_SIZE)
            );
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
