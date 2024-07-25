package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TutorialServerEvents {

    private static final String TUTORIAL_MAP_NAME = "reign_of_nether_tutorial";
    private static final Long TUTORIAL_MAP_SEED = 4756899154123723533L;
    private static boolean enabled = false;

    public static boolean isEnabled() { return enabled; }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        MinecraftServer server = evt.getEntity().getServer();
        if (server == null) {
            PlayerServerEvents.sendMessageToAllPlayers("Failed to load tutorial, server couldn't be found.");
            return;
        }
        String levelName = server.getWorldData().getLevelSettings().levelName();
        if (evt.getEntity().getLevel() instanceof ServerLevel serverLevel)
            if (serverLevel.getSeed() == TUTORIAL_MAP_SEED &&
                levelName.equals(TUTORIAL_MAP_NAME)) {
                TutorialClientboundPacket.setEnableTutorial(true);
                enabled = true;
            }
    }
}
