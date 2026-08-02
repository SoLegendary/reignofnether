package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CameraClientboundPacket {

    private final BlockPos pos;
    private final int ticks;

    public static void forceMoveCam(ServerPlayer player, BlockPos pos, int ticks) {
        if (player == null)
            return;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new CameraClientboundPacket(pos, ticks)
        );
    }

    public static void forceMoveCam(String playerName, BlockPos pos, int ticks) {
        if (playerName == null || playerName.isBlank())
            return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ServerPlayer sp = server.getPlayerList().getPlayerByName(playerName);
        if (sp != null)
            forceMoveCam(sp, pos, ticks);
    }


    public CameraClientboundPacket(BlockPos pos, int ticks) {
        this.pos = pos;
        this.ticks = ticks;
    }

    public CameraClientboundPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.ticks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.ticks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        OrthoviewClientEvents.forceMoveCam(this.pos, ticks);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}