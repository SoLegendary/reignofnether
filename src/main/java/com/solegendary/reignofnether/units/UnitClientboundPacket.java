package com.solegendary.reignofnether.units;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
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
                    // this packet is triggered on server mob spawn, but clients may not have the entity available yet
                    // therefore, queue up the ids to assign
                    ArrayList<Integer> unitPair = new ArrayList<>();
                    unitPair.add(this.unitId);
                    unitPair.add(this.controllingPlayerId);
                    UnitClientVanillaEvents.unitsToAssignCtrl.add(unitPair);
                    success.set(true);
                }
            );
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
