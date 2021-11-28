package com.solegendary.ageofcraft.gui;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Handler for TopdownGui, doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiCommonModEvents {

    private static boolean isGuiOpen = false;

    @SubscribeEvent
    public static void registerContainers(final RegistryEvent.Register<ContainerType<?>> event)
    {
        System.out.println("registerContainers");
        TopdownGuiObjects.CONTAINER_TYPE = IForgeContainerType.create(TopdownGuiContainer::createContainerClientSide);
        TopdownGuiObjects.CONTAINER_TYPE.setRegistryName("topdowngui_container");
        event.getRegistry().register(TopdownGuiObjects.CONTAINER_TYPE);
    }
}
