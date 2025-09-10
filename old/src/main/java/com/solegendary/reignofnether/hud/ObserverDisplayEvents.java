package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ObserverDisplayEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    private static ArrayList<ObserverPlayerDisplay> playerDisplays = new ArrayList<>();

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {

        if (!OrthoviewClientEvents.isEnabled() ||
            !(evt.getScreen() instanceof TopdownGui) ||
            !(Keybindings.tab.isDown())) {
            return;
        }

        if (MC.level == null) {
            return;
        }

        if (PlayerClientEvents.isRTSPlayer) {
            return; // only render for spectators
        }

        var mouseX = evt.getMouseX();
        var mouseY = evt.getMouseY();

        int screenWidth = MC.getWindow().getGuiScaledWidth();
        int screenHeight = MC.getWindow().getGuiScaledHeight();

        int blitX = screenWidth / 2 - ObserverPlayerDisplay.DISPLAY_WIDTH / 2;
        int blitY = 70;

        var guiGraphics = evt.getGuiGraphics();

        playerDisplays.removeIf(r -> !ResourcesClientEvents.resourcesList.contains(r.resources));

        var trackedResources = playerDisplays.stream().map(d -> d.resources).collect(Collectors.toCollection(ArrayList::new));
        for (Resources resources : ResourcesClientEvents.resourcesList) {
            if (!trackedResources.contains(resources)) {
                playerDisplays.add(new ObserverPlayerDisplay(resources));
            }
        }

        for (ObserverPlayerDisplay playerDisplay : playerDisplays) {
            playerDisplay.render(guiGraphics, blitX, blitY);
            blitY += Button.DEFAULT_ICON_FRAME_SIZE;
        }
    }
}
