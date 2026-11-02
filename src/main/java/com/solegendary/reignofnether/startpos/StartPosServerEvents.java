package com.solegendary.reignofnether.startpos;

import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.server.MinecraftServer;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.blocks.RTSStartBlock;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.soldier.SoldierServerEvents;
import com.solegendary.reignofnether.player.PlayerRole;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.faction.Factions;
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
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// manages start block and readied start (startRTSEveryone) actions

public class StartPosServerEvents {

    public static final int MAX_START_POSES = 16;

    public static ArrayList<StartPos> startPoses = new ArrayList<>();
    // lobby players without a seat of their own: soldiers and co-commanders
    public static final List<SeatJoin> joins = new ArrayList<>();
    // refused joins can't ask the same team again until then: player#colorId -> serverTicks
    private static final Map<String, Long> refusedUntil = new HashMap<>();
    private static final int REFUSED_TICKS = 10 * 20;
    private static long serverTicks = 0;

    private static int TICKS_TO_START_MAX = 100;
    private static int ticksToStart = TICKS_TO_START_MAX;
    private static boolean startingGame = false;

    private static int cullTicksMax = 100;
    private static int cullTicks = 0;

    public static boolean isStartingGame() {
        return startingGame;
    }

    public static void reset(ServerLevel serverLevel) {
        for (StartPos startPos : startPoses) {
            startPos.reset();
        }
        joins.clear();
        refusedUntil.clear();
        savePositions(serverLevel);
    }

    public static void setPlayerReady(String playerName, boolean ready) {
        SeatJoin asking = getJoin(playerName);
        if (asking != null && asking.pending)
            ready = false;
        boolean shouldStartGame = true;
        for (StartPos startPos : startPoses) {
            if (startPos.playerName.equals(playerName))
                startPos.ready = ready;
            if (startPos.enabled && (!startPos.ready || startPos.playerName.isBlank()))
                shouldStartGame = false;
        }
        for (SeatJoin join : joins) {
            if (join.playerName.equals(playerName))
                join.ready = ready;
            if (!join.ready && !join.pending)
                shouldStartGame = false;
        }
        if (shouldStartGame) {
            startGameCountdown();
        } else if (startingGame) {
            cancelStartGameCountdown(false);
        }
        if (ready)
            StartPosClientboundPacket.readyPlayer(playerName);
        else
            StartPosClientboundPacket.unreadyPlayer(playerName);
    }

    // hideEnemyPicks: a taken seat on another team is never sent to this player, only who sits there and if
    // they're ready. A player without a team yet (or a spectator) sees no taken seats.
    // viewerColor: the viewer's team, from their seat or their soldier/co-commander join
    static boolean isHiddenFrom(StartPos sp, @Nullable StartPos viewerSeat, @Nullable Integer viewerColor, boolean hidePicks) {
        if (!hidePicks || sp.playerName.isBlank() || sp == viewerSeat)
            return false;
        return viewerColor == null || viewerColor != sp.colorId;
    }

    static boolean isHiddenFrom(SeatJoin join, String viewerName, @Nullable Integer viewerColor, boolean hidePicks) {
        if (!hidePicks || join.playerName.equals(viewerName))
            return false;
        return viewerColor == null || viewerColor != join.colorId;
    }

    // send the whole seat list as this player may see it
    public static void syncTo(ServerPlayer player, boolean hidePicks) {
        // soldiers and co-commanders pick a seat by name, so these rules turn hiding off
        hidePicks &= !player.server.getGameRules().getBoolean(GameRuleRegistrar.CO_COMMANDERS) &&
                player.server.getGameRules().getInt(GameRuleRegistrar.SOLDIERS_PER_TEAM) == 0;
        String name = player.getName().getString();
        StartPos viewerSeat = startPoses.stream().filter(sp -> sp.playerName.equals(name)).findFirst().orElse(null);
        Integer viewerColor = viewerColor(name);
        StartPosClientboundPacket.clear(player);
        List<String> hidden = new ArrayList<>();
        Map<String, Boolean> hiddenReady = new HashMap<>();
        for (StartPos sp : startPoses) {
            StartPosAction action = sp.enabled ? StartPosAction.ADD : StartPosAction.ADD_DISABLED;
            boolean isHidden = isHiddenFrom(sp, viewerSeat, viewerColor, hidePicks);
            if (isHidden) {
                hidden.add(sp.playerName);
                hiddenReady.put(sp.playerName, sp.ready);
            }
            StartPos sent = sp;
            if (isHidden) {
                sent = new StartPos(sp.pos, sp.colorId);
                sent.hidden = true;
            }
            StartPosClientboundPacket.sendTo(player, action, sent);
        }
        for (SeatJoin join : joins) {
            if (isHiddenFrom(join, name, viewerColor, hidePicks)) {
                hidden.add(join.playerName);
                hiddenReady.put(join.playerName, join.ready);
            } else {
                StartPosClientboundPacket.sendJoin(player, join);
            }
        }
        // after all seats and sorted by name, so the order can't be matched back to a seat
        hidden.sort(Comparator.naturalOrder());
        for (String hiddenName : hidden)
            StartPosClientboundPacket.sendHiddenPlayer(player, hiddenName, hiddenReady.get(hiddenName));
    }

