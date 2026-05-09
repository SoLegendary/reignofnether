package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.blocks.RTSStartBlock;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.rtsmap.RTSMapInfo;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// manages start block and readied start (startRTSEveryone) actions

public class StartPosServerEvents {

    public static final int MAX_START_POSES = 16;

    public static ArrayList<StartPos> startPoses = new ArrayList<>();

    private static int TICKS_TO_START_MAX = 100;
    private static int ticksToStart = TICKS_TO_START_MAX;
    private static boolean startingGame = false;

    // Ready-check state
    private static boolean readyCheckActive = false;

    private static int cullTicksMax = 100;
    private static int cullTicks = 0;

    public static boolean isReadyCheckActive() {
        return readyCheckActive;
    }

    public static void reset(ServerLevel serverLevel) {
        for (StartPos startPos : startPoses) {
            startPos.reset();
        }
        readyCheckActive = false;
        savePositions(serverLevel);
    }

    /**
     * Check if all reserved players are ready.
     * Returns true if there is at least one reserved player and all reserved players are ready.
     */
    public static boolean areAllPlayersReady() {
        boolean hasReservedPlayer = false;
        for (StartPos startPos : startPoses) {
            if (startPos.faction != Faction.NONE) {
                hasReservedPlayer = true;
                if (!startPos.ready) {
                    return false;
                }
            }
        }
        return hasReservedPlayer;
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent evt) {
        if (evt.getPlacedBlock().getBlock() instanceof RTSStartBlock rtsStartBlock) {
            if (RTSMapInfoServerEvents.usingMapInfoStartPositions() && evt.getLevel().getServer() != null) {
                evt.getLevel().getServer().sendSystemMessage(Component.translatable("startpos.reignofnether.max_positions"));
            }
            if (startPoses.size() < MAX_START_POSES) {
                StartPos newStartPos = new StartPos(evt.getPos(), rtsStartBlock.defaultMapColor().id);
                startPoses.add(newStartPos);
                StartPosClientboundPacket.addPos(newStartPos);
            } else {
                evt.setCanceled(true);
                for (Player player : PlayerServerEvents.players)
                    if (player.distanceToSqr(Vec3.atCenterOf(evt.getPos())) < 100)
                        player.sendSystemMessage(Component.translatable("startpos.reignofnether.max_positions"));
            }
            if (evt.getLevel() instanceof ServerLevel serverLevel)
                savePositions(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (startPoses.removeIf(sp -> {
            if (sp.pos.equals(evt.getPos())) {
                StartPosClientboundPacket.removePos(evt.getPos());
                return true;
            }
            return false;
        }) && (evt.getLevel() instanceof ServerLevel serverLevel)) {
            savePositions(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        for (StartPos startPos : startPoses)
            StartPosClientboundPacket.addPos(startPos);
        // Sync ready-check state to newly joined player
        if (readyCheckActive) {
            StartPosClientboundPacket.readyCheckStarted();
        }
    }

    private static void cullInvalidPoses(ServerLevel serverLevel) {
        if (startPoses.removeIf(sp -> {
            if (sp.isFromStartBlock && !(serverLevel.getBlockState(sp.pos).getBlock() instanceof RTSStartBlock)) {
                StartPosClientboundPacket.removePos(sp.pos);
                return true;
            }
            return false;
        })) {
            savePositions(serverLevel);
        }
    }

    /**
     * Called when a player initiates the start game process.
     * Instead of immediately starting the countdown, this begins a ready-check phase
     * where all reserved players must signal ready before the countdown begins.
     */
    public static void startGameCountdown() {
        if (!startingGame && !readyCheckActive) {
            readyCheckActive = true;
            // Clear any previous ready states
            for (StartPos startPos : startPoses) {
                startPos.ready = false;
            }
            StartPosClientboundPacket.readyCheckStarted();
            PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.ready_check_started", true);
            // Check if all players are already ready (edge case: no reserved players)
            checkReadyAndStartCountdown();
        }
    }

    /**
     * Toggle the ready state of a player at a given position.
     * If all reserved players become ready, start the countdown.
     * If a player un-readies, cancel any active countdown.
     */
    public static void toggleReady(String playerName) {
        for (StartPos startPos : startPoses) {
            if (startPos.playerName.equals(playerName) && startPos.faction != Faction.NONE) {
                startPos.ready = !startPos.ready;
                StartPosClientboundPacket.syncReadyState(startPos.pos, startPos.ready);
                if (startPos.ready) {
                    PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.player_ready", true, playerName);
                    checkReadyAndStartCountdown();
                } else {
                    PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.player_not_ready", true, playerName);
                    // If countdown was active, cancel it
                    if (startingGame) {
                        cancelStartGameCountdown(false);
                    }
                }
                break;
            }
        }
    }

    /**
     * Check if all reserved players are ready, and if so, start the countdown.
     */
    private static void checkReadyAndStartCountdown() {
        if (!readyCheckActive) return;
        if (areAllPlayersReady()) {
            startCountdownAfterReadyCheck();
        }
    }

    /**
     * Begins the actual countdown after all players are ready.
     */
    private static void startCountdownAfterReadyCheck() {
        if (!startingGame) {
            ticksToStart = TICKS_TO_START_MAX;
            startingGame = true;
            readyCheckActive = false; // ready-check phase complete
            StartPosClientboundPacket.startGameCountdown();
        }
    }

    public static void cancelStartGameCountdown(boolean noMsg) {
        if (startingGame || readyCheckActive) {
            ticksToStart = TICKS_TO_START_MAX;
            startingGame = false;
            readyCheckActive = false;
            // Clear all ready states
            for (StartPos startPos : startPoses) {
                startPos.ready = false;
            }
            StartPosClientboundPacket.cancelStartGameCountdown();
            StartPosClientboundPacket.readyCheckCancelled();
            if (!noMsg)
                PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.cancelled_start_game", true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        if (startingGame) {
            if (ticksToStart % 20 == 0) {
                int secondsLeft = ticksToStart / 20;
                if (secondsLeft > 0) {
                    PlayerServerEvents.sendMessageToAllPlayersNoNewlines("startpos.reignofnether.starting_game", false, secondsLeft);
                    SoundClientboundPacket.playSoundForAllPlayers(SoundAction.CHAT);
                } else {
                    PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.started_game", true);
                    SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
                    for (ServerPlayer serverPlayer : PlayerServerEvents.players) {
                        for (StartPos startPos : startPoses) {
                            if (startPos.playerName.equals(serverPlayer.getName().getString()) && startPos.faction != Faction.NONE) {
                                PlayerServerEvents.startRTS(
                                        serverPlayer.getId(),
                                        new Vec3(startPos.pos.getX(), startPos.pos.getY(), startPos.pos.getZ()),
                                        startPos.faction,
                                        startPos.colorId
                                );
                                break;
                            }
                        }
                    }
                    // ally all players who start at the same color of start block
                    for (int i = 0; i < PlayerServerEvents.rtsPlayers.size(); i++) {
                        RTSPlayer p1 = PlayerServerEvents.rtsPlayers.get(i);
                        for (int j = i + 1; j < PlayerServerEvents.rtsPlayers.size(); j++) {
                            RTSPlayer p2 = PlayerServerEvents.rtsPlayers.get(j);
                            if (p1.startPosColorId == p2.startPosColorId) {
                                AlliancesServerEvents.addAlliance(p1.name, p2.name);
                            }
                        }
                    }
                    PlayerServerEvents.setRTSLock(true, true);
                    StartPosServerEvents.reset(evt.getServer().getLevel(Level.OVERWORLD));
                    StartPosClientboundPacket.reset();
                    startingGame = false;
                }
            }
            if (ticksToStart >= 0)
                ticksToStart -= 1;
        }
        else if (!(PlayerServerEvents.isGameActive())) {
            cullTicks += 1;
            if (cullTicks >= cullTicksMax) {
                cullTicks = 0;
                cullInvalidPoses(evt.getServer().getLevel(Level.OVERWORLD));
            }
        }
    }

    public static void savePositions(ServerLevel serverLevel) {
        if (!RTSMapInfoServerEvents.usingMapInfoStartPositions()) {
            StartPosSaveData startPosData = StartPosSaveData.getInstance(serverLevel);
            startPosData.startPoses.clear();
            startPosData.startPoses.addAll(startPoses);
            startPosData.save();
            serverLevel.getDataStorage().save();
            //ReignOfNether.LOGGER.info("saved " + startPoses.size() + " start positions in serverevents");
        }
    }

    @SubscribeEvent
    public static void loadPositions(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        // rtsMapInfo is read in RTSMapInfoServerEvents.loadInfo
        if (level != null) {
            if (RTSMapInfoServerEvents.rtsMapInfo == null) {
                StartPosSaveData startPosData = StartPosSaveData.getInstance(level);
                startPoses.clear();
                startPoses.addAll(startPosData.startPoses);
                ReignOfNether.LOGGER.info("loaded " + startPoses.size() + " start positions in serverevents from save");
            } else {
                loadPositionsFromMapInfo();
                ReignOfNether.LOGGER.info("loaded " + startPoses.size() + " start positions from json file");
            }
        }
    }

    public static void loadPositionsFromMapInfo() {
        if (!RTSMapInfoServerEvents.usingMapInfoStartPositions())
            return;
        int playerColorIndex = 0;
        List<StartPos> oldStartPoses = new ArrayList<>(startPoses);
        startPoses.clear();
        for (List<BlockPos> teamStartPoses : RTSMapInfoServerEvents.rtsMapInfo.getTeams()) {
            for (BlockPos startBlockPos : teamStartPoses) {
                StartPos startPos = new StartPos(startBlockPos, PlayerColors.colors[playerColorIndex].hexCode);
                startPos.isFromStartBlock = false;
                startPoses.add(startPos);
            }
            playerColorIndex += 1;
            if (playerColorIndex >= PlayerColors.colors.length)
                playerColorIndex = 0;
        }
        Set<BlockPos> newBlockPoses = startPoses.stream()
                .map(sp -> sp.pos)
                .collect(Collectors.toSet());
        for (StartPos oldStartPos : oldStartPoses)
            if (!newBlockPoses.contains(oldStartPos.pos))
                StartPosClientboundPacket.removePos(oldStartPos.pos);
        for (StartPos startPos : startPoses)
            StartPosClientboundPacket.addPos(startPos);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        for (StartPos startPos : startPoses) {
            if (evt.getEntity() instanceof ServerPlayer player &&
                    startPos.playerName.equals(player.getName().getString())) {
                StartPosClientboundPacket.unreservePos(startPos.pos);
                startPos.reset();
                // If a player leaves during ready-check, cancel the ready-check
                if (readyCheckActive) {
                    cancelStartGameCountdown(false);
                }
            }
        }
    }
}