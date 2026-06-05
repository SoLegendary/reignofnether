package com.solegendary.reignofnether.startpos;

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

    public static void readyPlayer(String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_READY, new BlockPos(0,0,0), Faction.NONE, playerName));
    }

    public static void unreadyPlayer(String playerName) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_UNREADY, new BlockPos(0,0,0), Faction.NONE, playerName));
    }

    public static void enablePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.ENABLE, pos, Faction.NONE, ""));
    }

    public static void disablePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.DISABLE, pos, Faction.NONE, ""));
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
            switch (action) {
                case RESERVE -> {
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos)) {
                            startPos.reset();
                            startPos.faction = faction;
                            startPos.playerName = playerName;
                            StartPosClientboundPacket.reservePos(blockPos, faction, playerName);
                        } else if (startPos.playerName.equals(playerName)) {
                            startPos.reset();
                        }
                    }
                    StartPosServerEvents.setPlayerReady(playerName, false);
                }
                case UNRESERVE -> {
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos)) {
                            startPos.reset();
                            StartPosClientboundPacket.unreservePos(blockPos);
                            break;
                        }
                    }
                    StartPosServerEvents.setPlayerReady(playerName, false);
                }
                case PLAYER_READY -> StartPosServerEvents.setPlayerReady(playerName, true);
                case PLAYER_UNREADY -> StartPosServerEvents.setPlayerReady(playerName, false);
                case ENABLE -> StartPosServerEvents.setPosEnabled(blockPos, true);
                case DISABLE -> StartPosServerEvents.setPosEnabled(blockPos, false);
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}