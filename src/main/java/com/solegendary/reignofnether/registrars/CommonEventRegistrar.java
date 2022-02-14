package com.solegendary.reignofnether.registrars;

import com.solegendary.reignofnether.cursor.CursorCommonVanillaEvents;
import com.solegendary.reignofnether.gui.TopdownGuiCommonModEvents;
import com.solegendary.reignofnether.gui.TopdownGuiCommonVanillaEvents;
import com.solegendary.reignofnether.units.UnitCommonModEvents;
import com.solegendary.reignofnether.units.UnitCommonVanillaEvents;

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
        modEventBus.register(UnitCommonModEvents.class);
    }
}
