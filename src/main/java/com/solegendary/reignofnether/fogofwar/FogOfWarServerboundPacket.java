package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
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

public class FogOfWarServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<FogOfWarServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "fog_of_war_serverbound"));

    @Override
    public CustomPacketPayload.Type<FogOfWarServerboundPacket> type() {
        return TYPE;
    }

    boolean enable;

    public static void setServerFog(boolean enable) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null)
            PacketHandler.sendToServer(new FogOfWarServerboundPacket(enable));
    }

    // packet-handler functions
    public FogOfWarServerboundPacket(boolean enable) {
        this.enable = enable;
    }

    public FogOfWarServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.enable = buffer.readBoolean();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(this.enable);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("FogOfWarServerboundPacket: Sender was null");
                return;
            } else if (!player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("FogOfWarServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                return;
            }

            ReignOfNether.LOGGER.info("[FogOfWar] {} set fog of war to {}", player.getName(), enable);

            FogOfWarServerEvents.setEnabled(enable);
        });
        return;
    }
}