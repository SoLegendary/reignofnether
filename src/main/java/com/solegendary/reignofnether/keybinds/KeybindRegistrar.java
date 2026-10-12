package com.solegendary.reignofnether.keybinds;

import net.neoforged.fml.common.EventBusSubscriber;
import com.solegendary.reignofnether.ReignOfNether;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = ReignOfNether.MOD_ID, value = Dist.CLIENT)
public class KeybindRegistrar {

    @SubscribeEvent
    public static void onRegister(RegisterKeyMappingsEvent event) {
        for (Keybinding kb : Keybindings.all()) {
            if (kb.getMapping() != null) {
                event.register(kb.getMapping());
            }
        }
    }
}
