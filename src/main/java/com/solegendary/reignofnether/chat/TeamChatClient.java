package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.ChatVisiblity;

import java.util.Locale;

// client side of team chat: builds "● Name ➜ message" with the dot in the player's colour and shows it
public class TeamChatClient {

    // "[Team] [Commander] ● Name ➜ message": the role tag and dot in the team's colour (a soldier's commander's)
    public static MutableComponent format(String sender, Component displayName, Scope scope, Component message) {
        MutableComponent line = Component.empty();
        if (scope == Scope.ALL)
            line.append(Component.translatable("chat.reignofnether.all").withStyle(ChatFormatting.DARK_GRAY)).append(" ");
        else if (scope == Scope.SPECTATOR)
            line.append(Component.translatable("chat.reignofnether.spectator").withStyle(ChatFormatting.DARK_GRAY)).append(" ");
        else if (scope == Scope.TEAM_SPECTATED)
            line.append(Component.translatable("chat.reignofnether.team").withStyle(ChatFormatting.DARK_GRAY)).append(" ");
        int colour = PlayerColors.getPlayerDisplayColorHex(PlayerRolesClient.commanderOf(sender));
        if (scope != Scope.LOBBY) {
            PlayerRole role = PlayerRolesClient.roleOf(sender);
            line.append(Component.translatable("chat.reignofnether.role." + role.name().toLowerCase(Locale.ROOT))
                    .withStyle(s -> role.inMatch ? s.withColor(colour) : s.withColor(ChatFormatting.DARK_GRAY))).append(" ");
        }
        return line.append(Component.literal("● ").withStyle(s -> s.withColor(colour)))
                .append(displayName.copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" ➜ ").withStyle(ChatFormatting.DARK_GRAY))
                .append(message.copy().withStyle(ChatFormatting.GRAY));
    }

    // respects the player's chat visibility setting and their blocked/hidden players, like vanilla chat
    public static void show(String sender, Scope scope, Component message) {
        show(sender, Component.literal(sender), scope, message);
    }

    public static void show(String sender, Component displayName, Scope scope, Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.chatVisibility().get() != ChatVisiblity.FULL)
            return;
        PlayerInfo info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(sender);
        if (info != null && mc.getPlayerSocialManager().shouldHideMessageFrom(info.getProfile().getId()))
            return;
        MutableComponent line = format(sender, displayName, scope, message);
        ChatLog.expect(ChatLog.Channel.of(scope), sender, message, line);
        mc.gui.getChat().addMessage(line);
    }
}