    // the player's team, from their seat or their join
    @Nullable
    private static Integer viewerColor(String playerName) {
        StartPos seat = startPoses.stream().filter(sp -> sp.playerName.equals(playerName)).findFirst().orElse(null);
        if (seat != null)
            return seat.colorId;
        SeatJoin join = getJoin(playerName);
        return join != null ? join.colorId : null;
    }

    @Nullable
    public static SeatJoin getJoin(String playerName) {
        for (SeatJoin join : joins)
            if (join.playerName.equals(playerName))
                return join;
        return null;
    }

    // play as a soldier on the team of the seat at seatPos, or as a co-commander sharing that seat.
    // Anyone who could answer has to accept first; a soldier on a team with nobody seated joins straight away
    public static void join(String playerName, PlayerRole role, BlockPos seatPos) {
        if (startingGame)
            return;
        StartPos seat = startPoses.stream().filter(sp -> sp.pos.equals(seatPos) && sp.enabled).findFirst().orElse(null);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (seat == null || server == null)
            return;
        if (role == PlayerRole.SOLDIER) {
            long teamSoldiers = joins.stream().filter(j -> j.isSoldier() && j.colorId == seat.colorId &&
                    !j.playerName.equals(playerName)).count();
            if (teamSoldiers >= server.getGameRules().getInt(GameRuleRegistrar.SOLDIERS_PER_TEAM))
                return;
        } else if (!canCoCommand(playerName, seat, server)) {
            return;
        }
        ServerPlayer asking = server.getPlayerList().getPlayerByName(playerName);
        long ticksLeft = refusedUntil.getOrDefault(refusalKey(playerName, seat.colorId), 0L) - serverTicks;
        if (ticksLeft > 0) {
            if (asking != null)
                asking.sendSystemMessage(SystemMessage.error(Component.translatable("startpos.reignofnether.join_refused_wait", (ticksLeft + 19) / 20)));
            return;
        }
        SeatJoin join = new SeatJoin(playerName, role, seat.colorId, role == PlayerRole.SOLDIER ? BlockPos.ZERO : seat.pos);
        join.pending = startPoses.stream().anyMatch(join::answeredBy);
        addJoin(join);
        if (join.pending)
            notifyRequest(server, join, asking);
    }

    private static boolean canCoCommand(String playerName, StartPos seat, MinecraftServer server) {
        return server.getGameRules().getBoolean(GameRuleRegistrar.CO_COMMANDERS) &&
                !seat.playerName.isBlank() && !seat.playerName.equals(playerName) &&
                joins.stream().noneMatch(j -> !j.isSoldier() && j.seat.equals(seat.pos) && !j.playerName.equals(playerName));
    }

    private static String refusalKey(String playerName, int colorId) {
        return playerName + "#" + colorId;
    }

    private static void notifyRequest(MinecraftServer server, SeatJoin join, @Nullable ServerPlayer asking) {
        for (StartPos sp : startPoses) {
            ServerPlayer holder = join.answeredBy(sp) ? server.getPlayerList().getPlayerByName(sp.playerName) : null;
            if (holder == null)
                continue;
            holder.sendSystemMessage(SystemMessage.info(Component.translatable(join.isSoldier() ?
                    "startpos.reignofnether.soldier_request" : "startpos.reignofnether.co_commander_request", join.playerName)));
            SoundClientboundPacket.playSoundForPlayer(SoundAction.CHAT, sp.playerName);
        }
        if (asking != null)
            asking.sendSystemMessage(SystemMessage.info(Component.translatable("startpos.reignofnether.join_requested")));
    }

