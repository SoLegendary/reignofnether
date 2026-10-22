package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.chat.ChatLog.Channel;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatLogTest {

    @Test
    void tagsOnlyTheExpectedLineAndKeepsMessagesUnchanged() {
        ChatLog.onLogout(null);
        Component line = Component.literal("Bob ➜ hi");
        ChatLog.expect(Channel.TEAM, "Bob", Component.literal("hi"), line);
        ChatLog.onAdded(Component.literal("◆ Alice is ready"));
        ChatLog.expect(Channel.TEAM, "Bob", Component.literal("hi"), line);
        ChatLog.onAdded(line);
        ChatLog.onAdded(Component.literal("plain"));

        var e = ChatLog.entries();
        assertEquals(Channel.INFO, e.get(0).channel());
        assertEquals("◆ Alice is ready", e.get(0).line().getString());
        assertEquals(Channel.TEAM, e.get(1).channel());
        assertEquals("Bob", e.get(1).sender());
        assertEquals(Channel.INFO, e.get(2).channel()); // expectation is used up
        assertEquals(2, ChatLog.recent(2, 10_000).size());
        assertEquals("plain", ChatLog.recent(2, 10_000).get(1).text().getString());
    }
}
