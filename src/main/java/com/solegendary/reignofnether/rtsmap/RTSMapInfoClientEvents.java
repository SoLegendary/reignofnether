package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RTSMapInfoClientEvents {

    public static ArrayList<String> modeNames = new ArrayList<>();
    public static String selectedMode = "";
    public static String mapName = ""; // TODO: render at top of start menu
    public static String description = "";
    public static Set<String> authors = new HashSet<>();
    public static String version = "";

    public static Button getCycleModeButton() {
        return new ButtonBuilder("Cycle Mode")
                .iconResource(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/rts_start_block_white.png"))
                .isHidden(() -> modeNames.size() <= 1 || Minecraft.getInstance().player == null || Minecraft.getInstance().player.getPermissionLevel() < 2)
                .onLeftClick(() -> {
                    if (!modeNames.contains(selectedMode))
                        return;
                    int index = modeNames.indexOf(selectedMode);
                    if (index < 0)
                        index = 0;
                    String mode = modeNames.get((index + 1) % modeNames.size());
                    RTSMapInfoServerboundPacket.setStartingMode(mode);
                })
                .onRightClick(() -> {
                    if (!modeNames.contains(selectedMode))
                        return;
                    int index = modeNames.indexOf(selectedMode);
                    if (index < 0)
                        index = 0;
                    String mode = modeNames.get(
                            (index - 1 + modeNames.size()) % modeNames.size()
                    );
                    RTSMapInfoServerboundPacket.setStartingMode(mode);
                })
                .tooltipLines(List.of(
                        Component.literal("Map Mode: " + selectedMode).getVisualOrderText()
                ))
                .build();
    }

    public static void reset() {
        modeNames = new ArrayList<>();
        selectedMode = "";
        mapName = "";
        description = "";
        authors = new HashSet<>();
        version = "";
    }
}
