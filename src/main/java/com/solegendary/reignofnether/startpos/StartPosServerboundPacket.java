package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class StartPosServerboundPacket {

    StartPosAction action;
    BlockPos blockPos;
    Faction faction;
    String playerName;

    public static void reservePos(BlockPos pos, Faction faction, String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.RESERVE, pos, faction, playerName));
    }

    public static void unreservePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.UNRESERVE, pos, Faction.NONE, ""));
    }

    public static void toggleReady(String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.TOGGLE_READY, BlockPos.ZERO, Faction.NONE, playerName));
    }

    public StartPosServerboundPacket(StartPosAction action, BlockPos pos, Faction faction, String playerName) {
        this.action = action;
        this.blockPos = pos;
        this.faction = faction;
        this.playerName = playerName;
    }

    public StartPosServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(StartPosAction.class);
        this.blockPos = buffer.readBlockPos();
        this.faction = buffer.readEnum(Faction.class);
        this.playerName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeEnum(this.faction);
        buffer.writeUtf(this.playerName);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ReignOfNether.LOGGER.info("[StartPos] {} position: action={}, pos={}, faction={}, playerName={}", action, blockPos, faction, playerName);
            switch (action) {
                case RESERVE -> {
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos)) {
                            startPos.faction = faction;
                            startPos.playerName = playerName;
                            // Clear ready state when reserving/re-reserving
                            startPos.ready = false;
                            StartPosClientboundPacket.reservePos(blockPos, faction, playerName);
                            // If a ready-check was active and someone changes position, cancel it
                            if (StartPosServerEvents.isReadyCheckActive()) {
                                StartPosServerEvents.cancelStartGameCountdown(false);
                            }
                            break;
                        }
                    }
                }
                case UNRESERVE -> {
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos)) {
                            startPos.faction = faction;
                            startPos.ready = false;
                            StartPosClientboundPacket.unreservePos(blockPos);
                            // If a ready-check was active and someone un-reserves, cancel it
                            if (StartPosServerEvents.isReadyCheckActive()) {
                                StartPosServerEvents.cancelStartGameCountdown(false);
                            }
                            break;
                        }
                    }
                }
                case TOGGLE_READY -> {
                    StartPosServerEvents.toggleReady(playerName);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}