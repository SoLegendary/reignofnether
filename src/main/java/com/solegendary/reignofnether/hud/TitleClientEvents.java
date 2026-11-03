package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.mixin.SplashRendererAccessor;
import com.solegendary.reignofnether.util.MiscUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TitleClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Random random = new Random();
    private static Faction titleBackgroundFaction = getRandomFaction();
    private static PanoramaRenderer panorama = new PanoramaRenderer(getCubeMap());
    public static String splash = getRandomSplash();

    public static PanoramaRenderer getPanorama() { return panorama; }

    private static CubeMap getCubeMap() {
        return new CubeMap(
            ResourceLocation.fromNamespaceAndPath(titleBackgroundFaction.key.getNamespace(), 
                String.format("textures/gui/title/background/%s/panorama", titleBackgroundFaction.key.getPath())
            )
        );
    }

    private static Faction getRandomFaction() {
        return Factions.getFaction(MiscUtil.getRandomItem(Factions.CLASSIC_FACTIONS));
    }

    private static Faction getNewRandomFaction() {
        List<ResourceLocation> factions = new ArrayList<>(Factions.CLASSIC_FACTIONS);
        factions.remove(titleBackgroundFaction.key);
        return Factions.getFaction(MiscUtil.getRandomItem(factions));
    }

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening evt) {
        if (evt.getScreen() instanceof TitleScreen) {
            titleBackgroundFaction = getNewRandomFaction();
            panorama = new PanoramaRenderer(getCubeMap());
        }
    }

    private static String getRandomSplash() {
        return ((SplashRendererAccessor)MC.getSplashManager().getSplash()).getSplash();
    }

    @SubscribeEvent
    public static void onKeyPress(ScreenEvent.KeyPressed.KeyPressed.Pre evt) {
        if (evt.getKeyCode() == GLFW.GLFW_KEY_SPACE)
            splash = getRandomSplash();
    }
}
