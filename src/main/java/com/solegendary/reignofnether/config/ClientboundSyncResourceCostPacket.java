package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/*
    Clientbound packet to synchronize serverside config options with the client
    so that the GUI and other elements can properly reflect the values present on the server.
 */
public class ClientboundSyncResourceCostPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<ClientboundSyncResourceCostPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "clientbound_sync_resource_cost"));

    @Override
    public CustomPacketPayload.Type<ClientboundSyncResourceCostPacket> type() {
        return TYPE;
    }
    private final int food;
    private final int wood;
    private final int ore;
    private final int ticks;
    private final int population;
    private final String id;

    public ClientboundSyncResourceCostPacket(ResourceCost entry) {
        this.food = entry.food;
        this.wood = entry.wood;
        this.ore = entry.ore;
        this.ticks = entry.ticks;
        this.population = entry.population;
        this.id = entry.id;
    }
    public ClientboundSyncResourceCostPacket(RegistryFriendlyByteBuf buf) {
        this.food = buf.readInt();
        this.wood = buf.readInt();
        this.ore = buf.readInt();
        this.ticks = buf.readInt();
        this.population = buf.readInt();
        this.id = buf.readUtf();
    }
    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.getFood());
        buf.writeInt(this.getWood());
        buf.writeInt(this.getOre());
        buf.writeInt(this.getTicks());
        buf.writeInt(this.getPopulation());
        buf.writeUtf(this.getId());
    }
    public static ClientboundSyncResourceCostPacket decode(RegistryFriendlyByteBuf buf) {
        return new ClientboundSyncResourceCostPacket(buf);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        ClientboundSyncResourceCostPacket msg = this;
        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConfigClientEvents.loadConfigData(msg));
        });
    }

    public int getFood() {
        return food;
    }

    public int getWood() {
        return wood;
    }

    public int getOre() {
        return ore;
    }

    public int getTicks() {
        return ticks;
    }

    public int getPopulation() {
        return population;
    }

    public String getId() {
        return id;
    }
}
