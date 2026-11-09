package com.solegendary.reignofnether.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// RoN's system lines in chat, one line each with a marker in front: ◆ info, ▲ important, ✖ error.
// Same look as team chat (TeamChatClient.format): dim marker, light body, no blank lines around it.
public final class SystemMessage {

    private SystemMessage() {}

    public static MutableComponent info(Component msg) {
        return line("◆", ChatFormatting.DARK_GRAY, ChatFormatting.GRAY, msg);
    }

    public static MutableComponent important(Component msg) {
        return line("▲", ChatFormatting.GOLD, ChatFormatting.WHITE, msg);
    }

    public static MutableComponent error(Component msg) {
        return line("✖", ChatFormatting.RED, ChatFormatting.GRAY, msg);
    }

    // the body colour only fills in where msg has none of its own
    private static MutableComponent line(String marker, ChatFormatting markerCol, ChatFormatting bodyCol, Component msg) {
        return Component.empty()
                .append(Component.literal(marker + " ").withStyle(markerCol))
                .append(Component.empty().withStyle(bodyCol).append(msg));
    }
}
