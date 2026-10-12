package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.util.ArrayUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.Supplier;

// send a list of worker unit ids that are idle at this point in time
public class UnitIdleWorkerClientBoundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitIdleWorkerClientBoundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_idle_worker_client_bound"));

    @Override
    public CustomPacketPayload.Type<UnitIdleWorkerClientBoundPacket> type() {
        return TYPE;
    }

    private final int[] oldUnitIds; // units to be controlled

    public static void sendIdleWorkerPacket() {
        var units = new LinkedList<Integer>();
        for (LivingEntity livingEntity : UnitServerEvents.getAllUnits()) {
            if (livingEntity instanceof WorkerUnit wu && WorkerUnit.isIdle(wu)) units.add(livingEntity.getId());
        }
        PacketHandler.send(PacketHandler.allPlayers(),
            new UnitIdleWorkerClientBoundPacket(ArrayUtil.intListToArray(units)));
    }

    // packet-handler functions
    public UnitIdleWorkerClientBoundPacket(
            int[] oldUnitIds
    ) {
        this.oldUnitIds = oldUnitIds;
    }

    public UnitIdleWorkerClientBoundPacket(RegistryFriendlyByteBuf buffer) {
        this.oldUnitIds = buffer.readVarIntArray();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarIntArray(this.oldUnitIds);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            UnitClientEvents.syncIdleWorkers(oldUnitIds);
        });
        return;
    }
}
