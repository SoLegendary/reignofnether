package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// During a match, chat goes to your team; start a message with '!' to talk to everyone. Spectators only reach
// other spectators (even with '!') so they can't call out positions. Outside a match chat is left alone. Runs after RoN's
// cheat-code handler (normal priority), which only reads the message.
public class TeamChatServerEvents {

    // TEAM_SPECTATED: a team line as spectators see it (every team's chat, tagged [Team])
    public enum Scope { TEAM, ALL, SPECTATOR, TEAM_SPECTATED }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChat(ServerChatEvent evt) {
        if (evt.isCanceled() || !isMatch())
            return;
        String name = evt.getPlayer().getName().getString();
        String msg = evt.getRawText().trim();
        evt.setCanceled(true);
        Scope scope = scopeFor(PlayerServerEvents.isRTSPlayer(name), msg);
        if (msg.startsWith("!"))
            msg = msg.substring(1).trim();
        if (!msg.isEmpty())
            send(name, scope, msg);
    }

    // a real match; sandbox and survival chat stays as vanilla
    static boolean isMatch() {
        return PlayerServerEvents.isGameActive() && !SandboxServer.isAnyoneASandboxPlayer() && !SurvivalServerEvents.isEnabled();
    }

    // '!' talks to everyone, but only for players: spectators never reach the players, '!' or not
    static Scope scopeFor(boolean isPlayer, String msg) {
        if (!isPlayer)
            return Scope.SPECTATOR;
        return msg.startsWith("!") ? Scope.ALL : Scope.TEAM;
    }

    public static void send(String sender, Scope scope, String text) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ReignOfNether.LOGGER.info("[Chat/{}] <{}> {}", scope, sender, text);
        TeamChatClientboundPacket packet = new TeamChatClientboundPacket(sender, scope, text);
        List<ServerPlayer> sentTo = recipients(server, sender, scope);
        for (ServerPlayer p : sentTo)
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), packet);
        // spectators follow every team's chat (players never see the other team's)
        if (scope == Scope.TEAM && isMatch()) {
            TeamChatClientboundPacket tagged = new TeamChatClientboundPacket(sender, Scope.TEAM_SPECTATED, text);
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                if (!sentTo.contains(p) && !PlayerServerEvents.isRTSPlayer(p.getName().getString()))
                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), tagged);
        }
    }

    static List<ServerPlayer> recipients(MinecraftServer server, String sender, Scope scope) {
        List<ServerPlayer> list = new ArrayList<>();
        Set<String> team = scope == Scope.TEAM ? AlliancesServerEvents.getAllAllies(sender) : Set.of();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String n = p.getName().getString();
            boolean add = switch (scope) {
                case ALL -> true;
                case TEAM -> n.equals(sender) || team.contains(n);
                case SPECTATOR -> !PlayerServerEvents.isRTSPlayer(n);
                case TEAM_SPECTATED -> false; // only built by send() for spectators
            };
            if (add)
                list.add(p);
        }
        return list;
    }
}
