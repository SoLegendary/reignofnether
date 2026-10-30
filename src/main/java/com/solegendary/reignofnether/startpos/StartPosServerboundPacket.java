package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.function.Supplier;

public class StartPosServerboundPacket {

    StartPosAction action;
    BlockPos blockPos;
    Faction faction;
    int botDifficulty = -1;
    String joinName = ""; // ACCEPT_JOIN, REFUSE_JOIN: the player being answered

    public static void reservePos(BlockPos pos, Faction faction) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.RESERVE, pos, faction));
    }

    // host only: an AI bot on this seat with this faction and difficulty (BotDifficulty ordinal)
    public static void reserveBotPos(BlockPos pos, Faction faction, int botDifficulty) {
        StartPosServerboundPacket packet = new StartPosServerboundPacket(StartPosAction.RESERVE_BOT, pos, faction);
        packet.botDifficulty = botDifficulty;
        PacketHandler.INSTANCE.sendToServer(packet);
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

    public static void joinAsSoldier(BlockPos teamSeat) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.JOIN_SOLDIER, teamSeat, Factions.NONE));
    }

    public static void joinAsCoCommander(BlockPos seat) {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.JOIN_CO_COMMANDER, seat, Factions.NONE));
    }

    public static void leaveJoin() {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.LEAVE_JOIN, BlockPos.ZERO, Factions.NONE));
    }

    public static void answerJoin(String playerName, boolean accept) {
        StartPosServerboundPacket packet = new StartPosServerboundPacket(accept ? StartPosAction.ACCEPT_JOIN : StartPosAction.REFUSE_JOIN, BlockPos.ZERO, Factions.NONE);
        packet.joinName = playerName;
        PacketHandler.INSTANCE.sendToServer(packet);
    }

    public static void closeEmptyPositions() {
        PacketHandler.INSTANCE.sendToServer(new StartPosServerboundPacket(StartPosAction.CLOSE_EMPTY, BlockPos.ZERO, Factions.NONE));
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
        this.botDifficulty = buffer.readInt();
        this.joinName = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeResourceLocation(this.faction.key);
        buffer.writeInt(this.botDifficulty);
        buffer.writeUtf(this.joinName);
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
            else if ((action == StartPosAction.ENABLE || action == StartPosAction.DISABLE || action == StartPosAction.CLOSE_EMPTY ||
                    action == StartPosAction.RESERVE_BOT) &&
                    !StartPosServerEvents.isHost(player)) {
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
                    StartPosServerEvents.joins.removeIf(j -> j.playerName.equals(playerName));
                    StartPosServerEvents.trimJoins(); // co-commanders of a seat this player left go with it
                    StartPosServerEvents.setPlayerReady(playerName, false);
                }
                case UNRESERVE -> {
                    // during the countdown only the host taking out a bot (it readies by itself); that stops it
                    boolean botSeat = StartPosServerEvents.startPoses.stream().anyMatch(sp -> sp.pos.equals(blockPos) && sp.isBot());
                    if (StartPosServerEvents.isStartingGame() && !(botSeat && StartPosServerEvents.isHost(player)))
                        return;
                    // the host can also clear someone else's seat (kick); unready whoever sat there
                    for (StartPos startPos : StartPosServerEvents.startPoses) {
                        if (startPos.pos.equals(blockPos) &&
                                (startPos.playerName.equals(playerName) || StartPosServerEvents.isHost(player))) {
                            String seated = startPos.playerName;
                            startPos.reset();
                            StartPosServerEvents.trimJoins();
                            if (!seated.isBlank())
                                StartPosServerEvents.setPlayerReady(seated, false);
                            break;
                        }
                    }
                }
                case RESERVE_BOT -> StartPosServerEvents.reserveBotPos(blockPos, faction, botDifficulty);
                case JOIN_SOLDIER -> StartPosServerEvents.join(playerName, PlayerRole.SOLDIER, blockPos);
                case JOIN_CO_COMMANDER -> StartPosServerEvents.join(playerName, PlayerRole.COMMANDER, blockPos);
                case LEAVE_JOIN -> {
                    if (!StartPosServerEvents.isStartingGame())
                        StartPosServerEvents.leaveJoin(playerName);
                }
                case ACCEPT_JOIN -> StartPosServerEvents.answerJoin(player, joinName, true);
                case REFUSE_JOIN -> StartPosServerEvents.answerJoin(player, joinName, false);
                case PLAYER_READY -> StartPosServerEvents.setPlayerReady(playerName, true);
                case PLAYER_UNREADY -> StartPosServerEvents.setPlayerReady(playerName, false);
                case ENABLE -> StartPosServerEvents.setPosEnabled(blockPos, true);
                case DISABLE -> StartPosServerEvents.setPosEnabled(blockPos, false);
                case CLOSE_EMPTY -> {
                    for (StartPos startPos : List.copyOf(StartPosServerEvents.startPoses))
                        if (startPos.enabled && startPos.playerName.isBlank())
                            StartPosServerEvents.setPosEnabled(startPos.pos, false);
                }
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}