package com.solegendary.reignofnether.fogofwar;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.keybinds.Keybindings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;

public class FogOfWarClientEvents {

    public static final ArrayList<LevelRenderer.RenderChunkInfo> oldChunks = new ArrayList<>();

    static final Minecraft MC = Minecraft.getInstance();
    static ArrayList<Pair<BlockPos, Direction>> foggedBlocks = new ArrayList<>(); // x/z coords that are in fog of war (to darken on the minimap)

    public static float brightnessMulti = 0f;

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == Keybindings.getFnum(9).key) {
            brightnessMulti -= 0.05f;
            System.out.println(brightnessMulti);
        }
        else if (evt.getKeyCode() == Keybindings.getFnum(10).key) {
            brightnessMulti += 0.05f;
            System.out.println(brightnessMulti);
        }
    }
}
