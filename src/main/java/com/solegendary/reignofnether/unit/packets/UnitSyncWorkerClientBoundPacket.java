package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitSyncWorkerClientBoundPacket {

    private final int entityId;
    private final boolean isBuilding; // for workers to show arms swinging
    private final boolean isGathering; // server-authoritative in-range gathering state (client can't compute it)
    private final ResourceName gatherName; // for workers to show arms swinging and have the right tool
    private final BlockPos gatherPos;
    private final int gatherTicks;
    @Nullable private final int[] villagerExp; // farmer, lumberjack, miner, mason, hunter; null for other workers

    public static void sendSyncWorkerPacket(LivingEntity entity) {
        if (entity instanceof WorkerUnit workerUnit) {
            BlockPos bp = workerUnit.getGatherResourceGoal().getGatherTarget();

            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new UnitSyncWorkerClientBoundPacket(entity.getId(),
                    workerUnit.getBuildRepairGoal().isBuilding(),
                    workerUnit.getGatherResourceGoal().isGathering(),
                    workerUnit.getGatherResourceGoal().getTargetResourceName(),
                    bp == null ? new BlockPos(0,0,0) : bp,
                    workerUnit.getGatherResourceGoal().getGatherTicksLeft(),
                    entity instanceof VillagerUnit v ? new int[] { v.farmerExp, v.lumberjackExp, v.minerExp, v.masonExp, v.hunterExp } : null)
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
        @Nullable int[] villagerExp
    ) {
        // filter out non-owned entities so we can't control them
        this.entityId = unitId;
        this.isBuilding = isBuilding;
        this.isGathering = isGathering;
        this.gatherName = gatherName;
        this.gatherPos = gatherPos;
        this.gatherTicks = gatherTicks;
        this.villagerExp = villagerExp;
    }

    public UnitSyncWorkerClientBoundPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.isBuilding = buffer.readBoolean();
        this.isGathering = buffer.readBoolean();
        this.gatherName = buffer.readEnum(ResourceName.class);
        this.gatherPos = buffer.readBlockPos();
        this.gatherTicks = buffer.readInt();
        // appended last and optional, so jars from before this field still read each other's packets
        if (buffer.readableBytes() >= 5 * 4) {
            this.villagerExp = new int[5];
            for (int i = 0; i < 5; i++)
                this.villagerExp[i] = buffer.readInt();
        } else {
            this.villagerExp = null;
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.entityId);
        buffer.writeBoolean(this.isBuilding);
        buffer.writeBoolean(this.isGathering);
        buffer.writeEnum(this.gatherName);
        buffer.writeBlockPos(this.gatherPos);
        buffer.writeInt(this.gatherTicks);
        if (this.villagerExp != null)
            for (int exp : this.villagerExp)
                buffer.writeInt(exp);
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
                        this.gatherTicks);
                    if (this.villagerExp != null)
                        UnitClientEvents.syncVillagerExp(this.entityId, this.villagerExp);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
