package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.registrars.Keybinds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static Building buildingSelected = null;
    public static Boolean initedStructures = false;

    // highlights an area to construct a building by drawing transparent green/red faces around it
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    public static void highlightArea(BlockPos bp) {

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelLastEvent evt) {
        if (buildingSelected != null) {

        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.ClientTickEvent evt) {
        if (!initedStructures) {
            Buildings.VILLAGER_HOUSE.loadNbt(MC);
            Buildings.VILLAGER_TOWER.loadNbt(MC);
            initedStructures = true;
        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (evt.getKey() == Keybinds.fnums[6].getKey().getValue()) {
                buildingSelected = Buildings.VILLAGER_HOUSE;
            }
            else if (evt.getKey() == Keybinds.fnums[7].getKey().getValue()) {
                buildingSelected = Buildings.VILLAGER_TOWER;
            }
            else if (evt.getKey() == Keybinds.fnums[8].getKey().getValue()) {
                buildingSelected = null;
            }
        }
        if (evt.getAction() == GLFW.GLFW_MOUSE_BUTTON_1 && buildingSelected != null) {

        }
    }
}
