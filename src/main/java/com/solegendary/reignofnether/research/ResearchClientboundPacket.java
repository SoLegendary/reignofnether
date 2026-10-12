package com.solegendary.reignofnether.research;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class ResearchClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ResearchClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "research_clientbound"));

    @Override
    public CustomPacketPayload.Type<ResearchClientboundPacket> type() {
        return TYPE;
    }

    public String playerName;
    public String itemName;
    public boolean add; // false for remove
    public boolean isCheat;
    public int value;

    public static void addCheat(String playerName, String itemName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new ResearchClientboundPacket(playerName, itemName, true, true, 0));
    }
    public static void addCheatWithValue(String playerName, String itemName, int value) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new ResearchClientboundPacket(playerName, itemName, true, true, value));
    }
    public static void removeCheat(String playerName, String itemName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new ResearchClientboundPacket(playerName, itemName, false, true, 0));
    }
    public static void addResearch(String playerName, String itemName) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new ResearchClientboundPacket(playerName, itemName, true, false, 0));
    }

    public ResearchClientboundPacket(String playerName, String itemName, boolean add, boolean isCheat, int value) {
        this.playerName = playerName;
        this.itemName = itemName;
        this.add = add;
        this.isCheat = isCheat;
        this.value = value;
    }

    public ResearchClientboundPacket(RegistryFriendlyByteBuf buffer) {
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
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (Minecraft.getInstance().player.getName().getString().equals(this.playerName)) {
                            if (isCheat) {
                                if (value > 0)
                                    ResearchClient.addCheatWithValue(this.itemName, this.value);
                                else if (add)
                                    ResearchClient.addCheat(this.itemName);
                                else
                                    ResearchClient.removeCheat(this.itemName);
                            } else {
                                if (add)
                                    ResearchClient.addResearch(this.playerName, ResourceLocation.tryParse(this.itemName));
                            }
                        }
                    });
        });
        return;
    }
}
