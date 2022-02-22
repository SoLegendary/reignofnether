package com.solegendary.reignofnether.gui;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TopdownGuiServerboundPackets {
    public boolean topdownGuiOpen = false;

    // client-side helper functions
    public static void openTopdownGui() {
        PacketHandler.INSTANCE.sendToServer(new TopdownGuiServerboundPackets(true));
    }
    public static void closeTopdownGui() {
        Minecraft.getInstance().popGuiLayer();
        PacketHandler.INSTANCE.sendToServer(new TopdownGuiServerboundPackets(false));
    }


    // packet-handler functions
    public TopdownGuiServerboundPackets(Boolean pos) { this.topdownGuiOpen = pos; }

    public TopdownGuiServerboundPackets(FriendlyByteBuf buffer) { this.topdownGuiOpen = buffer.readBoolean(); }

    public void encode(FriendlyByteBuf buffer) { buffer.writeBoolean(this.topdownGuiOpen); }


    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            System.out.println("Got packet from client!");

            if (topdownGuiOpen)
                TopdownGuiServerVanillaEvents.openTopdownGui();
            else
                TopdownGuiServerVanillaEvents.closeTopdownGui();

            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}