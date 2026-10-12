package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import com.solegendary.reignofnether.util.DistHelper;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.network.RTSSimplePayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.function.Supplier;

public class CameraClientboundPacket  implements RTSSimplePayload {

    public static final CustomPacketPayload.Type<CameraClientboundPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "camera_clientbound"));

    @Override
    public CustomPacketPayload.Type<CameraClientboundPacket> type() {
        return TYPE;
    }

    private final String playerName;
    private final BlockPos pos;
    private final int cameraLockTicks;
    private final int forcePanTicks;
    private final int zoomLevel;

    public static void forceMoveCam(ServerPlayer player, BlockPos pos, int cameraLockTicks, int forcePanTicks, int zoomLevel) {
        if (player == null)
            return;
        PacketHandler.send(PacketHandler.toPlayer(() -> player),
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

    public CameraClientboundPacket(RegistryFriendlyByteBuf buffer) {
        this.playerName = buffer.readUtf();
        this.pos = buffer.readBlockPos();
        this.cameraLockTicks = buffer.readInt();
        this.forcePanTicks = buffer.readInt();
        this.zoomLevel = buffer.readInt();
    }

    public void encode(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(this.playerName);
        buffer.writeBlockPos(this.pos);
        buffer.writeInt(this.cameraLockTicks);
        buffer.writeInt(this.forcePanTicks);
        buffer.writeInt(this.zoomLevel);
    }

    public void handle(IPayloadContext ctx) {

        ctx.enqueueWork(() -> {
            DistHelper.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> {
                        OrthoviewClientEvents.forceMoveCam(this.playerName, this.pos, cameraLockTicks, forcePanTicks, zoomLevel);
                    });
        });
        return;
    }
}