package com.solegendary.reignofnether.resources;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ResourcesClientboundPacket {

    // pos is used to identify the building object serverside
    public String ownerName;
    public int wood;
    public int food;
    public int ore;

    public static void syncServerResources(ArrayList<Resources> resourcesList) {
        for (Resources resources : resourcesList)
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new ResourcesClientboundPacket(resources.ownerName, resources.wood, resources.food, resources.ore));
    }

    public ResourcesClientboundPacket(String ownerName, int wood, int food, int ore) {
        this.ownerName = ownerName;
        this.wood = wood;
        this.food = food;
        this.ore = ore;
    }

    public ResourcesClientboundPacket(FriendlyByteBuf buffer) {
        this.ownerName = buffer.readUtf();
        this.wood = buffer.readInt();
        this.food = buffer.readInt();
        this.ore = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.ownerName);
        buffer.writeInt(this.wood);
        buffer.writeInt(this.food);
        buffer.writeInt(this.ore);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        ResourcesClientEvents.syncServerResources(new Resources(
                                this.ownerName,
                                this.wood,
                                this.food,
                                this.ore
                        ));
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
