package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class MapMarkerServerboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<MapMarkerServerboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "map_marker_serverbound"));

    @Override
    public CustomPacketPayload.Type<MapMarkerServerboundPacket> type() {
        return TYPE;
    }
    private final int x;
    private final int z;

    public MapMarkerServerboundPacket(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public MapMarkerServerboundPacket(RegistryFriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.z = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.z);
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            PlayerList playerList = server.getPlayerList();
            Set<ServerPlayer> recipients = new HashSet<>();
            recipients.add(player);
            String playerName = player.getName().getString();
            for (String allyName : AlliancesServerEvents.getAllAllies(playerName)) {
                ServerPlayer allyPlayer = playerList.getPlayerByName(allyName);
                if (allyPlayer != null) {
                    recipients.add(allyPlayer);
                }
            }

            MapMarkerClientboundPacket markerPacket = new MapMarkerClientboundPacket(x, z, playerName);
            for (ServerPlayer target : recipients) {
                PacketHandler.send(PacketHandler.toPlayer(() -> target), markerPacket);
                PacketHandler.send(PacketHandler.toPlayer(() -> target),
                        new SoundClientboundPacket(SoundAction.ALLY, BlockPos.ZERO, "", 1.0f, -1));
            }
        });
        return;
    }
}
