package com.solegendary.reignofnether.units;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitServerboundPacket {

    private boolean stopCommand;
    private int unitIdToAttack;
    private int unitIdToFollow;
    private int[] unitIdsToMove;
    private int[] unitIdsToAttackMove;
    private int[] preselectedUnitIds;
    private int[] selectedUnitIds;
    private BlockPos preselectedBlockPos;

    // packet-handler functions
    public UnitServerboundPacket(
            boolean stopCommand,
            int unitIdToAttack,
            int unitIdToFollow,
            int[] unitIdsToMove,
            int[] unitIdsToAttackMove,
            int[] preselectedUnitIds,
            int[] selectedUnitIds,
            BlockPos preselectedBlockPos
    ) {
        this.stopCommand = stopCommand;
        this.unitIdToAttack = unitIdToAttack;
        this.unitIdToFollow = unitIdToFollow;
        this.unitIdsToMove = unitIdsToMove;
        this.unitIdsToAttackMove = unitIdsToAttackMove;
        this.preselectedUnitIds = preselectedUnitIds;
        this.selectedUnitIds = selectedUnitIds;
        this.preselectedBlockPos = preselectedBlockPos;
    }

    public UnitServerboundPacket(FriendlyByteBuf buffer) {
        this.stopCommand = buffer.readBoolean();
        this.unitIdToAttack = buffer.readInt();
        this.unitIdToFollow = buffer.readInt();
        this.unitIdsToMove = buffer.readVarIntArray();
        this.unitIdsToAttackMove = buffer.readVarIntArray();
        this.preselectedUnitIds = buffer.readVarIntArray();
        this.selectedUnitIds = buffer.readVarIntArray();
        this.preselectedBlockPos = buffer.readBlockPos();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(this.stopCommand);
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
            UnitServerVanillaEvents.consumeUnitActionQueues(
                    this.stopCommand,
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
