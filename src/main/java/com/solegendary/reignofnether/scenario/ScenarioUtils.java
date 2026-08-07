package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.List;

public class ScenarioUtils {

    public static ResourceLocation getScenarioRoleColourTexture(String scenarioRole) {
        return null;
    }

    @Nullable
    public static ScenarioRole getScenarioRole(boolean isClientSide, int roleIndex) {
        List<ScenarioRole> roles = isClientSide ? ScenarioClientEvents.scenarioRoles : ScenarioServerEvents.scenarioRoles;
        for (ScenarioRole role : roles)
            if (role.index == roleIndex)
                return role;
        return null;
    }

    @Nullable
    public static boolean isScenarioNpc(boolean isClientSide, int roleIndex) {
        List<ScenarioRole> roles = isClientSide ? ScenarioClientEvents.scenarioRoles : ScenarioServerEvents.scenarioRoles;
        for (ScenarioRole role : roles)
            if (role.index == roleIndex)
                return role.isNpc;
        return false;
    }

    @Nullable
    public static boolean isScenarioNpc(boolean isClientSide, String name) {
        RTSPlayer rtsPlayer = isClientSide ? PlayerClientEvents.getRTSPlayer(name) : PlayerServerEvents.getRTSPlayer(name);
        return rtsPlayer != null && isScenarioNpc(isClientSide, rtsPlayer.scenarioRoleIndex);
    }
}
