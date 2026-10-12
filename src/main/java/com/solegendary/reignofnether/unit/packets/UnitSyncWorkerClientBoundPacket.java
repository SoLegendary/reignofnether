package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class UnitSyncWorkerClientBoundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<UnitSyncWorkerClientBoundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "unit_sync_worker_client_bound"));

    @Override
    public CustomPacketPayload.Type<UnitSyncWorkerClientBoundPacket> type() {
        return TYPE;
    }

    private final int entityId;
    private final boolean isBuilding; // for workers to show arms swinging
    private final boolean isGathering; // server-authoritative in-range gathering state (client can't compute it)
    private final ResourceName gatherName; // for workers to show arms swinging and have the right tool
    private final BlockPos gatherPos;
    private final int gatherTicks;

    public static void sendSyncWorkerPacket(LivingEntity entity) {
        if (entity instanceof WorkerUnit workerUnit) {
            BlockPos bp = workerUnit.getGatherResourceGoal().getGatherTarget();

            PacketHandler.send(PacketHandler.allPlayers(),
                new UnitSyncWorkerClientBoundPacket(entity.getId(),
                    workerUnit.getBuildRepairGoal().isBuilding(),
                    workerUnit.getGatherResourceGoal().isGathering(),
                    workerUnit.getGatherResourceGoal().getTargetResourceName(),
                    bp == null ? new BlockPos(0,0,0) : bp,
                    workerUnit.getGatherResourceGoal().getGatherTicksLeft())
            );
        }
    }

    // packet-handler functions
    public UnitSyncWorkerClientBoundPacket(
        int unitId,
        boolean isBuilding,
        boolean isGathering,
        ResourceName gatherName,
        BlockPos gatherPos,
        int gatherTicks
    ) {
        // filter out non-owned entities so we can't control them
        this.entityId = unitId;
        this.isBuilding = isBuilding;
        this.isGathering = isGathering;
        this.gatherName = gatherName;
        this.gatherPos = gatherPos;
        this.gatherTicks = gatherTicks;
    }

    public UnitSyncWorkerClientBoundPacket(RegistryFriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.isBuilding = buffer.readBoolean();
        this.isGathering = buffer.readBoolean();
        this.gatherName = buffer.readEnum(ResourceName.class);
        this.gatherPos = buffer.readBlockPos();
        this.gatherTicks = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.isBuilding);
        buffer.writeBoolean(this.isGathering);
        buffer.writeEnum(this.gatherName);
        buffer.writeBlockPos(this.gatherPos);
        buffer.writeInt(this.gatherTicks);
    }

    // client-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    UnitClientEvents.syncWorkerUnit(
                        this.entityId,
                        this.isBuilding,
                        this.isGathering,
                        this.gatherName,
                        this.gatherPos.equals(new BlockPos(0,0,0)) ? null : this.gatherPos,
                        this.gatherTicks);
                });
        });
        return;
    }
}
