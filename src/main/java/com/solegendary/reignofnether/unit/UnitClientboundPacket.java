package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.building.BuildingAction;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.hud.HudClientEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.solegendary.reignofnether.building.BuildingUtils.findBuilding;

public class UnitClientboundPacket {

    private final UnitSyncAction action;
    private final int unitId;
    private final BlockPos pos;

    // packet-handler functions
    public UnitClientboundPacket(
        UnitSyncAction action,
        int unitId,
        BlockPos pos
    ) {
        // filter out non-owned entities so we can't control them
        this.action = action;
        this.unitId = unitId;
        this.pos = pos;
    }

    public UnitClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(UnitSyncAction.class);
        this.unitId = buffer.readInt();
        this.pos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeBlockPos(this.pos);
    }

    // client-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> {
                    if (this.action == UnitSyncAction.LEAVE_LEVEL)
                        UnitClientEvents.onEntityLeave(this.unitId);
                });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
