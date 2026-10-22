package com.solegendary.reignofnether.voice;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.chat.TeamChatServerEvents;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// each player's chosen voice channel, clamped to what their role allows. The voice thread (RonVoicechatPlugin)
// only reads the per-tick snapshot below, never live game state
public class VoiceChannels {

    private record Seat(PlayerRole role, String faction, Set<String> team) {}

    private static final Map<UUID, VoiceChannel> chosen = new ConcurrentHashMap<>();
    private static volatile Map<UUID, Seat> seats = Map.of(); // empty outside a match
    private static volatile boolean allChat;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        if (!TeamChatServerEvents.isMatch()) {
            seats = Map.of();
            return;
        }
        Map<UUID, Seat> next = new HashMap<>();
        for (ServerPlayer p : evt.getServer().getPlayerList().getPlayers()) {
            String name = p.getName().getString();
            String faction = PlayerRoles.commanderOf(name);
            Set<String> team = new HashSet<>(AlliancesServerEvents.getAllAllies(faction));
            team.add(faction);
            next.put(p.getUUID(), new Seat(PlayerRoles.roleOf(name), faction, team));
        }
        allChat = evt.getServer().getGameRules().getBoolean(GameRuleRegistrar.ALL_CHAT);
        seats = next;
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        chosen.remove(evt.getEntity().getUUID());
    }

    public static boolean isMatch() {
        return !seats.isEmpty();
    }

    public static Set<UUID> players() {
        return seats.keySet();
    }

    public static VoiceChannel get(UUID player) {
        Seat seat = seats.get(player);
        PlayerRole role = seat != null ? seat.role() : PlayerRole.SPECTATOR;
        VoiceChannel channel = chosen.get(player);
        return channel != null && VoiceChannel.allowedFor(role).contains(channel) ? channel : VoiceChannel.defaultFor(role);
    }

    public static void set(ServerPlayer player, VoiceChannel channel) {
        chosen.put(player.getUUID(), channel);
    }

    public static boolean canHear(ServerPlayer speaker, ServerPlayer listener, VoiceChannel channel) {
        Seat s = seats.get(speaker.getUUID());
        Seat l = seats.get(listener.getUUID());
        if (s == null || l == null)
            return false;
        double distance = speaker.level() == listener.level() ? speaker.distanceTo(listener) : Double.MAX_VALUE;
        return VoiceChannel.canHear(channel, s.role(), l.role(), s.faction().equals(l.faction()),
                s.team().contains(l.faction()), distance, allChat);
    }
}
