package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

// Client asks the server to resend fogged (dark or edge) chunks that are completely missing from its chunk
// cache. The server answers with the same data it would have sent when the chunk was first loaded.
public class FogChunkRequestServerboundPacket {

    public static final int MAX_CHUNKS_PER_REQUEST = 32;

    private final List<ChunkPos> chunks;

    public static void requestChunks(List<ChunkPos> chunks) {
        Minecraft MC = Minecraft.getInstance();
        if (MC.player != null && !chunks.isEmpty())
            PacketHandler.INSTANCE.sendToServer(new FogChunkRequestServerboundPacket(chunks));
    }

    // packet-handler functions
    public FogChunkRequestServerboundPacket(List<ChunkPos> chunks) {
        this.chunks = chunks;
    }

    public FogChunkRequestServerboundPacket(FriendlyByteBuf buffer) {
        int n = Math.min(buffer.readVarInt(), MAX_CHUNKS_PER_REQUEST);
        this.chunks = new ArrayList<>(n);
        for (int i = 0; i < n; i++)
            this.chunks.add(new ChunkPos(buffer.readLong()));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(chunks.size());
        for (ChunkPos cp : chunks)
            buffer.writeLong(cp.toLong());
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                ReignOfNether.LOGGER.warn("FogChunkRequestServerboundPacket: Sender was null");
                return;
            }
            FogOfWarServerEvents.handleChunkRequest(player, chunks);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
