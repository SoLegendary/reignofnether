package com.solegendary.reignofnether.ping;

import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.minimap.MapMarkerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

// Every ping goes through here (from MapMarkerServerboundPacket): shows it to the sender, all connected allies
// and every spectator (no sound for them).
public class PingServerEvents {

    private static final int MAX_PINGS = 3;
    private static final long WINDOW_MS = 5000;
    private static final Map<String, Deque<Long>> recent = new HashMap<>();

    public static boolean ping(String sender, PingType type, BlockPos pos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !allow(sender, System.currentTimeMillis()))
            return false;
        // a spectator (e.g. a defeated player still allied to their old team) only reaches other spectators
        Set<String> team = new HashSet<>(Set.of(sender));
        if (!PlayerServerEvents.isGameActive() || PlayerServerEvents.isRTSPlayer(sender))
            team.addAll(AlliancesServerEvents.getAllAllies(sender)); // direct allies only, like before
        MapMarkerClientboundPacket packet = new MapMarkerClientboundPacket(pos.getX(), pos.getY(), pos.getZ(), sender, type);
        SoundAction sound = type == PingType.HELP || type == PingType.ENEMY ? SoundAction.ENEMY : SoundAction.ALLY;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String name = p.getName().getString();
            boolean ally = team.contains(name);
            if (!ally && PlayerServerEvents.isRTSPlayer(name))
                continue; // enemies never see it; spectators see everyone's
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), packet);
            if (ally)
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p),
                        new SoundClientboundPacket(sound, BlockPos.ZERO, "", 1.0f, -1));
        }
        return true;
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
