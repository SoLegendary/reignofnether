package com.solegendary.reignofnether.building;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.units.UnitServerboundPacket;
import dev.architectury.event.events.common.BlockEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class BuildingClientEvents {

    static final Minecraft MC = Minecraft.getInstance();

    public static String buildingSelected; // TODO: change to whatever the structure NBT data class should be

    // highlights an area to construct a building by drawing transparent green/red faces around it
    // based on whether the location is valid or not
    public static void highlightArea() {

    }

    @SubscribeEvent
    public static void onInput(InputEvent.KeyInputEvent evt) {
        if (evt.getAction() == GLFW.GLFW_PRESS) { // prevent repeated key actions

            if (MC.player != null) {
                if (evt.getKey() == Keybinds.fnums[6].getKey().getValue()) {
                    BuildingServerboundPacket.placeBlock(
                            MC.player.blockPosition(), null
                    );
                }
                else if (evt.getKey() == Keybinds.fnums[7].getKey().getValue()) {
                    BuildingServerboundPacket.destroyBlock(
                            MC.player.blockPosition()
                    );
                }
            }
        }
    }
}
