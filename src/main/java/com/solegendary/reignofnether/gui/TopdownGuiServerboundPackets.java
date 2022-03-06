package com.solegendary.reignofnether.gui;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class TopdownGuiServerboundPackets {
    public boolean topdownGuiOpen = false;
    public int playerId = -1; // to track

    // client-side helper functions
    public static void openTopdownGui(int playerId) {
        PacketHandler.INSTANCE.sendToServer(new TopdownGuiServerboundPackets(true, playerId));
    }
    public static void closeTopdownGui(int playerId) {
        Minecraft.getInstance().popGuiLayer();
        PacketHandler.INSTANCE.sendToServer(new TopdownGuiServerboundPackets(false, playerId));
    }


    // packet-handler functions
    public TopdownGuiServerboundPackets(Boolean pos, int playerId) {
        this.topdownGuiOpen = pos;
        this.playerId = playerId;
    }

    public TopdownGuiServerboundPackets(FriendlyByteBuf buffer) {
        this.topdownGuiOpen = buffer.readBoolean();
        this.playerId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.topdownGuiOpen);
        buffer.writeInt(this.playerId);
    }


    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            System.out.println("Got TopdownGui packet from client!");

            if (this.topdownGuiOpen)
                TopdownGuiServerVanillaEvents.openTopdownGui(this.playerId);
            else
                TopdownGuiServerVanillaEvents.closeTopdownGui(this.playerId);

            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}