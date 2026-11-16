package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class RonFileClientboundPacket {

    private static final int MAX_LENGTH = 262144; // a long file list

    private final RonFileAction action;
    private final String path;
    private final String text;

    public static void send(ServerPlayer player, RonFileAction action, String path, String text) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RonFileClientboundPacket(action, path, text));
    }

    public RonFileClientboundPacket(RonFileAction action, String path, String text) {
        this.action = action;
        this.path = path;
        this.text = text;
    }

    public RonFileClientboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(RonFileAction.class);
        this.path = buffer.readUtf(RonFileServerboundPacket.MAX_PATH);
        this.text = buffer.readUtf(MAX_LENGTH);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeUtf(path, RonFileServerboundPacket.MAX_PATH);
        buffer.writeUtf(text, MAX_LENGTH);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RonFilesClient.received(action, path, text)));
        ctx.get().setPacketHandled(true);
        return true;
    }
}
