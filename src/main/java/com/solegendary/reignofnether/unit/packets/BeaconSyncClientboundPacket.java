package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

// allow the server to force unit actions as though it was sent by the client so it is recorded on both sides
public class BeaconSyncClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<BeaconSyncClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "beacon_sync_clientbound"));

    @Override
    public CustomPacketPayload.Type<BeaconSyncClientboundPacket> type() {
        return TYPE;
    }

    private final UnitAction action;
    private final BlockPos beaconPos;
    private final boolean activate;

    public static void syncBeacon(UnitAction action, BlockPos beaconPos, boolean activate) {
        PacketHandler.send(PacketHandler.allPlayers(),
            new BeaconSyncClientboundPacket(action, beaconPos, activate));
    }

    // packet-handler functions
    public BeaconSyncClientboundPacket(
        UnitAction action,
        BlockPos beaconPos,
        boolean activate
    ) {
        this.action = action;
        this.beaconPos = beaconPos;
        this.activate = activate;
    }

    public BeaconSyncClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(UnitAction.class);
        this.beaconPos = buffer.readBlockPos();
        this.activate = buffer.readBoolean();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.beaconPos);
        buffer.writeBoolean(this.activate);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BuildingClientEvents.syncBeacon(
                this.action,
                this.beaconPos,
                this.activate
            );
        });
        return;
    }
}
