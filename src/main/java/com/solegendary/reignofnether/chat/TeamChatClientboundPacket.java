package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.player.PlayerColors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Colours are only known client-side (PlayerColors), so the server sends who said what and the client builds
// the line: "● Name ➜ message" with the dot in the player's colour.
public class TeamChatClientboundPacket {

    private final String sender;
    private final Scope scope;
    private final String text;

    public TeamChatClientboundPacket(String sender, Scope scope, String text) {
        this.sender = sender;
        this.scope = scope;
        this.text = text;
    }

    public TeamChatClientboundPacket(FriendlyByteBuf buf) {
        this.sender = buf.readUtf();
        this.scope = buf.readEnum(Scope.class);
        this.text = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(sender);
        buf.writeEnum(scope);
        buf.writeUtf(text);
    }

    public static MutableComponent format(String sender, Scope scope, Component message) {
        MutableComponent line = Component.empty();
        if (scope == Scope.ALL)
            line.append(Component.translatable("chat.reignofnether.all").withStyle(ChatFormatting.DARK_GRAY)).append(" ");
        else if (scope == Scope.SPECTATOR)
            line.append(Component.translatable("chat.reignofnether.spectator").withStyle(ChatFormatting.DARK_GRAY)).append(" ");
        else if (scope == Scope.TEAM_SPECTATED)
            line.append(Component.translatable("chat.reignofnether.team").withStyle(ChatFormatting.DARK_GRAY)).append(" ");
        int colour = PlayerColors.getPlayerDisplayColorHex(sender);
        return line.append(Component.literal("● ").withStyle(s -> s.withColor(colour)))
                .append(Component.literal(sender).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" ➜ ").withStyle(ChatFormatting.DARK_GRAY))
                .append(message.copy().withStyle(ChatFormatting.GRAY));
    }

    // respects the player's chat visibility setting and their blocked/hidden players, like vanilla chat
    public static void show(String sender, Scope scope, Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.chatVisibility().get() != ChatVisiblity.FULL)
            return;
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(sender);
        if (info != null && mc.getPlayerSocialManager().shouldHideMessageFrom(info.getProfile().getId()))
            return;
        mc.gui.getChat().addMessage(format(sender, scope, message));
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                show(sender, scope, Component.literal(text))));
        ctx.get().setPacketHandled(true);
    }
}
