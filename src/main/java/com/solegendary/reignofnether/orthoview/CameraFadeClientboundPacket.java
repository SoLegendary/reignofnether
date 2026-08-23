package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class CameraFadeClientboundPacket {

    private final String playerName;
    private final BlockPos pos;
    private final int fadeOutTicks;
    private final int blackoutTicks;
    private final int fadeInTicks;

    public static void fadeMoveCam(ServerPlayer player, BlockPos pos, int fadeOutTicks, int blackoutTicks, int fadeInTicks) {
        if (player == null)
            return;
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new CameraFadeClientboundPacket(player.getName().getString(), pos, fadeOutTicks, blackoutTicks, fadeInTicks)
        );
    }

    public CameraFadeClientboundPacket(String playerName, BlockPos pos, int fadeOutTicks, int blackoutTicks, int fadeInTicks) {
        this.playerName = playerName;
        this.pos = pos;
        this.fadeOutTicks = fadeOutTicks;
        this.blackoutTicks = blackoutTicks;
        this.fadeInTicks = fadeInTicks;
    }

    public CameraFadeClientboundPacket(FriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.fadeOutTicks = buffer.readInt();
        this.blackoutTicks = buffer.readInt();
        this.fadeInTicks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.fadeOutTicks);
        buffer.writeInt(this.blackoutTicks);
        buffer.writeInt(this.fadeInTicks);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);

        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        CameraFadeClientEvents.fadeMoveCam(this.playerName, this.pos,
                                this.fadeOutTicks, this.blackoutTicks, this.fadeInTicks);
                        success.set(true);
                    });
        });
        ctx.get().setPacketHandled(true);
        return success.get();
    }
}