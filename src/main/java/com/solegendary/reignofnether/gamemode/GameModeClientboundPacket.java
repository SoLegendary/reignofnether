package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class GameModeClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<GameModeClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "game_mode_clientbound"));

    @Override
    public CustomPacketPayload.Type<GameModeClientboundPacket> type() {
        return TYPE;
    }

    public GameMode gameMode;

    // sets the gamemode of all players
    // unlocked and reset back to
    public static void setAndLockAllClientGameModes(GameMode mode) {
        PacketHandler.send(PacketHandler.allPlayers(),
                new GameModeClientboundPacket(mode));
    }

    public GameModeClientboundPacket(GameMode gameMode) {
        this.gameMode = gameMode;
    }

    public GameModeClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.gameMode = buffer.readEnum(GameMode.class);
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeEnum(this.gameMode);
    }

    // server-side packet-consuming functions
    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        if (gameMode != GameMode.NONE) {
                            ClientGameModeHelper.gameModeLocked = true;
                            ClientGameModeHelper.gameMode = this.gameMode;
                            if (gameMode != GameMode.CLASSIC && StartPosClientEvents.hasReservedPos()) {
                                StartPosClientEvents.selectedFaction = Factions.NONE;
                                StartPosServerboundPacket.unreservePos(StartPosClientEvents.getPos().pos);
                            }
                        } else {
                            ClientGameModeHelper.gameModeLocked = false;
                        }
                    });
        });
        return;
    }
}
