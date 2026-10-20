package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.PacketHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class StartPosClientboundPacket {

    StartPosAction action;
    Faction faction;
    BlockPos blockPos;
    String playerName;
    int colorId;
    boolean ready = false;

    // seats are sent per player: with hideEnemyPicks each player only sees their own team's picks
    public static void sendTo(ServerPlayer player, StartPosAction action, StartPos sp) {
        StartPosClientboundPacket packet = new StartPosClientboundPacket(action, sp.pos, sp.faction, sp.playerName, sp.colorId);
        packet.ready = sp.ready;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // only who and whether they're ready: no position, faction or team
    public static void sendHiddenPlayer(ServerPlayer player, String playerName, boolean ready) {
        StartPosClientboundPacket packet = new StartPosClientboundPacket(StartPosAction.HIDDEN_PLAYER, BlockPos.ZERO, Factions.NONE, playerName, 0);
        packet.ready = ready;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void clear(ServerPlayer player) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new StartPosClientboundPacket(StartPosAction.CLEAR, new BlockPos(0,0,0), Factions.NONE, "", 0));
    }

    public static void removePos(BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.REMOVE, pos, Factions.NONE, "", 0));
    }

    public static void reset() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.RESET, new BlockPos(0,0,0), Factions.NONE, "", 0));
    }

    public static void startGameCountdown() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.SET_GAME_STARTING, new BlockPos(0,0,0), Factions.NONE, "", 0));
    }

    public static void cancelStartGameCountdown() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.UNSET_GAME_STARTING, new BlockPos(0,0,0), Factions.NONE, "", 0));
    }

    public static void readyPlayer(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.PLAYER_READY, new BlockPos(0,0,0), Factions.NONE, playerName, 0));
    }

    public static void unreadyPlayer(String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.PLAYER_UNREADY, new BlockPos(0,0,0), Factions.NONE, playerName, 0));
    }

    public StartPosClientboundPacket(StartPosAction action, BlockPos blockPos, Faction faction, String playerName, int colorId) {
        this.action = action;
        this.blockPos = blockPos;
        this.faction = faction;
        this.playerName = playerName;
        this.colorId = colorId;
    }

    public StartPosClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(StartPosAction.class);
        this.blockPos = buffer.readBlockPos();
        this.faction = Factions.getFaction(buffer.readResourceLocation());
        this.playerName = buffer.readUtf();
        this.colorId = buffer.readInt();
        this.ready = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeResourceLocation(this.faction.key);
        buffer.writeUtf(this.playerName);
        buffer.writeInt(this.colorId);
        buffer.writeBoolean(this.ready);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case ADD, ADD_DISABLED -> {
                                StartPosClientEvents.startPoses.removeIf(sp -> sp.pos.equals(blockPos));
                                StartPos pos = new StartPos(blockPos, faction, playerName, colorId);
                                pos.enabled = action == StartPosAction.ADD;
                                pos.ready = ready;
                                StartPosClientEvents.startPoses.add(pos);
                            }
                            case CLEAR -> {
                                StartPosClientEvents.startPoses.clear();
                                StartPosClientEvents.hiddenPlayers.clear();
                            }
                            case HIDDEN_PLAYER -> StartPosClientEvents.hiddenPlayers.put(playerName, ready);
                            case REMOVE -> {
                                StartPosClientEvents.startPoses.removeIf(sp -> sp.pos.equals(blockPos));
                            }
                            case RESET -> StartPosClientEvents.resetAll();
                            case SET_GAME_STARTING -> StartPosClientEvents.isStarting = true;
                            case UNSET_GAME_STARTING -> StartPosClientEvents.isStarting = false;
                            case PLAYER_READY -> StartPosClientEvents.setPlayerReady(playerName, true);
                            case PLAYER_UNREADY -> StartPosClientEvents.setPlayerReady(playerName, false);
                            case ENABLE -> StartPosClientEvents.setPosEnabled(blockPos, true);
                            case DISABLE -> StartPosClientEvents.setPosEnabled(blockPos, false);
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
