package com.solegendary.reignofnether.unit;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitServerboundPacket {

    private final UnitAction action;
    private final int unitId;
    private final int[] unitIds;
    private final BlockPos preselectedBlockPos;

    // packet-handler functions
    public UnitServerboundPacket(
        UnitAction action,
        int unitId,
        int[] unitIds,
        BlockPos preselectedBlockPos
    ) {
        // filter out non-owned entities so we can't control them
        this.action = action;
        this.unitId = unitId;
        this.unitIds = Arrays.stream(unitIds).filter(
                (int id) -> UnitClientEvents.getPlayerToEntityRelationship(id) == Relationship.OWNED
        ).toArray();
        this.preselectedBlockPos = preselectedBlockPos;
    }

    public UnitServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(UnitAction.class);
        this.unitId = buffer.readInt();
        this.unitIds = buffer.readVarIntArray();
        this.preselectedBlockPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeVarIntArray(this.unitIds);
        buffer.writeBlockPos(this.preselectedBlockPos);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            UnitServerEvents.addActionItem(
                this.action,
                this.unitId,
                this.unitIds,
                this.preselectedBlockPos
            );
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
