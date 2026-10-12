package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class StartPosServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<StartPosServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "start_pos_serverbound"));

    @Override
    public CustomPacketPayload.Type<StartPosServerboundPacket> type() {
        return TYPE;
    }

    StartPosAction action;
    BlockPos blockPos;
    Faction faction;
    String playerName;

    public static void reservePos(BlockPos pos, Faction faction, String playerName) {
        PacketHandler.sendToServer(new StartPosServerboundPacket(StartPosAction.RESERVE, pos, faction, playerName));
    }

    public static void unreservePos(BlockPos pos) {
        PacketHandler.sendToServer(new StartPosServerboundPacket(StartPosAction.UNRESERVE, pos, Factions.NONE, ""));
    }

    public static void readyPlayer(String playerName) {
        PacketHandler.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_READY, new BlockPos(0,0,0), Factions.NONE, playerName));
    }

    public static void unreadyPlayer(String playerName) {
        PacketHandler.sendToServer(new StartPosServerboundPacket(StartPosAction.PLAYER_UNREADY, new BlockPos(0,0,0), Factions.NONE, playerName));
    }

    public static void enablePos(BlockPos pos) {
        PacketHandler.sendToServer(new StartPosServerboundPacket(StartPosAction.ENABLE, pos, Factions.NONE, ""));
    }

    public static void disablePos(BlockPos pos) {
        PacketHandler.sendToServer(new StartPosServerboundPacket(StartPosAction.DISABLE, pos, Factions.NONE, ""));
    }

    public StartPosServerboundPacket(StartPosAction action, BlockPos pos, Faction faction, String playerName) {
        this.action = action;
        this.blockPos = pos;
        this.faction = faction;
        this.playerName = playerName;
    }

    public StartPosServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.action = buffer.readEnum(StartPosAction.class);
        this.blockPos = buffer.readBlockPos();
        this.faction = Factions.getFaction(buffer.readResourceLocation());
        this.playerName = buffer.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeResourceLocation(this.faction.key);
        buffer.writeUtf(this.playerName);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Sender was null");
                return;
            }
            else if ((action == StartPosAction.ENABLE || action == StartPosAction.DISABLE) &&
                    !player.hasPermissions(4)) {
                ReignOfNether.LOGGER.warn("GameruleServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                return;
            }

            switch (action) {
                case RESERVE -> {
                    if (StartPosServerEvents.isStartingGame())
                        return;
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos) && startPos.enabled) {
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
                    if (StartPosServerEvents.isStartingGame())
                        return;
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
        });
        return;
    }
}