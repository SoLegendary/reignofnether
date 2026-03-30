package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ScenarioServerEvents {

    // index 0 is treated as neutral
    public static final ArrayList<ScenarioRole> scenarioRoles = new ArrayList<>(List.of(
            new ScenarioRole(0),
            new ScenarioRole(1),
            new ScenarioRole(2),
            new ScenarioRole(3),
            new ScenarioRole(4),
            new ScenarioRole(5),
            new ScenarioRole(6),
            new ScenarioRole(7)
    ));

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            saveScenarioRoles(level);
        }
    }

    public static void saveScenarioRoles() {
        saveScenarioRoles(PlayerServerEvents.serverLevel);
    }

    public static void saveScenarioRoles(ServerLevel level) {
        ScenarioRoleSaveData scenarioRoleSaveData = ScenarioRoleSaveData.getInstance(level);
        scenarioRoleSaveData.scenarioRoleSaves.clear();
        scenarioRoleSaveData.scenarioRoleSaves.addAll(scenarioRoles);
        scenarioRoleSaveData.save();
        level.getDataStorage().save();
    }

    @SubscribeEvent
    public static void loadScenarioRoles(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            ScenarioRoleSaveData scenarioRoleSaveData = ScenarioRoleSaveData.getInstance(level);
            if (!scenarioRoleSaveData.scenarioRoleSaves.isEmpty()) {
                scenarioRoles.clear();
                scenarioRoles.addAll(scenarioRoleSaveData.scenarioRoleSaves);
            }
            ReignOfNether.LOGGER.info("loaded scenario roles in serverevents");
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        MinecraftServer server = evt.getEntity().level().getServer();
        if (server == null || !server.isDedicatedServer()) {
            CompletableFuture.delayedExecutor(1000,  TimeUnit.MILLISECONDS).execute(() -> syncScenarioRoles());
        } else {
            syncScenarioRoles();
        }
    }

    public static void syncScenarioRoles() {
        for (ScenarioRole role : scenarioRoles) {
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new ScenarioClientboundPacket(
                    ScenarioAction.LOAD_SCENARIO_ROLE, role.nbt
            ));
        }
    }

    public static int getNumScenarioUnits() {
        int count = 0;
        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (le instanceof Unit unit && unit.getScenarioRoleIndex() >= 0) {
                count++;
            }
        }
        return count;
    }

    public static int getNumScenarioBuildings() {
        int count = 0;
        for (BuildingPlacement bpl : BuildingServerEvents.getBuildings()) {
            if (bpl.scenarioRoleIndex >= 0) {
                count++;
            }
        }
        return count;
    }
}

