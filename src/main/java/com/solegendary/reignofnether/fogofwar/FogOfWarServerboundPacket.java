package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FogOfWarServerboundPacket {

    private final String playerName;
    private final int[] xPos;
    private final int[] zPos;

    public static void saveExploredChunks(String playerName, Set<ChunkPos> exploredChunkPoses) {
        // TODO confirm this is in the right order
        int[] xp = exploredChunkPoses.stream().mapToInt(ChunkPos::getMinBlockX).toArray();
        int[] zp = exploredChunkPoses.stream().mapToInt(ChunkPos::getMinBlockZ).toArray();

        PacketHandler.INSTANCE.sendToServer(new FogOfWarServerboundPacket(playerName, xp, zp));
    }

    // packet-handler functions
    public FogOfWarServerboundPacket(String playerName, int[] xPos, int[] zPos) {
        this.playerName = playerName;
        this.xPos = xPos;
        this.zPos = zPos;
    }

    public FogOfWarServerboundPacket(FriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.xPos = buffer.readVarIntArray();
        this.zPos = buffer.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(playerName);
        buffer.writeVarIntArray(xPos);
        buffer.writeVarIntArray(zPos);
    }

    // server-side packet-consuming functions
    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            FogOfWarServerEvents.saveExploredChunks(playerName, xPos, zPos);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
