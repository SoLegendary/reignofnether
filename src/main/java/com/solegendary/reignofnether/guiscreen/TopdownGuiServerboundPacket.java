package com.solegendary.reignofnether.guiscreen;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class TopdownGuiServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<TopdownGuiServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "topdown_gui_serverbound"));

    @Override
    public CustomPacketPayload.Type<TopdownGuiServerboundPacket> type() {
        return TYPE;
    }
    public boolean topdownGuiOpen = false;
    public int playerId = -1; // to track

    // client-side helper functions
    public static void openTopdownGui(int playerId) {
        PacketHandler.sendToServer(new TopdownGuiServerboundPacket(true, playerId));
    }
    public static void closeTopdownGui(int playerId) {
        Minecraft.getInstance().popGuiLayer();
        PacketHandler.sendToServer(new TopdownGuiServerboundPacket(false, playerId));
    }

    // packet-handler functions
    public TopdownGuiServerboundPacket(Boolean pos, int playerId) {
        this.topdownGuiOpen = pos;
        this.playerId = playerId;
    }

    public TopdownGuiServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.topdownGuiOpen = buffer.readBoolean();
        this.playerId = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(this.topdownGuiOpen);
        buffer.writeInt(this.playerId);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("TopdownGuiServerboundPacket: Sender was null");
                return;
            } else if (player.getId() != playerId) {
                ReignOfNether.LOGGER.warn("TopdownGuiServerboundPacket: Tried to process packet from " + player.getName() + " for id: " + this.playerId);
                return;
            }

            if (this.topdownGuiOpen)
                PlayerServerEvents.openTopdownGui(this.playerId);
            else
                PlayerServerEvents.closeTopdownGui(this.playerId);

        });
        return;
    }
}