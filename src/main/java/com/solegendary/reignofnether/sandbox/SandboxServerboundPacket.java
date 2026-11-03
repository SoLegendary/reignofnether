package com.solegendary.reignofnether.sandbox;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class SandboxServerboundPacket {

    public SandboxAction sandboxAction;
    public String playerName;
    public String unitName;
    public BlockPos blockPos;
    public int[] entityIds;

    public static void spawnUnit(SandboxAction sandboxAction, String playerName, String unitName, BlockPos blockPos) {
        if (!unitName.isBlank())
            PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(sandboxAction, playerName, unitName, blockPos,  new int[]{}));
    }
    public static void setAnchor(BlockPos blockPos, int[] entityIds) {
        PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(SandboxAction.SET_ANCHOR, "", "", blockPos, entityIds));
    }
    public static void resetToAnchor(int[] entityIds) {
        PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(SandboxAction.RESET_TO_ANCHOR, "", "", new BlockPos(0,0,0), entityIds));
    }
    public static void removeAnchor(int[] entityIds) {
        PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(SandboxAction.REMOVE_ANCHOR, "", "", new BlockPos(0,0,0), entityIds));
    }
    public static void setUnitOwner(int[] entityIds, String ownerName) {
        PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(SandboxAction.SET_UNIT_OWNER, ownerName, "", new BlockPos(0,0,0), entityIds));
    }
    public static void setBuildingOwner(BlockPos pos, String ownerName) {
        PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(SandboxAction.SET_BUILDING_OWNER, ownerName, "", pos,  new int[]{}));
    }
    public static void removeBuilding(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new SandboxServerboundPacket(SandboxAction.REMOVE_BUILDING, "", "", pos, new int[]{}));
    }

    public SandboxServerboundPacket(SandboxAction sandboxAction, String playerName, String unitName, BlockPos blockPos, int[] entityIds) {
        this.sandboxAction = sandboxAction;
        this.playerName = playerName;
        this.unitName = unitName;
        this.blockPos = blockPos;
        this.entityIds = entityIds;
    }

    public SandboxServerboundPacket(FriendlyByteBuf buffer) {
        this.sandboxAction = buffer.readEnum(SandboxAction.class);
        this.playerName = buffer.readUtf();
        this.unitName = buffer.readUtf();
        this.blockPos = buffer.readBlockPos();
        this.entityIds = buffer.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.sandboxAction);
        buffer.writeUtf(this.playerName);
        buffer.writeUtf(this.unitName);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeVarIntArray(this.entityIds);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("SandboxServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            else if (!SandboxServer.isSandboxOp(player)) {
                ReignOfNether.LOGGER.warn("SandboxServerboundPacket: Tried to process packet from non-sandbox or non-op player " + player.getName());
                success.set(false);
                return;
            }

            switch (sandboxAction) {
                case SPAWN_UNIT -> SandboxServer.spawnUnit(this.playerName, this.unitName, this.blockPos);
                case SET_ANCHOR -> SandboxServer.setAnchor(this.entityIds, this.blockPos);
                case RESET_TO_ANCHOR -> SandboxServer.resetToAnchor(this.entityIds);
                case REMOVE_ANCHOR -> SandboxServer.removeAnchor(this.entityIds);
                case SET_UNIT_OWNER -> SandboxServer.setUnitOwner(this.entityIds, this.playerName);
                case SET_BUILDING_OWNER -> SandboxServer.setBuildingOwner(this.blockPos, this.playerName);
                case REMOVE_BUILDING -> SandboxServer.removeBuilding(this.blockPos);
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
