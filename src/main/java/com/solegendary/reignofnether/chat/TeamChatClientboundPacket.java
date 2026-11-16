package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Colours are only known client-side (PlayerColors), so the server sends who said what and the client builds
// the line (TeamChatClient).
public class TeamChatClientboundPacket {

    private final String sender;
    private final Component displayName;
    private final Scope scope;
    private final String text;

    public TeamChatClientboundPacket(String sender, Component displayName, Scope scope, String text) {
        this.sender = sender;
        this.displayName = displayName;
        this.scope = scope;
        this.text = text;
    }

    public TeamChatClientboundPacket(FriendlyByteBuf buf) {
        this.sender = buf.readUtf();
        this.displayName = buf.readComponent();
        this.scope = buf.readEnum(Scope.class);
        this.text = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(sender);
        buf.writeComponent(displayName);
        buf.writeEnum(scope);
        buf.writeUtf(text);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                TeamChatClient.show(sender, displayName, scope, Component.literal(text))));
        ctx.get().setPacketHandled(true);
    }
}