    // a seat holder lets a pending join in, or sends a join away (pending or not); false if they can't answer it
    public static boolean answerJoin(ServerPlayer holder, String requester, boolean accept) {
        String holderName = holder.getName().getString();
        SeatJoin join = getJoin(requester);
        if (startingGame || join == null || (accept && !join.pending) ||
                startPoses.stream().noneMatch(sp -> sp.playerName.equals(holderName) && join.answeredBy(sp)))
            return false;
        if (accept) {
            join.pending = false;
            syncAll();
            setPlayerReady(requester, false);
        } else {
            refusedUntil.put(refusalKey(requester, join.colorId), serverTicks + REFUSED_TICKS);
            leaveJoin(requester);
        }
        ServerPlayer asking = holder.server.getPlayerList().getPlayerByName(requester);
        if (asking != null)
            asking.sendSystemMessage(accept ?
                    SystemMessage.info(Component.translatable("startpos.reignofnether.join_accepted", holderName)) :
                    SystemMessage.error(Component.translatable("startpos.reignofnether.join_refused", holderName)));
        return true;
    }

    // the seat holder (or the host) drops a co-commander: from the lobby, or mid-match back to spectating
    public static boolean kickCoCommander(ServerPlayer kicker, String target) {
        String kickerName = kicker.getName().getString();
        SeatJoin join = getJoin(target);
        if (join != null && !join.isSoldier()) {
            boolean holder = startPoses.stream().anyMatch(sp -> sp.pos.equals(join.seat) && sp.playerName.equals(kickerName));
            if (!holder && !isHost(kicker))
                return false;
            leaveJoin(target);
        } else {
            String owner = PlayerRoles.ownerOf(target);
            if (owner.equals(target) || (!owner.equals(kickerName) && !isHost(kicker)))
                return false; // not a co-commander, or not of the kicker's faction
            PlayerServerEvents.defeat(target, "");
        }
        ServerPlayer kicked = kicker.server.getPlayerList().getPlayerByName(target);
        if (kicked != null)
            kicked.sendSystemMessage(SystemMessage.error(Component.translatable("startpos.reignofnether.co_commander_kicked", kickerName)));
        return true;
    }

    private static void addJoin(SeatJoin join) {
        for (StartPos sp : startPoses)
            if (sp.playerName.equals(join.playerName))
                sp.reset();
        joins.removeIf(j -> j.playerName.equals(join.playerName));
        joins.add(join);
        trimJoins();
        setPlayerReady(join.playerName, false);
    }

    public static void leaveJoin(String playerName) {
        if (joins.removeIf(j -> j.playerName.equals(playerName))) {
            syncAll();
            setPlayerReady(playerName, false);
        }
    }

