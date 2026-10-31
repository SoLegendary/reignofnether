package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.chat.ChatPanel;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRolesClient;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

/**
 * Voice follows the chat channel: switching TEAM / ALL in chat switches who hears you too, so there is one
 * channel to think about. Only with Simple Voice Chat installed and connected to the server's voice chat.
 */
public class VoiceClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    @Nullable private static VoiceChannel sent = null;

    public static void reset() {
        sent = null;
        checkedAt = 0;
    }

    private static Method getClient, getConnection, isInitialized;
    private static boolean lastConnected = false;
    private static long checkedAt = 0;

    /** Whether Simple Voice Chat is connected to this server's voice chat (read by reflection, no dependency).
     *  Re-checked at most once a second: it's read every tick and every chat panel frame. */
    public static boolean connected() {
        if (MC.player == null || !ModList.get().isLoaded("voicechat"))
            return false;
        long now = System.currentTimeMillis();
        if (now - checkedAt < 1000)
            return lastConnected;
        checkedAt = now;
        lastConnected = false;
        try {
            if (getClient == null)
                getClient = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager").getMethod("getClient");
            Object client = getClient.invoke(null);
            if (client == null)
                return false;
            if (getConnection == null)
                getConnection = client.getClass().getMethod("getConnection");
            Object connection = getConnection.invoke(client);
            if (connection == null)
                return false;
            if (isInitialized == null)
                isInitialized = connection.getClass().getMethod("isInitialized");
            lastConnected = (boolean) isInitialized.invoke(connection);
        } catch (ReflectiveOperationException | RuntimeException e) {
            lastConnected = false;
        }
        return lastConnected;
    }

    /** The voice channel matching the chat channel: TEAM chat is your side, ALL chat is everyone (or nearby). */
    public static VoiceChannel current() {
        PlayerRole role = PlayerRolesClient.myRole();
        boolean team = ChatPanel.isTeamChannel();
        return switch (role) {
            case COMMANDER -> team ? VoiceChannel.TEAM : VoiceChannel.ALL;
            case SOLDIER -> team ? VoiceChannel.SQUAD : VoiceChannel.LOCAL;
            case SPECTATOR -> VoiceChannel.TEAM;
        };
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || PlayerClientEvents.rtsPlayers.isEmpty() || !connected())
            return;
        VoiceChannel want = current();
        if (want != sent) {
            VoiceChannelServerboundPacket.choose(want);
            sent = want;
        }
    }
}
