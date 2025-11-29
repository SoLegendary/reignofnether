package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class GameModeClientboundPacket {

    public GameMode gameMode;

    // sets the gamemode of all players
    // unlocked and reset back to
    public static void setAndLockAllClientGameModes(GameMode mode) {
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new GameModeClientboundPacket(mode));
    }

    public GameModeClientboundPacket(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameModeClientboundPacket(FriendlyByteBuf buffer) {
        this.gameMode = buffer.readEnum(GameMode.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(this.gameMode);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (gameMode != GameMode.NONE) {
                            ClientGameModeHelper.gameModeLocked = true;
                            ClientGameModeHelper.gameMode = this.gameMode;
                            if (gameMode != GameMode.CLASSIC && StartPosClientEvents.hasReservedPos()) {
                                StartPosClientEvents.selectedFaction = Faction.NONE;
                                StartPosServerboundPacket.unreservePos(StartPosClientEvents.getPos().pos);
                            }
                        } else {
                            ClientGameModeHelper.gameModeLocked = false;
                        }
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
