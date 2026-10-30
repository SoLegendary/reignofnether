package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.ui.Theme;
import net.minecraft.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Every line added to the chat this session, with its channel and sender, for the RTS chat panel, the
 * recent-chat overlay and the match setup screen.
 *
 * Lines are captured where vanilla adds them to the chat (ChatComponentMixin), so client-side messages
 * (tutorial, ready checks) are caught too. Whoever knows who said what (player chat) calls
 * {@link #expect} just before, and the next line added is tagged with it; anything else is INFO.
 */
public final class ChatLog {

    private ChatLog() { }

    public enum Channel {
        ALL(Theme.MUTED), INFO(Theme.ORANGE);

        public final int colour;

        Channel(int colour) {
            this.colour = colour;
        }

        public String label() {
            return I18n.get("chat.reignofnether.channel." + name().toLowerCase(Locale.ROOT));
        }
    }

    /**
     * @param sender empty for INFO lines
     * @param text   the message without the sender
     * @param line   the whole line as vanilla chat shows it
     */
    public record Entry(Channel channel, String sender, Component text, Component line, long at) {
        public long age() {
            return Util.getMillis() - at;
        }
    }

    static final int MAX = 100;
    private static final Deque<Entry> entries = new ArrayDeque<>();
    @Nullable
    private static Entry expected = null;

    public static List<Entry> entries() {
        return new ArrayList<>(entries);
    }

    /** The newest {@code max} entries younger than maxAgeMs, oldest first. */
    public static List<Entry> recent(int max, long maxAgeMs) {
        List<Entry> out = new ArrayList<>();
        var it = entries.descendingIterator();
        while (it.hasNext() && out.size() < max) {
            Entry e = it.next();
            if (e.age() >= maxAgeMs)
                break;
            out.add(0, e);
        }
        return out;
    }

    /** Tags the next line added to the chat, if it is {@code line}. */
    public static void expect(Channel channel, String sender, Component text, Component line) {
        expected = new Entry(channel, sender, text, line, 0);
    }

    /** Called by ChatComponentMixin for every line added to the vanilla chat. */
    public static void onAdded(Component line) {
        Entry e = expected != null && expected.line().equals(line)
                ? new Entry(expected.channel(), expected.sender(), expected.text(), line, Util.getMillis())
                : new Entry(Channel.INFO, "", line, line, Util.getMillis());
        expected = null;
        if (entries.size() >= MAX)
            entries.pollFirst();
        entries.addLast(e);
    }

    // last handler so the line matches what is finally added
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerChat(ClientChatReceivedEvent.Player evt) {
        if (evt.getMessage() != null)
            expect(Channel.ALL, evt.getBoundChatType().name().getString(),
                    evt.getPlayerChatMessage().decoratedContent(), evt.getMessage());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        entries.clear();
        expected = null;
    }
}
