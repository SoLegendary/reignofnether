package com.solegendary.reignofnether.rtsmap;

import net.minecraftforge.server.ServerLifecycleHooks;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.util.WorldState;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RTSMapInfoServerEvents {

    public static RTSMapInfo rtsMapInfo = null;
    private static boolean lockedByTeamMode = false;
    static {
        WorldState.onServerReset(() -> lockedByTeamMode = false);
    }

    public static boolean usingMapInfoStartPositions() {
        return rtsMapInfo != null && rtsMapInfo.supportsMode(rtsMapInfo.getDefaultMode());
    }

    public static boolean isTeamMode() {
        return usingMapInfoStartPositions() && rtsMapInfo.isTeamMode();
    }

    // a team mode keeps its teams, so alliances stay locked, then unlock when it ends (the host can relock them);
    // tells clients so they can show it
    public static void syncTeamMode() {
        boolean teamMode = isTeamMode();
        RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_TEAM_MODE, String.valueOf(teamMode));
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && (teamMode || lockedByTeamMode)) {
            server.getGameRules().getRule(GameRuleRegistrar.LOCK_ALLIANCES).set(teamMode, server);
            GameruleClientboundPacket.setLockAlliances(teamMode);
        }
        lockedByTeamMode = teamMode;
    }


    @SubscribeEvent
    public static void loadInfo(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        // read from rtsmap.json, if empty, then ignore saved startPoses and start blocks
        if (level != null) {
            rtsMapInfo = RTSMapInfoLoader.load(level);
            if (rtsMapInfo != null) {
                ReignOfNether.LOGGER.info("Loaded rtsMapInfo: " +
                        rtsMapInfo.getName() + '|' +
                        rtsMapInfo.getDescription() + '|' +
                        rtsMapInfo.getVersion() + '|' +
                        rtsMapInfo.getAuthor());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (rtsMapInfo != null) {
            // the loading screen shows the name, authors, version and description, so chat stays clear
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MAP_NAME, rtsMapInfo.getName());
            for (String author : rtsMapInfo.getAuthor())
                RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.ADD_AUTHOR, author);
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_DESCRIPTION, rtsMapInfo.getDescription());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_MODE, rtsMapInfo.getDefaultMode());
            RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_VERSION, rtsMapInfo.getVersion());
            syncTeamMode();
            for (String mode : rtsMapInfo.getModes().keySet())
                RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.ADD_MODE, mode);
        }
    }
}
