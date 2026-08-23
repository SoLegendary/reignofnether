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

    private final String playerName;
    private final BlockPos pos;
    private final int cameraLockTicks;
    private final int forcePanTicks;
    private final int zoomLevel;

    public static void forceMoveCam(ServerPlayer player, BlockPos pos, int cameraLockTicks, int forcePanTicks, int zoomLevel) {
        if (player == null)
            return;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new CameraClientboundPacket(player.getName().getString(), pos, cameraLockTicks, forcePanTicks, zoomLevel)
        );
    }

    public CameraClientboundPacket(String playerName, BlockPos pos, int cameraLockTicks, int forcePanTicks, int zoomLevel) {
        this.playerName = playerName;
        this.pos = pos;
        this.cameraLockTicks = cameraLockTicks;
        this.forcePanTicks = forcePanTicks;
        this.zoomLevel = zoomLevel;
    }

    public CameraClientboundPacket(FriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.cameraLockTicks = buffer.readInt();
        this.forcePanTicks = buffer.readInt();
        this.zoomLevel = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.cameraLockTicks);
        buffer.writeInt(this.forcePanTicks);
        buffer.writeInt(this.zoomLevel);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        OrthoviewClientEvents.forceMoveCam(this.playerName, this.pos, cameraLockTicks, forcePanTicks, zoomLevel);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}