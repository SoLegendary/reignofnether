package com.solegendary.reignofnether.keybinds;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID,
                        bus = Mod.EventBusSubscriber.Bus.MOD,
                        value = Dist.CLIENT)
public class KeybindRegistrar {

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        for (Keybinding kb : Keybindings.all()) {
            if (kb.mapping != null) {
                event.register(kb.mapping);
            }
        }
    }
}
