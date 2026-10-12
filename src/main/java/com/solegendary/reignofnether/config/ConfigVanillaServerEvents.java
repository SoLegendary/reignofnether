package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.function.Supplier;

public class ConfigVanillaServerEvents {
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        Supplier<ServerPlayer> serverPlayerSupplier = () -> (ServerPlayer) evt.getEntity();
        //ReignOfNether.LOGGER.info("onPlayerJoined fired from ConfigVanillaServerEvents");
        //If this is not a singleplayer server that we own..
        if(!evt.getEntity().getServer().isSingleplayerOwner(evt.getEntity().getGameProfile())) {
            //rebake from serverside configs
            //System.out.println("Attempted to send packet to rebake from server..");
            for(String id : ResourceCost.ENTRIES.keySet()) {
                PacketHandler.send(PacketHandler.toPlayer(serverPlayerSupplier),
                        new ClientboundSyncResourceCostPacket(ResourceCost.ENTRIES.get(id))
                );
            }
        }
    }
}
