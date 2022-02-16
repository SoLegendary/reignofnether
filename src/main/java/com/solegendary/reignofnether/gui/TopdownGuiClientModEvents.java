package com.solegendary.reignofnether.gui;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Handler for TopdownGui, doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiClientModEvents {

    private static boolean isGuiOpen = false;

    @SubscribeEvent
    public static void registerContainers(final RegistryEvent.Register<MenuType<?>> event)
    {
        TopdownGuiObjects.CONTAINER_TYPE = IForgeMenuType.create(TopdownGuiContainer::createContainerClientSide);
        TopdownGuiObjects.CONTAINER_TYPE.setRegistryName("topdowngui_container");
        event.getRegistry().register(TopdownGuiObjects.CONTAINER_TYPE);
    }

    @SubscribeEvent
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        MenuScreens.register(TopdownGuiObjects.CONTAINER_TYPE, TopdownGui::new);
    }
}
