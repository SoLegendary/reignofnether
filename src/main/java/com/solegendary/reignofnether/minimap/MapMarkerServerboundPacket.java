package com.solegendary.reignofnether.minimap;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
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

public class MapMarkerServerboundPacket {
    private final int x;
    private final int z;



    public MapMarkerServerboundPacket(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public MapMarkerServerboundPacket(FriendlyByteBuf buffer) {
        this.x = buffer.readInt();
        this.z = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.x);
        buffer.writeInt(this.z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
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
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> target), markerPacket);
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> target),
                        new SoundClientboundPacket(SoundAction.ALLY, BlockPos.ZERO, "", 1.0f, -1));
            }
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
