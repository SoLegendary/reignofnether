package com.solegendary.reignofnether.fogofwar;

import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FogOfWarServerEvents {

    private static boolean enabled = false; // enforced for all clients

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        syncClientFog();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        syncClientFog();
    }

    // sets the fog to match what all
    private static int syncClientFog() {
        FogOfWarClientboundPacket.setEnabled(enabled);
        return 1;
    }
}
