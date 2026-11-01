package com.solegendary.reignofnether.chat;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;

// During a match, chat goes to your team (commanders and soldiers); start a message with '!' to talk to everyone,
// unless the allChat rule is off. Spectators only talk to other spectators. In the match lobby chat is formatted the same way
// (no channel tag); sandbox, survival and other worlds keep vanilla chat. Runs after RoN's
// cheat-code handler (normal priority), which only reads the message.
public class TeamChatServerEvents {

    // TEAM_SPECTATED: a team line as spectators see it (every team's chat, tagged [Team])
    // LOBBY: before a match, everyone, no channel tag (kept last: sent by ordinal)
    public enum Scope { TEAM, ALL, SPECTATOR, TEAM_SPECTATED, LOBBY }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChat(ServerChatEvent evt) {
        if (evt.isCanceled())
            return;
        String name = evt.getPlayer().getName().getString();
        String msg = evt.getRawText().trim();
        if (!isMatch()) {
            if (isLobby() && !msg.isEmpty()) {
                evt.setCanceled(true);
                send(name, evt.getPlayer().getDisplayName(), Scope.LOBBY, msg);
            }
            return;
        }
        evt.setCanceled(true);
        boolean allChat = evt.getPlayer().level().getGameRules().getBoolean(GameRuleRegistrar.ALL_CHAT);
        if (msg.startsWith("!") && !allChat) {
            evt.getPlayer().sendSystemMessage(SystemMessage.error(Component.translatable("chat.reignofnether.all_disabled")));
            return;
        }
        Scope scope = scopeFor(PlayerRoles.roleOf(name), msg, allChat);
        if (msg.startsWith("!"))
            msg = msg.substring(1).trim();
        if (!msg.isEmpty())
            send(name, evt.getPlayer().getDisplayName(), scope, msg);
    }

    // a map with start positions and no match running yet
    static boolean isLobby() {
        return !PlayerServerEvents.isGameActive() && !StartPosServerEvents.startPoses.isEmpty()
                && !SandboxServer.isAnyoneASandboxPlayer() && !SurvivalServerEvents.isEnabled();
    }

    // a real match; sandbox and survival chat stays as vanilla
    public static boolean isMatch() {
        return PlayerServerEvents.isGameActive() && !SandboxServer.isAnyoneASandboxPlayer() && !SurvivalServerEvents.isEnabled();
    }

    // '!' talks to everyone when allChat is on; otherwise players reach their team. Spectators (defeated players
    // included) only ever reach each other, so they can't call out what they see
    static Scope scopeFor(PlayerRole role, String msg, boolean allChat) {
        if (!role.inMatch)
            return Scope.SPECTATOR;
        return msg.startsWith("!") && allChat ? Scope.ALL : Scope.TEAM;
    }

    // displayName carries whatever the server adds to the name (e.g. a network's badges)
    public static void send(String sender, Component displayName, Scope scope, String text) {
        send(sender, displayName, scope, Component.literal(text));
    }

    public static void send(String sender, Component displayName, Scope scope, Component text) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        ReignOfNether.LOGGER.info("[Chat/{}] <{}> {}", scope, sender, text.getString());
        TeamChatClientboundPacket packet = new TeamChatClientboundPacket(sender, displayName, scope, text);
        List<ServerPlayer> sentTo = recipients(server, sender, scope);
        for (ServerPlayer p : sentTo)
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), packet);
        // spectators follow every team's chat (players never see the other team's)
        if (scope == Scope.TEAM && isMatch()) {
            TeamChatClientboundPacket tagged = new TeamChatClientboundPacket(sender, displayName, Scope.TEAM_SPECTATED, text);
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                if (!sentTo.contains(p) && !PlayerRoles.roleOf(p.getName().getString()).inMatch)
                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), tagged);
        }
    }

    static List<ServerPlayer> recipients(MinecraftServer server, String sender, Scope scope) {
        List<ServerPlayer> list = new ArrayList<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String n = p.getName().getString();
            boolean add = switch (scope) {
                case ALL, LOBBY -> true;
                case TEAM -> n.equals(sender) || sameTeam(sender, n);
                case SPECTATOR -> !PlayerRoles.roleOf(n).inMatch;
                case TEAM_SPECTATED -> false; // only built by send() for spectators
            };
            if (add)
                list.add(p);
        }
        return list;
    }

    // same faction (commanders, co-commanders, soldiers) or allied factions, both still in the match
    public static boolean sameTeam(String a, String b) {
        if (!PlayerRoles.roleOf(a).inMatch || !PlayerRoles.roleOf(b).inMatch)
            return false;
        String fa = PlayerRoles.commanderOf(a);
        String fb = PlayerRoles.commanderOf(b);
        return fa.equals(fb) || AlliancesServerEvents.isAllied(fa, fb);
    }
}
