package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.resources.ResourceCost;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/*
    Clientbound packet to synchronize serverside config options with the client
    so that the GUI and other elements can properly reflect the values present on the server.
 */
public class ClientboundSyncResourceCostPacket {
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
    public ClientboundSyncResourceCostPacket(FriendlyByteBuf buf) {
        this.food = buf.readInt();
        this.wood = buf.readInt();
        this.ore = buf.readInt();
        this.ticks = buf.readInt();
        this.population = buf.readInt();
        this.id = buf.readUtf();
    }
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.getFood());
        buf.writeInt(this.getWood());
        buf.writeInt(this.getOre());
        buf.writeInt(this.getTicks());
        buf.writeInt(this.getPopulation());
        buf.writeUtf(this.getId());
    }
    public static ClientboundSyncResourceCostPacket decode(FriendlyByteBuf buf) {
        return new ClientboundSyncResourceCostPacket(buf);
    }

    public static boolean handle(ClientboundSyncResourceCostPacket msg, Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ConfigClientEvents.loadConfigData(msg, ctx));
            success.set(true);
        });
        context.setPacketHandled(true);
        return success.get();
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
