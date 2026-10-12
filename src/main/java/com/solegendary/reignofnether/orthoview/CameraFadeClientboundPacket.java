package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

public class CameraFadeClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<CameraFadeClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "camera_fade_clientbound"));

    @Override
    public CustomPacketPayload.Type<CameraFadeClientboundPacket> type() {
        return TYPE;
    }

    private final String playerName;
    private final BlockPos pos;
    private final int fadeOutTicks;
    private final int blackoutTicks;
    private final int fadeInTicks;

    public static void fadeMoveCam(ServerPlayer player, BlockPos pos, int fadeOutTicks, int blackoutTicks, int fadeInTicks) {
        if (player == null)
            return;
        PacketHandler.send(PacketHandler.toPlayer(() -> player),
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

    public CameraFadeClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.fadeOutTicks = buffer.readInt();
        this.blackoutTicks = buffer.readInt();
        this.fadeInTicks = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.fadeOutTicks);
        buffer.writeInt(this.blackoutTicks);
        buffer.writeInt(this.fadeInTicks);
    }

    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        CameraFadeClientEvents.fadeMoveCam(this.playerName, this.pos,
                                this.fadeOutTicks, this.blackoutTicks, this.fadeInTicks);
                    });
        });
        return;
    }
}