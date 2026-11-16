package com.solegendary.reignofnether.guide;

import com.solegendary.reignofnether.registrars.PacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// what the player did that map scripts wait for: read a dialogue, reached a line with a callback, or a game input
public class GuideServerboundPacket {

    public enum Kind { DIALOGUE_END, LINE, INPUT, READY }

    private final Kind kind;
    private final String value;

    public static void dialogueEnded() {
        PacketHandler.INSTANCE.sendToServer(new GuideServerboundPacket(Kind.DIALOGUE_END, ""));
    }

    // a line sent with "then <file>" started showing: value is its callback id
    public static void lineShown(int callbackId) {
        PacketHandler.INSTANCE.sendToServer(new GuideServerboundPacket(Kind.LINE, String.valueOf(callbackId)));
    }

    // an input event a script listens for (RonEvents.INPUT), with its detail, e.g. "unit_selected/villager"
    public static void input(String event) {
        PacketHandler.INSTANCE.sendToServer(new GuideServerboundPacket(Kind.INPUT, event));
    }

    // the loading screen finished: map scripts can talk to this player now
    public static void ready() {
        PacketHandler.INSTANCE.sendToServer(new GuideServerboundPacket(Kind.READY, ""));
    }

    public GuideServerboundPacket(Kind kind, String value) {
        this.kind = kind;
        this.value = value;
    }

    public GuideServerboundPacket(FriendlyByteBuf buffer) {
        this.kind = buffer.readEnum(Kind.class);
        this.value = buffer.readUtf(256);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeEnum(kind);
        buffer.writeUtf(value, 256);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null)
                return;
            switch (kind) {
                case DIALOGUE_END -> GuideServer.dialogueEnded(player);
                case LINE -> GuideServer.lineShown(player, value);
                case INPUT -> GuideServer.input(player, value);
                case READY -> GuideServer.ready(player);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}
