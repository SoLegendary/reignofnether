package com.solegendary.ageofcraft.gui;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
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
    public static void registerContainers(final RegistryEvent.Register<MenuType<?>> event)
    {
        TopdownGuiObjects.CONTAINER_TYPE = IForgeMenuType.create(TopdownGuiContainer::createContainerClientSide);
        TopdownGuiObjects.CONTAINER_TYPE.setRegistryName("topdowngui_container");
        event.getRegistry().register(TopdownGuiObjects.CONTAINER_TYPE);
    }
}
