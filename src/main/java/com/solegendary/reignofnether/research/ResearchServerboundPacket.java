package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class ResearchServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ResearchServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_serverbound"));

    @Override
    public CustomPacketPayload.Type<ResearchServerboundPacket> type() {
        return TYPE;
    }

    public String playerName;
    public String itemName;
    public boolean add; // false for remove
    public boolean isCheat;
    public int value;

    public static void addCheat(String playerName, String itemName) {
        PacketHandler.sendToServer(new ResearchServerboundPacket(playerName, itemName, true, true, 0));
    }
    public static void removeCheat(String playerName, String itemName) {
        PacketHandler.sendToServer(new ResearchServerboundPacket(playerName, itemName, false, true, 0));
    }

    public ResearchServerboundPacket(String playerName, String itemName, boolean add, boolean isCheat, int value) {
        this.playerName = playerName;
        this.itemName = itemName;
        this.add = add;
        this.isCheat = isCheat;
        this.value = value;
    }

    public ResearchServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.itemName = buffer.readUtf();
        this.add = buffer.readBoolean();
        this.isCheat = buffer.readBoolean();
        this.value = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeUtf(this.itemName);
        buffer.writeBoolean(this.add);
        buffer.writeBoolean(this.isCheat);
        buffer.writeInt(this.value);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("ResearchServerboundPacket (cheats): Sender was null");
                return;
            } else if (!player.getName().getString().equals(this.playerName)) {
                ReignOfNether.LOGGER.warn("ResearchServerboundPacket (cheats): Tried to process packet from " + player.getName() + " for id: " + this.playerName);
                return;
            }

            if (isCheat) {
                ReignOfNether.LOGGER.info("[Research] {} {} cheat research: {}", player.getName(), add ? "added" : "removed", this.itemName);
                if (!player.hasPermissions(4)) {
                    ReignOfNether.LOGGER.warn("ResearchServerboundPacket (cheats): Tried to process packet from " + player.getName() + " with insufficient permissions");
                    return;
                }
                if (add) {
                    ResearchServerEvents.addCheat(this.playerName, this.itemName);
                    ResearchClientboundPacket.addCheat(this.playerName, this.itemName);
                }
                else {
                    ResearchServerEvents.removeCheat(this.playerName, this.itemName);
                    ResearchClientboundPacket.removeCheat(this.playerName, this.itemName);
                }
            }
        });
        return;
    }
}
