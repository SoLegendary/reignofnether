package com.solegendary.reignofnether.replay;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** One piece of a replay downloaded from the end screen (size -1: the server can't send it). */
public class ReplayChunkClientboundPacket {

    public static final int CHUNK = 32 * 1024;

    private final String fileName;
    private final long size;
    private final long offset;
    private final byte[] data;

    public static void send(ServerPlayer player, String fileName, long size, long offset, byte[] data) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new ReplayChunkClientboundPacket(fileName, size, offset, data));
    }

    public static void fail(ServerPlayer player) {
        send(player, "", -1, 0, new byte[0]);
    }

    public ReplayChunkClientboundPacket(String fileName, long size, long offset, byte[] data) {
        this.fileName = fileName;
        this.size = size;
        this.offset = offset;
        this.data = data;
    }

    public ReplayChunkClientboundPacket(FriendlyByteBuf buffer) {
        this.fileName = buffer.readUtf();
        this.size = buffer.readLong();
        this.offset = buffer.readLong();
        this.data = buffer.readByteArray(CHUNK);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(fileName);
        buffer.writeLong(size);
        buffer.writeLong(offset);
        buffer.writeByteArray(data);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ReplayClientEvents.onDownloadChunk(fileName, size, offset, data)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
