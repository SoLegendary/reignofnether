package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.hud.ActionName;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitServerboundPacket {

    private final ActionName specialAction;
    private final int unitIdToAttack;
    private final int unitIdToFollow;
    private final int[] unitIdsToMove;
    private final int[] unitIdsToAttackMove;
    private final int[] preselectedUnitIds;
    private final int[] selectedUnitIds;
    private final BlockPos preselectedBlockPos;

    // packet-handler functions
    public UnitServerboundPacket(
        ActionName specialAction,
        int unitIdToAttack,
        int unitIdToFollow,
        int[] unitIdsToMove,
        int[] unitIdsToAttackMove,
        int[] preselectedUnitIds,
        int[] selectedUnitIds,
        BlockPos preselectedBlockPos
    ) {
        // filter out non-owned entities so we can't control them
        this.specialAction = specialAction;
        this.unitIdToAttack = unitIdToAttack;
        this.unitIdToFollow = unitIdToFollow;
        this.unitIdsToMove = Arrays.stream(unitIdsToMove).filter(
                (int id) -> UnitClientEvents.getPlayerToEntityRelationship(id) == Relationship.OWNED
        ).toArray();
        this.unitIdsToAttackMove = Arrays.stream(unitIdsToAttackMove).filter(
                (int id) -> UnitClientEvents.getPlayerToEntityRelationship(id) == Relationship.OWNED
        ).toArray();
        this.preselectedUnitIds = preselectedUnitIds;
        this.selectedUnitIds = Arrays.stream(selectedUnitIds).filter(
            (int id) -> UnitClientEvents.getPlayerToEntityRelationship(id) == Relationship.OWNED
        ).toArray();
        this.preselectedBlockPos = preselectedBlockPos;
    }

    public UnitServerboundPacket(FriendlyByteBuf buffer) {
        this.specialAction = buffer.readEnum(ActionName.class);
        this.unitIdToAttack = buffer.readInt();
        this.unitIdToFollow = buffer.readInt();
        this.unitIdsToMove = buffer.readVarIntArray();
        this.unitIdsToAttackMove = buffer.readVarIntArray();
        this.preselectedUnitIds = buffer.readVarIntArray();
        this.selectedUnitIds = buffer.readVarIntArray();
        this.preselectedBlockPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.specialAction);
        buffer.writeInt(this.unitIdToAttack);
        buffer.writeInt(this.unitIdToFollow);
        buffer.writeVarIntArray(this.unitIdsToMove);
        buffer.writeVarIntArray(this.unitIdsToAttackMove);
        buffer.writeVarIntArray(this.preselectedUnitIds);
        buffer.writeVarIntArray(this.selectedUnitIds);
        buffer.writeBlockPos(this.preselectedBlockPos);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            UnitServerEvents.consumeUnitActionQueues(
                this.specialAction,
                this.unitIdToAttack,
                this.unitIdToFollow,
                this.unitIdsToMove,
                this.unitIdsToAttackMove,
                this.preselectedUnitIds,
                this.selectedUnitIds,
                this.preselectedBlockPos
            );
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
