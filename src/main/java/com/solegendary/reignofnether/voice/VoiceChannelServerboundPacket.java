package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// the voice channel a player picked; VoiceChannels only keeps it while their role allows it
public class VoiceChannelServerboundPacket {

    private final VoiceChannel channel;

    public static void choose(VoiceChannel channel) {
        PacketHandler.INSTANCE.sendToServer(new VoiceChannelServerboundPacket(channel));
    }

    public VoiceChannelServerboundPacket(VoiceChannel channel) {
        this.channel = channel;
    }

    public VoiceChannelServerboundPacket(FriendlyByteBuf buffer) {
        this.channel = buffer.readEnum(VoiceChannel.class);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(channel);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
                VoiceChannels.set(player, channel);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
