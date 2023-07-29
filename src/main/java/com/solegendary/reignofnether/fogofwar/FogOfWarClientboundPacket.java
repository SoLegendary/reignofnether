package com.solegendary.reignofnether.fogofwar;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FogOfWarClientboundPacket {

    private final String playerName;
    private final int[] xPos;
    private final int[] zPos;

    public static void loadExploredChunks(String playerName, Set<ChunkPos> exploredChunkPoses) {
        System.out.println("loading " + exploredChunkPoses.size() + " explored chunks for: " + playerName);

        // TODO confirm this is in the right order
        int[] xp = exploredChunkPoses.stream().mapToInt(ChunkPos::getMinBlockX).toArray();
        int[] zp = exploredChunkPoses.stream().mapToInt(ChunkPos::getMinBlockZ).toArray();

        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new FogOfWarClientboundPacket(playerName, xp, zp));
    }

    // packet-handler functions
    public FogOfWarClientboundPacket(String playerName, int[] xPos, int[] zPos) {
        this.playerName = playerName;
        this.xPos = xPos;
        this.zPos = zPos;
    }

    public FogOfWarClientboundPacket(FriendlyByteBuf buffer) {
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
            FogOfWarClientEvents.loadExploredChunks(playerName, xPos, zPos);
            success.set(true);
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
