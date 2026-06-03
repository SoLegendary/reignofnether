package com.solegendary.reignofnether.commands;

import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

// Centralised debug mode flag. Server-authoritative — clients receive updates via RtsDebugClientboundPacket.
// When enabled: path-preview lines stay visible and a perf-stats overlay renders top-right.
public class RtsDebug {

    public static boolean enabled = false;

    public static void setEnabled(boolean value) {
        enabled = value;
        RtsDebugClientboundPacket.broadcast(value);
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("rts-debug").then(Commands.literal("enable")
                .executes((ctx) -> { setEnabled(true);  return 1; })));
        evt.getDispatcher().register(Commands.literal("rts-debug").then(Commands.literal("disable")
                .executes((ctx) -> { setEnabled(false); return 1; })));
    }
}
