package com.solegendary.reignofnether.gui;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.ContainerRegistrar;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Handler for TopdownGui, doing stuff like initialising it and controlling where it comes up in the game client
 *
 * @author SoLegendary
 */

public class TopdownGuiClientModEvents {

    @SubscribeEvent
    public static void onClientSetupEvent(FMLClientSetupEvent evt) {
        MenuScreens.register(ContainerRegistrar.TOPDOWNGUI_CONTAINER.get(), TopdownGui::new);
    }
}
