package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GameModeServerEvents {

    private static GameMode getGameMode(ServerLevel level) {
        for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
            if (rtsPlayer.faction == Factions.NONE)
                return GameMode.SANDBOX;

        return GameMode.CLASSIC;
    }

    private static boolean isGameModeLocked() {
        return !PlayerServerEvents.rtsPlayers.isEmpty();
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (isGameModeLocked() && !evt.getEntity().level().isClientSide())
            GameModeClientboundPacket.setAndLockAllClientGameModes(getGameMode((ServerLevel) evt.getEntity().level()));
    }
}
