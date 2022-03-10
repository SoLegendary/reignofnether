package com.solegendary.reignofnether.units;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitClientboundPacket {

    private int unitId;
    private UUID ownerUUID;

    // packet-handler functions
    public UnitClientboundPacket(
            int unitId,
            UUID ownerUUID
    ) {
        this.unitId = unitId;
        this.ownerUUID = ownerUUID;
    }

    public UnitClientboundPacket(FriendlyByteBuf buffer) {
        this.unitId = buffer.readInt();
        this.ownerUUID = buffer.readUUID();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.unitId);
        buffer.writeUUID(this.ownerUUID);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    // this packet is triggered on server mob spawn, but clients may not have the entity available yet
                    // therefore, queue up the ids to assign
                    UnitClientVanillaEvents.unitsToAssignCtrl.add(unitId);
                    UnitClientVanillaEvents.playerUUIDs.add(ownerUUID);
                    success.set(true);
                }
            );
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
