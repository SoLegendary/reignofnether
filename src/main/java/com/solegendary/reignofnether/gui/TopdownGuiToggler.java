package com.solegendary.reignofnether.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TopdownGuiToggler {
    public boolean topdownGuiOpen = false;

    public TopdownGuiToggler(Boolean pos) { this.topdownGuiOpen = pos; }

    public TopdownGuiToggler(FriendlyByteBuf buffer) { this.topdownGuiOpen = buffer.readBoolean(); }

    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(this.topdownGuiOpen); }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            System.out.println("Got packet from client!");

            //if (topdownGuiOpen)
            //    TopdownGuiServerVanillaEvents.openTopdownGui();
            //else
            //    TopdownGuiServerVanillaEvents.closeTopdownGui();

            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}