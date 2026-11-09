package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.util.WorldState;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved game reopened mid-match waits behind the resume screen until the host resumes it.
 * In singleplayer that screen pauses the integrated server; a published one (LAN/P2P) is frozen by MinecraftServerMixin.
 * A player leaving a LAN/P2P match mid-game brings the same screen back.
 */
public class MatchResumeServerEvents {

    public static boolean resumePending = false;
    static {
        WorldState.onServerReset(() -> {
            matchEnded = false;
            resumePending = false;
        });
    }
    public static boolean matchEnded = false; // the end screen is up: nobody to wait for

    /** A player dropping out of a LAN/P2P match, maybe from a crash, freezes it behind the resume screen until the host resumes. */
    public static void onPlayerLeft(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (resumePending || matchEnded || server.isDedicatedServer() || server.isSingleplayerOwner(player.getGameProfile())
                || !WorldBorderServerEvents.isRtsOptimisedMap(server.overworld())
                || !PlayerServerEvents.inRtsList(player.getName().getString()))
            return;
        resumePending = true;
    }

    /** Shows (or refreshes) the resume screen for everyone online while the match waits: who is back changes it. */
    public static void sendScreenToAll(MinecraftServer server) {
        if (resumePending)
            for (ServerPlayer p : server.getPlayerList().getPlayers())
                sendScreen(p);
    }

    /** Shows the resume screen to one player. */
    public static void sendScreen(ServerPlayer player) {
        boolean fog = FogOfWarServerEvents.isEnabled();
        String viewer = player.getName().getString();
        List<MatchResumeClientboundPacket.Row> rows = new ArrayList<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers) {
                boolean bot = p.id < 0;
                boolean online = bot || player.server.getPlayerList().getPlayerByName(p.name) != null;
                // under fog, only your own and your allies' bases are shown
                BlockPos base = !fog || AlliancesServerEvents.isAlliedOrOwned(viewer, p.name) ? base(p.name) : null;
                rows.add(new MatchResumeClientboundPacket.Row(p.name, p.faction.key.toString(), p.startPosColorId, bot,
                        String.join(", ", AlliancesServerEvents.getAllAllies(p.name)), online,
                        base != null ? base.getX() : MatchResumeClientboundPacket.NO_BASE, base != null ? base.getZ() : 0));
            }
        }
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new MatchResumeClientboundPacket(
                true, player.server.getWorldData().getLevelName(), PlayerServerEvents.rtsGameTicks,
                StartPosServerEvents.isHost(player), rows, fog));
    }

    /** A player's base: their capitol, else their first building. */
    private static BlockPos base(String owner) {
        BlockPos first = null;
        for (BuildingPlacement b : BuildingServerEvents.getBuildings())
            if (owner.equals(b.ownerName)) {
                if (b.isCapitol)
                    return b.centrePos;
                if (first == null)
                    first = b.centrePos;
            }
        return first;
    }

    public static void resume(ServerPlayer host) {
        if (!resumePending || !StartPosServerEvents.isHost(host))
            return;
        resumePending = false;
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new MatchResumeClientboundPacket(false, "", 0, false, List.of(), false));
    }
}