    // drop joins the rules or seats no longer allow: soldiers over the team cap, co-commanders on an empty seat,
    // requests nobody is left to answer
    public static void trimJoins() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return;
        int soldierCap = server.getGameRules().getInt(GameRuleRegistrar.SOLDIERS_PER_TEAM);
        boolean coCommanders = server.getGameRules().getBoolean(GameRuleRegistrar.CO_COMMANDERS);
        Map<Integer, Integer> soldiersByTeam = new HashMap<>();
        joins.removeIf(j -> {
            if (j.pending && startPoses.stream().noneMatch(j::answeredBy))
                return true;
            if (j.isSoldier())
                return soldiersByTeam.merge(j.colorId, 1, Integer::sum) > soldierCap ||
                        startPoses.stream().noneMatch(sp -> sp.enabled && sp.colorId == j.colorId);
            return !coCommanders || startPoses.stream().noneMatch(sp -> sp.enabled && sp.pos.equals(j.seat) && !sp.playerName.isBlank());
        });
        syncAll();
    }

    // after any seat change: who sees what depends on everyone's seat, so resend it all
    public static void syncAll(boolean hidePicks) {
        for (ServerPlayer player : PlayerServerEvents.players)
            syncTo(player, hidePicks);
    }

    public static void syncAll() {
        syncAll(hidePicks());
    }

    private static boolean hidePicks() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && server.getGameRules().getBoolean(GameRuleRegistrar.HIDE_ENEMY_PICKS);
    }

    // the singleplayer/Essential host or an op
    public static boolean isHost(ServerPlayer player) {
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static void setPosEnabled(BlockPos pos, boolean enable) {
        if (startingGame)
            return;
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
        syncAll();
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent evt) {
        if (evt.getPlacedBlock().getBlock() instanceof RTSStartBlock rtsStartBlock) {
            if (RTSMapInfoServerEvents.usingMapInfoStartPositions() && evt.getLevel().getServer() != null) {
                evt.getLevel().getServer().sendSystemMessage(Component.translatable("startpos.reignofnether.max_positions"));
            }
            if (startPoses.size() < MAX_START_POSES) {
                StartPos newStartPos = new StartPos(evt.getPos(),
                        rtsStartBlock.getMapColor(rtsStartBlock.defaultBlockState(),
                            evt.getLevel(), evt.getPos(),
                            rtsStartBlock.defaultMapColor()).id
                );
                startPoses.add(newStartPos);
                syncAll();
            } else {
                evt.setCanceled(true);
                for (Player player : PlayerServerEvents.players)
                    if (player.distanceToSqr(Vec3.atCenterOf(evt.getPos())) < 100)
                        player.sendSystemMessage(SystemMessage.error(Component.translatable("startpos.reignofnether.max_positions")));
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
        if (evt.getEntity() instanceof ServerPlayer player)
            syncTo(player, hidePicks());
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

    public static void startGameCountdown() {
        if (!startingGame) {
            ticksToStart = TICKS_TO_START_MAX;
            startingGame = true;
            StartPosClientboundPacket.startGameCountdown();
        }
    }

    public static void cancelStartGameCountdown(boolean noMsg) {
        if (startingGame) {
            ticksToStart = TICKS_TO_START_MAX;
            startingGame = false;
            StartPosClientboundPacket.cancelStartGameCountdown();
            if (!noMsg)
                PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.cancelled_start_game", true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        serverTicks++;

        if (startingGame) {
            if (ticksToStart % 20 == 0) {
                int secondsLeft = ticksToStart / 20;
                if (secondsLeft > 0) {
                    PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.starting_game", false, secondsLeft);
                    SoundClientboundPacket.playSoundForAllPlayers(SoundAction.CHAT);
                } else {
                    PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.started_game", true);
                    SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
                    FogOfWarServerEvents.setStartingPlayers(startPoses.stream()
                            .filter(sp -> !sp.playerName.isBlank() && sp.faction != Factions.NONE)
                            .map(sp -> sp.playerName).toList());
                    try {
                        for (ServerPlayer serverPlayer : PlayerServerEvents.players) {
                            for (StartPos startPos : startPoses) {
                                if (startPos.playerName.equals(serverPlayer.getName().getString()) && startPos.faction != Factions.NONE) {
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
                    } finally {
                        FogOfWarServerEvents.setStartingPlayers(List.of());
                    }
                    RTSMapInfoServerEvents.syncTeamMode(); // catches a /gamerule during the lobby
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
                    startJoins(evt.getServer());
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

    // after every seat has started: co-commanders take on their seat's faction, soldiers join their team's commander
    private static void startJoins(MinecraftServer server) {
        for (SeatJoin join : joins) {
            if (join.pending)
                continue; // never answered: they stay a spectator
            ServerPlayer player = server.getPlayerList().getPlayerByName(join.playerName);
            StartPos seat = startPoses.stream()
                    .filter(sp -> !sp.playerName.isBlank() && PlayerServerEvents.inRtsList(sp.playerName) &&
                            (join.isSoldier() ? sp.colorId == join.colorId : sp.pos.equals(join.seat)))
                    .findFirst().orElse(null);
            if (player == null)
                continue;
            if (seat == null) { // their team's commander never started
                player.sendSystemMessage(SystemMessage.error(Component.translatable("startpos.reignofnether.join_no_commander")));
                continue;
            }
            if (join.isSoldier()) {
                SoldierServerEvents.bind(player, seat.playerName, seat.pos);
            } else {
                PlayerRoles.setCoCommander(join.playerName, seat.playerName);
                PlayerClientboundPacket.teleport(join.playerName, seat.pos);
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
        RTSMapInfoServerEvents.syncTeamMode();
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
        syncAll();
    }

    // statics outlive the world: clear them so the next world opened in this game starts clean
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent evt) {
        startPoses.clear();
        joins.clear();
        refusedUntil.clear();
        startingGame = false;
        ticksToStart = TICKS_TO_START_MAX;
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        boolean shouldStopCountdown = joins.removeIf(j -> j.playerName.equals(evt.getEntity().getName().getString()));
        for (StartPos startPos : startPoses) {
            if (evt.getEntity() instanceof ServerPlayer player &&
                    startPos.playerName.equals(player.getName().getString())) {
                startPos.reset();
                shouldStopCountdown = true;
            }
        }
        if (shouldStopCountdown)
            trimJoins();
        if (shouldStopCountdown && startingGame) {
            cancelStartGameCountdown(false);
        }
    }
}
