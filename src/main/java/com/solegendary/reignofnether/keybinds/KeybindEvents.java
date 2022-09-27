package com.solegendary.reignofnether.keybinds;

import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class KeybindEvents {

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent evt) {
        Keybinding.registerAllKeys(evt);
    }
}
