package com.solegendary.reignofnether.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A packet of this mod: a {@link CustomPacketPayload} whose body is written through a plain
 * {@link FriendlyByteBuf}, the way the Forge 1.20.1 build wrote every one of its 71 packets.
 *
 * <p>Keeping the codec and the handler shape this thin is deliberate. On 1.20.1 each packet
 * declared {@code encode(FriendlyByteBuf)}, a {@code Packet(FriendlyByteBuf)} constructor and
 * {@code handle(Supplier<NetworkEvent.Context>)}; NeoForge 21.1 removed
 * {@code NetworkEvent.Context} entirely, so the handlers now receive an
 * {@link IPayloadContext} directly. Everything else - the field order in the buffer, the
 * packet bodies, the call sites that send them - stays exactly as it was, so the wire format
 * and the game logic are untouched by the port.
 */
public interface RTSSimplePayload extends CustomPacketPayload {

    /** Writes this packet's fields to the buffer. */
    void encode(RegistryFriendlyByteBuf buffer);

    /** Runs on the main thread; the registrar is configured for that. */
    void handle(IPayloadContext ctx);
}