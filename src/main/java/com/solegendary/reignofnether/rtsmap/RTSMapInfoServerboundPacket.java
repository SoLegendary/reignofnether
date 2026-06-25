package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class RTSMapInfoServerboundPacket {
    private final String mode;

    public static void setStartingMode(String mode) {
        PacketHandler.INSTANCE.sendToServer(new RTSMapInfoServerboundPacket(mode));
    }

    public RTSMapInfoServerboundPacket(String mode) {
        this.mode = mode;
    }

    public RTSMapInfoServerboundPacket(FriendlyByteBuf buffer) {
        this.mode = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.mode);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {

            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("RTSMapInfoServerboundPacket: Sender was null");
                success.set(false);
                return;
            } else if (!player.hasPermissions(2)) {
                ReignOfNether.LOGGER.warn("RTSMapInfoServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                success.set(false);
                return;
            }
            if (RTSMapInfoServerEvents.rtsMapInfo != null &&
                RTSMapInfoServerEvents.rtsMapInfo.supportsMode(mode)) {
                RTSMapInfoServerEvents.rtsMapInfo.setDefaultMode(mode);
                RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MODE, mode);
                StartPosServerEvents.loadPositionsFromMapInfo();
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
