package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RonFileServerboundPacket {

    // a file's UTF-8 size, well under the 32 KiB a client may send in one packet
    public static final int MAX_BYTES = 30000;
    static final int MAX_PATH = 256;

    private final RonFileAction action;
    private final String path;
    private final String text;

    public static void send(RonFileAction action, String path, String text) {
        PacketHandler.INSTANCE.sendToServer(new RonFileServerboundPacket(action, path, text));
    }

    public RonFileServerboundPacket(RonFileAction action, String path, String text) {
        this.action = action;
        this.path = path;
        this.text = text;
    }

    public RonFileServerboundPacket(FriendlyByteBuf buffer) {
        this.action = buffer.readEnum(RonFileAction.class);
        this.path = buffer.readUtf(MAX_PATH);
        this.text = buffer.readUtf(MAX_BYTES);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(action);
        buffer.writeUtf(path, MAX_PATH);
        buffer.writeUtf(text, MAX_BYTES);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            // ron/ files run as an op at match start, so only ops or the singleplayer host may see or change them
            if (!PlayerServerEvents.isOpOrHost(player)) {
                ReignOfNether.LOGGER.warn("RonFileServerboundPacket: " + player.getName().getString() + " is not an op");
                return;
            }
            RonFilesServer.handle(player, action, path, text);
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
