package com.solegendary.ageofcraft.util;

import com.solegendary.ageofcraft.cursor.CursorCommonVanillaEvents;
import com.solegendary.ageofcraft.gui.TopdownGuiCommonModEvents;
import com.solegendary.ageofcraft.gui.TopdownGuiCommonVanillaEvents;
import com.solegendary.ageofcraft.units.UnitCommonVanillaEvents;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class CommonEventRegistrar {
    private final IEventBus vanillaEventBus = MinecraftForge.EVENT_BUS;
    private final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    public CommonEventRegistrar() { }

    /**
     * Register common events. This method can be run from the main parent class without using DistExecutor
     */
    public void registerCommonEvents() {
        modEventBus.register(TopdownGuiCommonModEvents.class);
        vanillaEventBus.register(TopdownGuiCommonVanillaEvents.class);
        vanillaEventBus.register(CursorCommonVanillaEvents.class);
        vanillaEventBus.register(UnitCommonVanillaEvents.class);
    }
}
