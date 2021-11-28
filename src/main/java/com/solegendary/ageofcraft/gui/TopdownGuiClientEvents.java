package com.solegendary.ageofcraft.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import org.lwjgl.glfw.GLFW;

/**
 * Handler for TopdownGui, doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiClientEvents {

    // register the factory that is used on the client to generate a ContainerScreen corresponding to our Container
    @SubscribeEvent
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        System.out.println("onClientSetupEvent");
        ScreenManager.register(TopdownGuiObjects.CONTAINER_TYPE, TopdownGui::new);
    }
}
