package com.solegendary.reignofnether.unit.packets;

import com.solegendary.reignofnether.alliance.Share;
import com.solegendary.reignofnether.alliance.Sharing;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class UnitActionServerboundPacket {

    private static final int MAX_UNIT_IDS = 2048;

    private final String ownerName; // player that is issuing this command
    private final UnitAction action;
    private final int unitId;
    private final int[] unitIds; // units to be controlled
    private final BlockPos preselectedBlockPos;
    private final BlockPos selectedBuildingPos; // for building abilities
    private final boolean shiftQueue; // shift queue actions
    private final boolean groupMove; // ctrl was held: match speeds of the moving group

    // packet-handler functions
    public UnitActionServerboundPacket(
        String ownerName,
        UnitAction action,
        int unitId,
        int[] unitIds,
        BlockPos preselectedBlockPos,
        BlockPos selectedBuildingPos,
        boolean shiftQueue
    ) {
        this(ownerName, action, unitId, unitIds, preselectedBlockPos, selectedBuildingPos, shiftQueue, false);
    }

    public UnitActionServerboundPacket(
        String ownerName,
        UnitAction action,
        int unitId,
        int[] unitIds,
        BlockPos preselectedBlockPos,
        BlockPos selectedBuildingPos,
        boolean shiftQueue,
        boolean groupMove
    ) {
        this.ownerName = ownerName;
        this.action = action;
        this.unitId = unitId;
        this.unitIds = unitIds;
        this.preselectedBlockPos = preselectedBlockPos;
        this.selectedBuildingPos = selectedBuildingPos;
        this.shiftQueue = shiftQueue;
        this.groupMove = groupMove;
    }

    public UnitActionServerboundPacket(FriendlyByteBuf buffer) {
        this.ownerName = buffer.readUtf();
        this.action = buffer.readEnum(UnitAction.class);
        this.unitId = buffer.readInt();
        this.unitIds = buffer.readVarIntArray(MAX_UNIT_IDS);
        this.preselectedBlockPos = buffer.readBlockPos();
        this.selectedBuildingPos = buffer.readBlockPos();
        this.shiftQueue = buffer.readBoolean();
        this.groupMove = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.ownerName);
        buffer.writeEnum(this.action);
        buffer.writeInt(this.unitId);
        buffer.writeVarIntArray(this.unitIds);
        buffer.writeBlockPos(this.preselectedBlockPos);
        buffer.writeBlockPos(this.selectedBuildingPos);
        buffer.writeBoolean(this.shiftQueue);
        buffer.writeBoolean(this.groupMove);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            if (this.action == UnitAction.DEBUG1) {
                UnitServerEvents.debug1(this.preselectedBlockPos);
            }
            if (this.action == UnitAction.DEBUG2) {
                UnitServerEvents.debug2();
            }

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("Sender for unit action packet was null");
                success.set(false);
            }
            else if (!player.getName().getString().equals(ownerName) &&
                    !SandboxServer.isSandboxOp(player) &&
                    !Sharing.shares(ownerName, player.getName().getString(), Share.CONTROL)) {
                ReignOfNether.LOGGER.warn("UnitActionServerboundPacket: Tried to process packet from " + player.getName() + " for " + ownerName);
                success.set(false);
            }
            else {
                //ReignOfNether.LOGGER.info("[UnitAction] {} issued {} (unitIds: {})", player.getName(), this.action, java.util.Arrays.toString(this.unitIds));
                UnitServerEvents.addActionItem(
                        this.ownerName,
                        this.action,
                        this.unitId,
                        this.unitIds,
                        this.preselectedBlockPos,
                        this.selectedBuildingPos,
                        this.shiftQueue,
                        this.groupMove
                );
                success.set(true);
            }
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
