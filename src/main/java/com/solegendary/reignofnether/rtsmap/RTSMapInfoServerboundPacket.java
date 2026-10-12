package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class RTSMapInfoServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<RTSMapInfoServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "rts_map_info_serverbound"));

    @Override
    public CustomPacketPayload.Type<RTSMapInfoServerboundPacket> type() {
        return TYPE;
    }
    private final String mode;

    public static void setStartingMode(String mode) {
        PacketHandler.sendToServer(new RTSMapInfoServerboundPacket(mode));
    }

    public RTSMapInfoServerboundPacket(String mode) {
        this.mode = mode;
    }

    public RTSMapInfoServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.mode = buffer.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.mode);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {

            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                ReignOfNether.LOGGER.warn("RTSMapInfoServerboundPacket: Sender was null");
                return;
            } else if (!player.hasPermissions(2)) {
                ReignOfNether.LOGGER.warn("RTSMapInfoServerboundPacket: Tried to process packet from " + player.getName() + " with insufficient permissions");
                return;
            }
            if (RTSMapInfoServerEvents.rtsMapInfo != null &&
                RTSMapInfoServerEvents.rtsMapInfo.supportsMode(mode) &&
                !StartPosServerEvents.isStartingGame()) {
                RTSMapInfoServerEvents.rtsMapInfo.setDefaultMode(mode);
                RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MODE, mode);
                StartPosServerEvents.loadPositionsFromMapInfo();
            }
        });
        return;
    }
}
