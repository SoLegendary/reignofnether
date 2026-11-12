package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitSyncWorkerClientBoundPacket {

    private final int entityId;
    private final boolean isBuilding; // for workers to show arms swinging
    private final boolean isGathering; // server-authoritative in-range gathering state (client can't compute it)
    private final ResourceName gatherName; // for workers to show arms swinging and have the right tool
    private final BlockPos gatherPos;
    private final int gatherTicks;
    private final BlockPos farmPos; // origin of the farm this worker is assigned to (0,0,0 if none)

    public static void sendSyncWorkerPacket(LivingEntity entity) {
        if (entity instanceof WorkerUnit workerUnit) {
            BlockPos bp = workerUnit.getGatherResourceGoal().getGatherTarget();
            BuildingPlacement farm = workerUnit.getGatherResourceGoal().getTargetFarm();

            FogOfWarServerEvents.sendToPlayersWhoCanSee(entity,
                new UnitSyncWorkerClientBoundPacket(entity.getId(),
                    workerUnit.getBuildRepairGoal().isBuilding(),
                    workerUnit.getGatherResourceGoal().isGathering(),
                    workerUnit.getGatherResourceGoal().getTargetResourceName(),
                    bp == null ? new BlockPos(0,0,0) : bp,
                    workerUnit.getGatherResourceGoal().getGatherTicksLeft(),
                    farm == null ? new BlockPos(0,0,0) : farm.originPos)
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
        int gatherTicks,
        BlockPos farmPos
    ) {
        // filter out non-owned entities so we can't control them
        this.entityId = unitId;
        this.isBuilding = isBuilding;
        this.isGathering = isGathering;
        this.gatherName = gatherName;
        this.gatherPos = gatherPos;
        this.gatherTicks = gatherTicks;
        this.farmPos = farmPos;
    }

    public UnitSyncWorkerClientBoundPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.isBuilding = buffer.readBoolean();
        this.isGathering = buffer.readBoolean();
        this.gatherName = buffer.readEnum(ResourceName.class);
        this.gatherPos = buffer.readBlockPos();
        this.gatherTicks = buffer.readInt();
        this.farmPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.isBuilding);
        buffer.writeBoolean(this.isGathering);
        buffer.writeEnum(this.gatherName);
        buffer.writeBlockPos(this.gatherPos);
        buffer.writeInt(this.gatherTicks);
        buffer.writeBlockPos(this.farmPos);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    UnitClientEvents.syncWorkerUnit(
                        this.entityId,
                        this.isBuilding,
                        this.isGathering,
                        this.gatherName,
                        this.gatherPos.equals(new BlockPos(0,0,0)) ? null : this.gatherPos,
                        this.gatherTicks,
                        this.farmPos.equals(new BlockPos(0,0,0)) ? null : this.farmPos);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
