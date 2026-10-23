package com.solegendary.reignofnether.ping;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.minimap.MapMarkerClientboundPacket;
import com.solegendary.reignofnether.chat.TeamChatServerEvents;
import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.soldier.SoldierServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

// Every ping goes through here (from MapMarkerServerboundPacket): shows it to the sender's team (commanders,
// co-commanders, soldiers and allies) and every spectator (no sound for them). A commander's attack or defend
// ping also becomes their soldiers' current order.
public class PingServerEvents {

    private static final int MAX_PINGS = 3;
    private static final long WINDOW_MS = 5000;
    private static final Map<String, Deque<Long>> recent = new HashMap<>();

    public static boolean ping(String sender, PingType type, BlockPos pos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !allow(sender, System.currentTimeMillis()))
            return false;
        boolean inGame = PlayerServerEvents.isGameActive();
        SoundAction sound = type == PingType.HELP || type == PingType.ENEMY ? SoundAction.ENEMY : SoundAction.ALLY;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String name = p.getName().getString();
            // a spectator (e.g. a defeated player still allied to their old team) only reaches other spectators
            boolean ally = name.equals(sender) || (inGame ? TeamChatServerEvents.sameTeam(sender, name)
                    : AlliancesServerEvents.isAllied(sender, name));
            if (!ally && PlayerRoles.roleOf(name).inMatch)
                continue; // enemies never see it; spectators see everyone's
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), new MapMarkerClientboundPacket(
                    pos.getX(), pos.getY(), pos.getZ(), sender, type, ally ? Scope.TEAM : Scope.TEAM_SPECTATED));
            if (ally)
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p),
                        new SoundClientboundPacket(sound, BlockPos.ZERO, "", 1.0f, -1));
        }
        if (PlayerRoles.roleOf(sender) == PlayerRole.COMMANDER)
            SoldierServerEvents.setOrder(PlayerRoles.ownerOf(sender), type, pos);
        return true;
    }

    public static synchronized void forget(String sender) {
        recent.remove(sender);
    }

    // rate limit: MAX_PINGS per WINDOW_MS per sender
    static synchronized boolean allow(String sender, long now) {
        Deque<Long> times = recent.computeIfAbsent(sender, k -> new ArrayDeque<>());
        while (!times.isEmpty() && now - times.peekFirst() > WINDOW_MS)
            times.pollFirst();
        if (times.size() >= MAX_PINGS)
            return false;
        times.addLast(now);
        return true;
    }
}
