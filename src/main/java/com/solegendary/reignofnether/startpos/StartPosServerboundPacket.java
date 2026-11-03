package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class StartPosServerboundPacket {

    StartPosAction action;
    BlockPos blockPos;
    Faction faction;

    public static void reservePos(BlockPos pos, Faction faction) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.RESERVE, pos, faction));
    }

    public static void unreservePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.UNRESERVE, pos, Factions.NONE));
    }

    public static void readyPlayer() {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_READY, BlockPos.ZERO, Factions.NONE));
    }

    public static void unreadyPlayer() {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_UNREADY, BlockPos.ZERO, Factions.NONE));
    }

    public static void enablePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.ENABLE, pos, Factions.NONE));
    }

    public static void disablePos(BlockPos pos) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.DISABLE, pos, Factions.NONE));
    }

    public StartPosServerboundPacket(StartPosAction action, BlockPos pos, Faction faction) {
        this.action = action;
        this.blockPos = pos;
        this.faction = faction;
    }

    public StartPosServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(StartPosAction.class);
        this.blockPos = buffer.readBlockPos();
        this.faction = Factions.getFaction(buffer.readResourceLocation());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeResourceLocation(this.faction.key);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("StartPosServerboundPacket: Sender was null");
                success.set(false);
                return;
            }
            else if ((action == StartPosAction.ENABLE || action == StartPosAction.DISABLE) &&
                    !player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("StartPosServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                success.set(false);
                return;
            }

            // always acts as the sender
            String playerName = player.getName().getString();
            switch (action) {
                case RESERVE -> {
                    if (StartPosServerEvents.isStartingGame())
                        return;
                    // never take someone else's seat (with hideEnemyPicks an enemy seat looks empty on the map)
                    if (StartPosServerEvents.startPoses.stream().anyMatch(sp -> sp.pos.equals(blockPos) &&
                            !sp.playerName.isBlank() && !sp.playerName.equals(playerName)))
                        return;
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos) && startPos.enabled) {
                            startPos.reset();
                            startPos.faction = faction;
                            startPos.playerName = playerName;
                        } else if (startPos.playerName.equals(playerName)) {
                            startPos.reset();
                        }
                    }
                    StartPosServerEvents.syncAll();
                    StartPosServerEvents.setPlayerReady(playerName, false);
                }
                case UNRESERVE -> {
                    if (StartPosServerEvents.isStartingGame())
                        return;
                    // only your own seat
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos) && startPos.playerName.equals(playerName)) {
                            startPos.reset();
                            StartPosServerEvents.syncAll();
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