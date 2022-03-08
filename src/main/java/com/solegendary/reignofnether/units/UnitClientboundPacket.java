package com.solegendary.reignofnether.units;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitClientboundPacket {

    private int unitId;
    private int controllingPlayerId;

    // packet-handler functions
    public UnitClientboundPacket(
            int unitId,
            int controllingPlayerId
    ) {
        this.unitId = unitId;
        this.controllingPlayerId = controllingPlayerId;
    }

    public UnitClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.controllingPlayerId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeInt(this.controllingPlayerId);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    System.out.println("Received packet from server:");
                    System.out.println(this.unitId);
                    System.out.println(this.controllingPlayerId);

                    Entity entity = Minecraft.getInstance().level.getEntity(this.unitId);
                    if (entity != null)
                        ((Unit) entity).setControllingPlayerId(this.controllingPlayerId);
                    success.set(true);
                }
            );
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
