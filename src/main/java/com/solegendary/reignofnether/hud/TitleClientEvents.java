package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.mixin.SplashRendererAccessor;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class TitleClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    public static String splash = getRandomSplash();

    private static String getRandomSplash() {
        return ((SplashRendererAccessor)MC.getSplashManager().getSplash()).getSplash();
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_SPACE)
            splash = getRandomSplash();
    }
}
