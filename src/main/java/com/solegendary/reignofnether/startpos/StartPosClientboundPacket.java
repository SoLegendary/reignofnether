package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
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

    public static void addPos(StartPos startPos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.ADD, startPos.pos, startPos.faction, startPos.playerName, startPos.colorId));
    }

    public static void removePos(BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.REMOVE, pos, Faction.NONE, "", 0));
    }

    public static void reservePos(BlockPos pos, Faction faction, String playerName) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.RESERVE, pos, faction, playerName, 0));
    }

    public static void unreservePos(BlockPos pos) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.UNRESERVE, pos, Faction.NONE, "", 0));
    }

    public static void reset() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.RESET, new BlockPos(0,0,0), Faction.NONE, "", 0));
    }

    public static void startGameCountdown() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.SET_GAME_STARTING, new BlockPos(0,0,0), Faction.NONE, "", 0));
    }

    public static void cancelStartGameCountdown() {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new StartPosClientboundPacket(StartPosAction.UNSET_GAME_STARTING, new BlockPos(0,0,0), Faction.NONE, "", 0));
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
        this.faction = buffer.readEnum(Faction.class);
        this.playerName = buffer.readUtf();
        this.colorId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.action);
        buffer.writeBlockPos(this.blockPos);
        buffer.writeEnum(this.faction);
        buffer.writeUtf(this.playerName);
        buffer.writeInt(this.colorId);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        switch (action) {
                            case ADD -> {
                                StartPosClientEvents.startPoses.removeIf(sp -> sp.pos.equals(blockPos));
                                StartPosClientEvents.startPoses.add(new StartPos(blockPos, faction, playerName, colorId));
                            }
                            case REMOVE -> {
                                StartPosClientEvents.startPoses.removeIf(sp -> sp.pos.equals(blockPos));
                            }
                            case RESERVE -> {
                                for (StartPos startPos : StartPosClientEvents.startPoses) {
                                    if (startPos.pos.equals(blockPos)) {
                                        startPos.faction = faction;
                                        startPos.playerName = playerName;
                                        break;
                                    }
                                }
                            }
                            case UNRESERVE -> {
                                for (StartPos startPos : StartPosClientEvents.startPoses) {
                                    if (startPos.pos.equals(blockPos)) {
                                        startPos.reset();
                                        break;
                                    }
                                }
                            }
                            case RESET -> StartPosClientEvents.resetAll();
                            case SET_GAME_STARTING -> StartPosClientEvents.isStarting = true;
                            case UNSET_GAME_STARTING -> StartPosClientEvents.isStarting = false;
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
