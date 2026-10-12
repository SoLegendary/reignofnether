package com.solegendary.reignofnether.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Who a payload is being sent to.
 *
 * <p>Forge expressed this with {@code PacketDistributor.ALL.noArg()} /
 * {@code PacketDistributor.PLAYER.with(supplier)}, chosen at each call site and then handed to
 * {@code SimpleChannel.send}. NeoForge 21.1 has no channel object to hand a distributor to —
 * the static methods on {@link PacketDistributor} are the whole API — so the mod resolves the
 * recipient itself and keeps the same call-site shape.
 *
 * <p>Only the two targets this mod ever used are implemented; adding a third means adding a
 * factory here, so an unsupported recipient fails at compile time instead of silently dropping
 * the packet.
 */
@FunctionalInterface
public interface PacketTarget {

    /** Every player currently connected to the server. */
    PacketTarget ALL_PLAYERS = payload -> PacketDistributor.sendToAllPlayers(payload);

    /** The player the supplier resolves to, skipping the send if it no longer exists. */
    static PacketTarget toPlayer(Supplier<ServerPlayer> player) {
        return payload -> {
            ServerPlayer target = player.get();
            if (target != null) PacketDistributor.sendToPlayer(target, payload);
        };
    }

    void send(RTSSimplePayload payload);
}