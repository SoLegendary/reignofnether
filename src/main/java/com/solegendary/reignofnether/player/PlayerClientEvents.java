package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.util.Faction;
import net.minecraft.commands.Commands;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerClientEvents {

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("startrts").then(Commands.literal("villagers")
                .executes((command) -> {
                    PlayerServerboundPacket.startRTS(Faction.VILLAGERS);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("startrts").then(Commands.literal("monsters")
                .executes((command) -> {
                    PlayerServerboundPacket.startRTS(Faction.MONSTERS);
                    return 1;
                })));
    }
}
