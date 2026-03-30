package com.solegendary.reignofnether.scenario;

import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.RectZone;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
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
