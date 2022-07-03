package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.units.UnitServerboundPacket;
import dev.architectury.event.events.common.BlockEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static StructureTemplate structureSelected = null;

    // highlights an area to construct a building by drawing transparent green/red faces around it
    // based on whether the location is valid or not
    // location should be 1 space above the selected spot
    public static void highlightArea(BlockPos bp) {

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelLastEvent evt) {
        if (structureSelected != null) {

        }
    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {

        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (evt.getKey() == Keybinds.fnums[6].getKey().getValue()) {
                structureSelected = BuildingTemplates.VILLAGER_HOUSE;
            }
            else if (evt.getKey() == Keybinds.fnums[7].getKey().getValue()) {
                structureSelected = BuildingTemplates.VILLAGER_TOWER;
            }
            else if (evt.getKey() == Keybinds.fnums[8].getKey().getValue()) {
                structureSelected = null;
            }
        }
        if (evt.getAction() == GLFW.GLFW_MOUSE_BUTTON_1 && structureSelected != null) {

        }
    }
}
