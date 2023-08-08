package com.solegendary.reignofnether.fogofwar;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static com.solegendary.reignofnether.player.PlayerServerEvents.sendMessageToAllPlayers;

public class FogOfWarServerEvents {

    private static boolean enabled = false; // enforced for all clients

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        syncClientFog();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        sendMessageToAllPlayers((enabled ? "Enabled" : "Disabled") + " fog of war for all players");
        syncClientFog();
    }

    // sets the fog to match what all
    private static void syncClientFog() {
        FogOfWarClientboundPacket.setEnabled(enabled);
    }
}
