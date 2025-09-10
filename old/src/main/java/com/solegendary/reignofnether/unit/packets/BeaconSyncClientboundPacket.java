package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// allow the server to force unit actions as though it was sent by the client so it is recorded on both sides
public class BeaconSyncClientboundPacket {

    private final UnitAction action;
    private final BlockPos beaconPos;
    private final boolean activate;

    public static void syncBeacon(UnitAction action, BlockPos beaconPos, boolean activate) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
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

    public BeaconSyncClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(UnitAction.class);
        this.beaconPos = buffer.readBlockPos();
        this.activate = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.beaconPos);
        buffer.writeBoolean(this.activate);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            BuildingClientEvents.syncBeacon(
                this.action,
                this.beaconPos,
                this.activate
            );
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
