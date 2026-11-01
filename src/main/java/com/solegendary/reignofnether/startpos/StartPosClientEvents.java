package com.solegendary.reignofnether.startpos;

import javax.annotation.Nullable;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StartPosClientEvents {

    // client player is considered to have reserved a spot if selectedFaction != NONE && startPosIndex >= 0
    public static ArrayList<StartPos> startPoses = new ArrayList<>();
    public static Faction selectedFaction = Factions.NONE;
    public static boolean isStarting = false; // game is counting down to start
    // players on other teams whose seats the server hides (hideEnemyPicks): name -> ready
    public static final Map<String, Boolean> hiddenPlayers = new LinkedHashMap<>();
    public static final List<SeatJoin> joins = new ArrayList<>(); // soldiers and co-commanders this player may see
    public static final List<Faction> botFactions = new ArrayList<>(); // what the host can give an AI seat (BotPlans)

    public static boolean isEnabled() {
        return ClientGameModeHelper.gameMode == GameMode.CLASSIC && !startPoses.isEmpty();
    }

    public static void setPlayerReady(String playerName, boolean ready) {
        boolean changed = false;
        for (StartPos startPos : startPoses) {
            if (startPos.playerName.equals(playerName) && startPos.ready != ready) {
                startPos.ready = ready;
                changed = true;
            }
        }
        for (SeatJoin join : joins) {
            if (join.playerName.equals(playerName) && join.ready != ready) {
                join.ready = ready;
                changed = true;
            }
        }
        Boolean hiddenReady = hiddenPlayers.get(playerName);
        if (hiddenReady != null && hiddenReady != ready) {
            hiddenPlayers.put(playerName, ready);
            changed = true;
        }
        if (changed && MC.player != null) {
            if (ready) {
                MC.player.sendSystemMessage(SystemMessage.info(Component.translatable("startpos.reignofnether.player_ready",
                        playerName, getNumPlayersReady(), getNumEnabledPoses())));
            } else {
                MC.player.sendSystemMessage(SystemMessage.info(Component.translatable("startpos.reignofnether.player_not_ready", playerName)));
            }
        }
    }

    public static void setPosEnabled(BlockPos pos, boolean enable) {
        for (StartPos startPos : startPoses) {
            if (startPos.pos.equals(pos)) {
                startPos.enabled = enable;
                if (!startPos.enabled) {
                    startPos.playerName = "";
                    startPos.ready = false;
                    startPos.faction = Factions.NONE;
                }
            }
        }
    }

    // the singleplayer/P2P host or an op; the server checks this too
    public static boolean isHost() {
        return MC.hasSingleplayerServer() || (MC.player != null && MC.player.hasPermissions(2));
    }

    public static boolean hasReservedPos() {
        return getPos() != null;
    }

    // the local player's soldier/co-commander join, if any
    @Nullable
    public static SeatJoin getJoin() {
        for (SeatJoin join : joins)
            if (MC.player != null && MC.player.getName().getString().equals(join.playerName))
                return join;
        return null;
    }

    public static int getNumPlayersReady() {
        int readyPlayers = 0;
        for (StartPos startPose : startPoses)
            if (startPose.faction != Factions.NONE && startPose.ready && !startPose.playerName.isBlank())
                readyPlayers++;
        for (SeatJoin join : joins)
            if (join.ready)
                readyPlayers++;
        for (boolean ready : hiddenPlayers.values())
            if (ready)
                readyPlayers++;
        return readyPlayers;
    }
    public static int getNumEnabledPoses() {
        // hidden seats are counted with the seats, so hidden players beyond them are hidden joins
        int enabledPoses = (int) joins.stream().filter(j -> !j.pending).count() + hiddenPlayers.size();
        for (StartPos startPose : startPoses) {
            if (startPose.enabled)
                enabledPoses++;
            if (startPose.hidden)
                enabledPoses--;
        }
        return enabledPoses;
    }

    public static StartPos getPos() {
        for (StartPos startPos : startPoses)
            if (MC.player != null && MC.player.getName().getString().equals(startPos.playerName))
                return startPos;
        return null;
    }

    @SubscribeEvent
    public static void onChangeGamemode(PlayerEvent.PlayerChangeGameModeEvent evt) {
        StartPos startPos = getPos();
        if (evt.getEntity() == MC.player && startPos != null && MC.player != null) {
            selectedFaction = Factions.NONE;
            StartPosServerboundPacket.unreservePos(startPos.pos);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (!OrthoviewClientEvents.isEnabled() || MC.player == null)
            return;

        for (StartPos startPos : startPoses) {
            if (startPos.faction != Factions.NONE) {
                Building capitolBuilding = startPos.faction.getBuilding(startPos.faction.capitolBuilding);
                int forceColour = 2;
                if (startPos.playerName.equals(MC.player.getName().getString()))
                    forceColour = 1;
                if (capitolBuilding != null) {
                    ArrayList<BuildingBlock> blocksToDraw = capitolBuilding.getRelativeBlockData(MC.level);
                    Vec3i dimensions = BuildingUtils.getBuildingSize(blocksToDraw);
                    BlockPos bp = BuildingUtils.getBuildingOriginPos(startPos.pos, false, Rotation.NONE, dimensions);
                    BuildingClientEvents.drawBuilding(capitolBuilding.getRelativeBlockData(MC.level), evt.getPoseStack(), bp, forceColour);
                }
            }
        }
    }

    private static final Minecraft MC = Minecraft.getInstance();

    public static void resetAll() {
        selectedFaction = Factions.NONE;
        isStarting = false;
        hiddenPlayers.clear();
        joins.clear();
        for (StartPos startPos : startPoses)
            startPos.reset();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        // LOG OUT FROM SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer() != null && evt.getPlayer().getId() == MC.player.getId()) {
            resetAll();
            startPoses.clear();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent evt) {
        // LOG OUT FROM SINGLEPLAYER WORLD ONLY
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId()) {
            resetAll();
            startPoses.clear();
        }
    }
}
