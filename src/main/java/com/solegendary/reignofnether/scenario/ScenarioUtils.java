package com.solegendary.reignofnether.scenario;

import net.minecraft.resources.ResourceLocation;

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
}
