package com.solegendary.reignofnether.tutorial;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TutorialServerEvents {

    private static final String TUTORIAL_MAP_NAME = "reign_of_nether_tutorial";
    private static final Long TUTORIAL_MAP_SEED = 4756899154123723533L;
    private static boolean enabled = false;

    private static final Vec3i ANIMAL_POS = new Vec3i(-2923, 67, -1184);

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
                TutorialClientboundPacket.enableTutorial();
                enabled = true;
            }
    }

    public static void spawnAnimals() {
        if (PlayerServerEvents.players.isEmpty())
            return;
        ServerLevel level = PlayerServerEvents.players.get(0).getLevel();
        for (int i = 0; i < 3; i++) {
            Entity entity = EntityType.PIG.create(level);
            if (entity != null) {
                entity.moveTo(ANIMAL_POS.getX() + i, ANIMAL_POS.getY(), ANIMAL_POS.getZ());
                level.addFreshEntity(entity);
            }
        }
    }
}
