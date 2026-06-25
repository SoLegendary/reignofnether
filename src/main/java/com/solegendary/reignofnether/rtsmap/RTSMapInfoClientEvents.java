package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.ButtonBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class RTSMapInfoClientEvents {

    public static ArrayList<String> modeNames = new ArrayList<>();
    public static String selectedMode = "";

    // TODO: if permissions >= 2, render at top left of start menu
    public Button getCycleModeButton(ResourceLocation iconResource) {
        if (modeNames == null || modeNames.size() <= 1)
            return null;

        return new ButtonBuilder("Cycle Mode")
                .iconResource(iconResource)
                .onLeftClick(() -> {
                    if (!modeNames.contains(selectedMode))
                        return;
                    int index = modeNames.indexOf(selectedMode);
                    if (index < 0)
                        index = 0;
                    selectedMode = modeNames.get((index + 1) % modeNames.size());
                    // TODO: send to server
                })
                .onRightClick(() -> {
                    if (!modeNames.contains(selectedMode))
                        return;
                    int index = modeNames.indexOf(selectedMode);
                    if (index < 0)
                        index = 0;
                    selectedMode = modeNames.get(
                            (index - 1 + modeNames.size()) % modeNames.size()
                    );
                    // TODO: send to server
                })
                .tooltipLines(List.of(
                        Component.literal("Map Mode: " + selectedMode).getVisualOrderText()
                ))
                .build();
    }
}
