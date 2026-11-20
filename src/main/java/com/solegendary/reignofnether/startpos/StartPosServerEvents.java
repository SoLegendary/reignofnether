package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ai.BotDifficulty;
import com.solegendary.reignofnether.ai.BotPlans;
import com.solegendary.reignofnether.ai.BotServerEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.util.RandomSource;

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
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesClientboundPacket;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfo;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.guide.GuideServer;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// manages start block and readied start (startRTSEveryone) actions

public class StartPosServerEvents {

    public static final int MAX_START_POSES = 16;
    public static final String BLOCKS_MODE = "start_blocks";

    public static ArrayList<StartPos> startPoses = WorldState.server(new ArrayList<>());
    // lobby players without a seat of their own: soldiers and co-commanders
    public static final List<SeatJoin> joins = WorldState.server(new ArrayList<>());
    // refused joins can't ask the same team again until then: player#colorId -> serverTicks
    private static final Map<String, Long> refusedUntil = WorldState.server(new HashMap<>());
    private static final int REFUSED_TICKS = 10 * 20;
    private static long serverTicks = 0;

    private static int TICKS_TO_START_MAX = 100;
    private static int ticksToStart = TICKS_TO_START_MAX;
    static {
        WorldState.onServerReset(() -> {
            startingGame = false;
            ticksToStart = TICKS_TO_START_MAX;
        });
    }
    private static boolean startingGame = false;

    // map.json seat name -> who started the match in it, so commands can name a seat (seatOccupant); saved in RTSPlayerSaveData
    public static final Map<String, String> seatOccupants = WorldState.server(new HashMap<>());


    public static boolean isStartingGame() {
        return startingGame;
    }

    public static void reset(ServerLevel serverLevel) {
        for (StartPos startPos : startPoses) {
            startPos.reset();
        }
        joins.clear();
        refusedUntil.clear();
    }

    public static void setPlayerReady(String playerName, boolean ready) {
        SeatJoin asking = getJoin(playerName);
        if (asking != null && asking.pending)
            ready = false;
        // an old or newer format map can't be played: nobody readies, bots included
        Component refusal = ready ? RTSMapInfoServerEvents.playRefusal() : null;
        if (refusal != null) {
            ready = false;
            for (ServerPlayer player : PlayerServerEvents.players)
                if (player.getName().getString().equals(playerName))
                    player.sendSystemMessage(SystemMessage.error(refusal));
        }
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
        // seats can all be bots, but someone has to be here to watch
        if (PlayerServerEvents.players.isEmpty())
            shouldStartGame = false;
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
        return viewerColor == null || viewerColor != team(sp);
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
        for (Faction f : BotPlans.available())
            StartPosClientboundPacket.sendBotFaction(player, f);
        boolean host = isHost(player);
        List<String> hidden = new ArrayList<>();
        Map<String, Boolean> hiddenReady = new HashMap<>();
        for (StartPos sp : startPoses) {
            StartPosAction action = sp.enabled ? StartPosAction.ADD : StartPosAction.ADD_DISABLED;
            boolean isHidden = !(host && sp.isBot()) && isHiddenFrom(sp, viewerSeat, viewerColor, hidePicks); // the host runs the bots
            if (isHidden) {
                hidden.add(sp.playerName);
                hiddenReady.put(sp.playerName, sp.ready);
            }
            StartPos sent = sp;
            if (isHidden) {
                sent = new StartPos(sp.pos, sp.colorId);
                sent.team = sp.team;
                sent.hidden = true;
                sent.seatName = sp.seatName;
                sent.fixedFaction = sp.fixedFaction;
                sent.mapBot = sp.mapBot;
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

    static int team(StartPos sp) {
        return StartPos.teamOf(sp, startPoses);
    }

    // on maps without set teams: the host can move any open seat, a player only their own. Every seat's number is
    // fixed first, so moving one never renumbers the others
    public static void setTeam(ServerPlayer player, BlockPos pos, int team) {
        if (startingGame || PlayerServerEvents.isGameActive() || RTSMapInfoServerEvents.isTeamMode()
                || team < 1 || team > StartPos.maxTeams(startPoses))
            return;
        StartPos seat = startPoses.stream().filter(sp -> sp.pos.equals(pos) && sp.enabled).findFirst().orElse(null);
        if (seat == null || (!isHost(player) && !seat.playerName.equals(player.getName().getString())))
            return;
        Map<StartPos, Integer> numbers = StartPos.teamNumbers(startPoses);
        for (StartPos sp : startPoses)
            sp.team = numbers.get(sp);
        seat.team = team;
        if (!seat.playerName.isBlank() && !seat.isBot())
            setPlayerReady(seat.playerName, false); // nobody starts on a team they didn't see
        trimJoins(); // soldiers of a team left empty go, and everyone gets the new teams
    }

    // the player's team, from their seat or their join
    @Nullable
    private static Integer viewerColor(String playerName) {
        StartPos seat = startPoses.stream().filter(sp -> sp.playerName.equals(playerName)).findFirst().orElse(null);
        if (seat != null)
            return team(seat);
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
            long teamSoldiers = joins.stream().filter(j -> j.isSoldier() && j.colorId == team(seat) &&
                    !j.playerName.equals(playerName)).count();
            if (teamSoldiers >= server.getGameRules().getInt(GameRuleRegistrar.SOLDIERS_PER_TEAM))
                return;
        } else if (!canCoCommand(playerName, seat, server)) {
            return;
        }
        ServerPlayer asking = server.getPlayerList().getPlayerByName(playerName);
        long ticksLeft = refusedUntil.getOrDefault(refusalKey(playerName, team(seat)), 0L) - serverTicks;
        if (ticksLeft > 0) {
            if (asking != null)
                asking.sendSystemMessage(SystemMessage.error(Component.translatable("startpos.reignofnether.join_refused_wait", (ticksLeft + 19) / 20)));
            return;
        }
        SeatJoin join = new SeatJoin(playerName, role, team(seat), role == PlayerRole.SOLDIER ? BlockPos.ZERO : seat.pos);
        join.pending = startPoses.stream().anyMatch(sp -> join.answeredBy(sp, startPoses));
        addJoin(join);
        if (join.pending)
            notifyRequest(server, join, asking);
    }

    private static boolean canCoCommand(String playerName, StartPos seat, MinecraftServer server) {
        return server.getGameRules().getBoolean(GameRuleRegistrar.CO_COMMANDERS) &&
                !seat.playerName.isBlank() && !seat.playerName.equals(playerName) && !seat.isBot() &&
                joins.stream().noneMatch(j -> !j.isSoldier() && j.seat.equals(seat.pos) && !j.playerName.equals(playerName));
    }

    private static String refusalKey(String playerName, int colorId) {
        return playerName + "#" + colorId;
    }

    private static void notifyRequest(MinecraftServer server, SeatJoin join, @Nullable ServerPlayer asking) {
        for (StartPos sp : startPoses) {
            ServerPlayer holder = join.answeredBy(sp, startPoses) ? server.getPlayerList().getPlayerByName(sp.playerName) : null;
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
                startPoses.stream().noneMatch(sp -> sp.playerName.equals(holderName) && join.answeredBy(sp, startPoses)))
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
            if (j.pending && startPoses.stream().noneMatch(sp -> j.answeredBy(sp, startPoses)))
                return true;
            if (j.isSoldier())
                return soldiersByTeam.merge(j.colorId, 1, Integer::sum) > soldierCap ||
                        startPoses.stream().noneMatch(sp -> sp.enabled && team(sp) == j.colorId);
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

    // ---------------------------------------------------------------- AI seats

    private static final int BOT_READY_TICKS = 5 * 20;
    private static final Map<String, Long> botReadyAt = WorldState.server(new HashMap<>()); // a bot readies up a moment after a change

    // the host puts a bot on an empty seat, or changes a bot seat's faction or difficulty. A faction whose AI is
    // in maintenance (BotPlans) can't be picked; RANDOM picks among the available ones at match start
    public static void reserveBotPos(BlockPos pos, Faction faction, int botDifficulty) {
        if (startingGame || BotPlans.available().isEmpty() || !BotServerEvents.botsAvailable())
            return;
        if (faction != Factions.RANDOM && !BotPlans.isAvailable(faction))
            faction = Factions.RANDOM;
        for (StartPos startPos : startPoses) {
            if (!startPos.pos.equals(pos) || !startPos.enabled)
                continue;
            if (!startPos.playerName.isBlank() && !startPos.isBot())
                return; // never kick a human
            if (!startPos.isBot())
                startPos.playerName = BotServerEvents.getUnusedBotName();
            if (startPos.fixedFaction != Factions.NONE)
                faction = startPos.fixedFaction;
            startPos.faction = faction;
            startPos.botDifficulty = BotDifficulty.fromOrdinal(botDifficulty).ordinal();
            syncAll();
            setPlayerReady(startPos.playerName, false);
            botReadyAt.put(startPos.playerName, serverTicks + BOT_READY_TICKS);
            return;
        }
    }

    // the gamerule was switched off in the lobby: bots can't play this match, their seats open up again
    private static void dropUnavailableBots() {
        if (!PlayerServerEvents.rtsPlayers.isEmpty() || startPoses.stream().noneMatch(StartPos::isBot) || BotServerEvents.botsAvailable())
            return;
        for (StartPos startPos : startPoses)
            if (startPos.isBot()) {
                String seated = startPos.playerName;
                startPos.reset();
                setPlayerReady(seated, false);
            }
        trimJoins();
        syncAll();
    }

    // map.json "bot" seats get their bot back in every lobby, and stay closed while bots can't play here
    private static void fillMapBots() {
        if (startingGame || !PlayerServerEvents.rtsPlayers.isEmpty())
            return;
        boolean available = BotServerEvents.botsAvailable() && !BotPlans.available().isEmpty();
        for (StartPos sp : startPoses) {
            if (!sp.mapBot || (sp.isBot() == available && sp.enabled == available))
                continue;
            if (available) {
                sp.enabled = true;
                reserveBotPos(sp.pos, Factions.RANDOM, BotDifficulty.NORMAL.ordinal());
            } else {
                sp.reset();
                sp.enabled = false;
                syncAll();
            }
        }
    }

    private static void readyBots() {
        if (botReadyAt.isEmpty())
            return;
        for (var e : List.copyOf(botReadyAt.entrySet())) {
            if (serverTicks < e.getValue())
                continue;
            botReadyAt.remove(e.getKey());
            // only if the bot still holds its seat (the host may have removed it meanwhile)
            if (!startingGame && startPoses.stream().anyMatch(sp -> sp.isBot() && sp.playerName.equals(e.getKey())))
                setPlayerReady(e.getKey(), true);
        }
    }

    private static void startBot(StartPos seat, RandomSource random) {
        Faction faction = BotPlans.resolve(seat.faction, random);
        if (faction == Factions.NONE)
            return; // every faction's AI went into maintenance meanwhile
        PlayerServerEvents.startRTSBot(seat.playerName, new Vec3(seat.pos.getX(), seat.pos.getY(), seat.pos.getZ()), faction, seat.colorId);
        BotServerEvents.addBot(seat.playerName, BotDifficulty.fromOrdinal(seat.botDifficulty));
    }

    // the singleplayer/P2P host or an op
    public static boolean isHost(ServerPlayer player) {
        return PlayerServerEvents.isOpOrHost(player);
    }

    public static void setPosEnabled(BlockPos pos, boolean enable) {
        if (startingGame)
            return;
        for (StartPos startPos : startPoses) {
            if (startPos.pos.equals(pos) && !startPos.mapBot) {
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

    // ron/map.json is the map itself: ops, or the host of a singleplayer world, may change it
    public static boolean canEditMap(@Nullable Entity entity) {
        return entity instanceof ServerPlayer player && PlayerServerEvents.isOpOrHost(player);
    }

    // a start block is a seat in ron/map.json (a world without one gets one now), so only a mapmaker may place it
    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent evt) {
        if (!(evt.getPlacedBlock().getBlock() instanceof RTSStartBlock) || !(evt.getLevel() instanceof ServerLevel serverLevel))
            return;
        if (refuseMapEdit(evt.getEntity())) {
            evt.setCanceled(true);
            return;
        }
        RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo != null ? RTSMapInfoServerEvents.rtsMapInfo : new RTSMapInfo();
        if (info.getSeat(evt.getPos()) != null)
            return;
        if (info.getSeats().size() >= MAX_START_POSES) {
            evt.setCanceled(true);
            if (evt.getEntity() instanceof ServerPlayer player)
                player.sendSystemMessage(SystemMessage.error(Component.translatable("startpos.reignofnether.max_positions")));
            return;
        }
        info.getSeats().add(new RTSMapInfo.Seat(nextSeatId(info), evt.getPos()));
        groupBlockSeats(info, serverLevel);
        RTSMapInfoServerEvents.setAndSave(serverLevel.getServer(), info);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent evt) {
        if (!(evt.getState().getBlock() instanceof RTSStartBlock) || !(evt.getLevel() instanceof ServerLevel serverLevel))
            return;
        if (refuseMapEdit(evt.getPlayer())) {
            evt.setCanceled(true);
            return;
        }
        RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo;
        RTSMapInfo.Seat seat = info != null ? info.getSeat(evt.getPos()) : null;
        if (seat == null)
            return;
        // the block's seat goes, and with it its place in every mode
        info.getSeats().remove(seat);
        info.getModes().replaceAll((mode, teams) -> teams.stream()
                .map(team -> team.stream().filter(id -> id != seat.id).collect(Collectors.toList()))
                .collect(Collectors.toList()));
        groupBlockSeats(info, serverLevel);
        RTSMapInfoServerEvents.setAndSave(serverLevel.getServer(), info);
    }

    // start blocks change ron/map.json: only for ops or the singleplayer host, and never on an old or newer format map
    private static boolean refuseMapEdit(@Nullable Entity entity) {
        Component refusal = RTSMapInfoServerEvents.writeRefusal();
        if (refusal == null && canEditMap(entity))
            return false;
        if (refusal != null && entity instanceof ServerPlayer player)
            player.sendSystemMessage(SystemMessage.error(refusal));
        return true;
    }

    private static boolean hasMapSeats() {
        return RTSMapInfoServerEvents.rtsMapInfo != null && !RTSMapInfoServerEvents.rtsMapInfo.getSeats().isEmpty();
    }

    public static int nextSeatId(RTSMapInfo info) {
        int id = 1;
        while (info.getSeat(id) != null)
            id++;
        return id;
    }

    // block colour = team: kept as the "start_blocks" mode, made once two blocks share a colour on a map without modes
    // of its own
    public static void groupBlockSeats(RTSMapInfo info, ServerLevel level) {
        if (!info.getModes().isEmpty() && !info.getDefaultMode().equals(BLOCKS_MODE))
            return;
        Map<Integer, List<Integer>> teams = new LinkedHashMap<>();
        for (RTSMapInfo.Seat seat : info.getSeats()) {
            BlockPos pos = seat.toBlockPos();
            int team = level.getBlockState(pos).getBlock() instanceof RTSStartBlock block ?
                    block.getMapColor(block.defaultBlockState(), level, pos, block.defaultMapColor()).id : -1 - seat.id;
            teams.computeIfAbsent(team, t -> new ArrayList<>()).add(seat.id);
        }
        if (info.getDefaultMode().equals(BLOCKS_MODE) || teams.values().stream().anyMatch(team -> team.size() > 1)) {
            info.getModes().put(BLOCKS_MODE, new ArrayList<>(teams.values()));
            info.setDefaultMode(BLOCKS_MODE);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player) {
            syncTo(player, hidePicks());
        }
    }

    private static boolean autoStart() {
        return RTSMapInfoServerEvents.rtsMapInfo != null && RTSMapInfoServerEvents.rtsMapInfo.isAutoStart();
    }

    // map.json "autoStart" (solo maps): no lobby, the host takes the first open seat and the match starts at once,
    // once their loading screen is done (GuideServer.ready). Not once a match or the editor runs (a saved world reopens in it)
    public static void autoStart(ServerPlayer player) {
        if (!autoStart() || !player.server.isSingleplayer() || startingGame || !PlayerServerEvents.rtsPlayers.isEmpty() || RTSMapInfoServerEvents.playRefusal() != null)
            return;
        String name = player.getName().getString();
        StartPos seat = startPoses.stream().filter(sp -> sp.enabled && sp.playerName.isBlank()).findFirst().orElse(null);
        if (seat == null)
            return;
        seat.faction = seat.fixedFaction != Factions.NONE ? seat.fixedFaction : Factions.RANDOM;
        seat.playerName = name;
        for (StartPos sp : startPoses) // other open player seats stay empty; map bots still start (readyBots)
            if (sp != seat && !sp.mapBot && sp.playerName.isBlank())
                sp.enabled = false;
        syncAll();
        setPlayerReady(name, true);
    }

    public static void startGameCountdown() {
        Component refusal = RTSMapInfoServerEvents.playRefusal();
        if (refusal != null) {
            for (ServerPlayer player : PlayerServerEvents.players)
                player.sendSystemMessage(SystemMessage.error(refusal));
            return;
        }
        if (!startingGame) {
            ticksToStart = autoStart() ? 0 : TICKS_TO_START_MAX;
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
        dropUnavailableBots();
        fillMapBots();
        readyBots();

        if (startingGame) {
            if (ticksToStart % 20 == 0) {
                int secondsLeft = ticksToStart / 20;
                if (secondsLeft > 0) {
                    SoundClientboundPacket.playSoundForAllPlayers(SoundAction.CHAT);
                } else {
                    PlayerServerEvents.sendMessageToAllPlayers("startpos.reignofnether.started_game", true);
                    SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
                    FogOfWarServerEvents.setStartingPlayers(startPoses.stream()
                            .filter(sp -> !sp.playerName.isBlank() && sp.faction != Factions.NONE)
                            .map(StartPosServerEvents::owner).toList());
                    seatOccupants.clear();
                    try {
                        for (ServerPlayer serverPlayer : PlayerServerEvents.players) {
                            for (StartPos startPos : startPoses) {
                                if (startPos.playerName.equals(serverPlayer.getName().getString()) && startPos.faction != Factions.NONE) {
                                    if (isSoldierSeat(startPos))
                                        startSoldier(serverPlayer, startPos);
                                    else
                                        PlayerServerEvents.startRTS(
                                                serverPlayer.getId(),
                                                new Vec3(startPos.pos.getX(), startPos.pos.getY(), startPos.pos.getZ()),
                                                startPos.faction,
                                                startPos.colorId
                                        );
                                    startInSeat(startPos);
                                    break;
                                }
                            }
                        }
                    } finally {
                        FogOfWarServerEvents.setStartingPlayers(List.of());
                    }
                    RTSMapInfoServerEvents.syncTeamMode(); // catches a /gamerule during the lobby
                    // bots start after humans, so the first human start still runs its one-off match setup
                    for (StartPos startPos : startPoses)
                        if (startPos.enabled && startPos.isBot()) {
                            startBot(startPos, evt.getServer().overworld().getRandom());
                            startInSeat(startPos);
                        }
                    // ally all players on the same team: picked in the lobby, else the same colour of start block
                    Map<StartPos, Integer> teams = StartPos.teamNumbers(startPoses);
                    Map<String, Integer> teamOf = new HashMap<>();
                    for (StartPos sp : startPoses)
                        if (!sp.playerName.isBlank())
                            teamOf.put(owner(sp), teams.get(sp));
                    for (int i = 0; i < PlayerServerEvents.rtsPlayers.size(); i++) {
                        RTSPlayer p1 = PlayerServerEvents.rtsPlayers.get(i);
                        for (int j = i + 1; j < PlayerServerEvents.rtsPlayers.size(); j++) {
                            RTSPlayer p2 = PlayerServerEvents.rtsPlayers.get(j);
                            Integer t1 = teamOf.get(p1.name);
                            if (t1 != null && t1.equals(teamOf.get(p2.name))) {
                                AlliancesServerEvents.addAlliance(p1.name, p2.name);
                            }
                        }
                    }
                    startJoins(evt.getServer());
                    botReadyAt.clear();
                    PlayerServerEvents.setRTSLock(true, true);
                    StartPosServerEvents.reset(evt.getServer().getLevel(Level.OVERWORLD));
                    StartPosClientboundPacket.reset();
                    startingGame = false;
                    RonFiles.matchStart(evt.getServer());
                }
            }
            if (ticksToStart >= 0)
                ticksToStart -= 1;
        }
    }

    private static boolean isSoldierSeat(StartPos sp) {
        return RTSMapInfoServerEvents.seatStart(sp.pos).equals("soldier");
    }

    // who owns the seat's units: its player, or for a soldier seat the seat itself (its name, else seat<id>), a
    // faction with no AI that its player fights for in first person
    private static String owner(StartPos sp) {
        if (!isSoldierSeat(sp))
            return sp.playerName;
        if (!sp.seatName.isBlank())
            return sp.seatName;
        RTSMapInfo.Seat seat = RTSMapInfoServerEvents.rtsMapInfo.getSeat(sp.pos);
        return "seat" + (seat != null ? seat.id : sp.colorId);
    }

    // ponytail: the seat's faction is an AI-less bot entry, so it shows as a bot in the player list
    private static void startSoldier(ServerPlayer player, StartPos sp) {
        Faction faction = sp.faction == Factions.RANDOM
                ? MiscUtil.getRandomItem(List.of(Factions.VILLAGERS, Factions.MONSTERS, Factions.PIGLINS)) : sp.faction;
        String owner = owner(sp);
        PlayerServerEvents.startRTSBot(owner, new Vec3(sp.pos.getX(), sp.pos.getY(), sp.pos.getZ()), faction, sp.colorId);
        if (PlayerServerEvents.isRTSPlayer(owner))
            SoldierServerEvents.bind(player, owner, sp.pos, true);
    }

    // a map.json seat's fixed food/wood/ore replace the normal starting amounts
    private static void startInSeat(StartPos sp) {
        RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo;
        RTSMapInfo.Seat seat = info == null ? null : info.getSeat(sp.pos);
        String owner = owner(sp);
        if (seat == null || !PlayerServerEvents.isRTSPlayer(owner))
            return;
        if (seat.name != null && !seat.name.isBlank())
            seatOccupants.put(seat.name, owner);
        for (Resources resources : ResourcesServerEvents.resourcesList) {
            if (resources.ownerName.equals(owner)) {
                if (seat.food != null) resources.food = seat.food;
                if (seat.wood != null) resources.wood = seat.wood;
                if (seat.ore != null) resources.ore = seat.ore;
            }
        }
        ResourcesClientboundPacket.syncResources(ResourcesServerEvents.resourcesList);
    }

    // a map.json seat name -> the player playing it, or the name itself when nobody does
    // (units of an unplayed seat are owned by its name); null if no seat has this name
    @Nullable
    public static String seatOccupant(String name) {
        RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo;
        if (info == null)
            return null;
        for (RTSMapInfo.Seat seat : info.getSeats()) {
            if (seat.name != null && seat.name.equalsIgnoreCase(name)) {
                String player = seatOccupants.get(seat.name);
                return player != null && PlayerServerEvents.isRTSPlayer(player) ? player : seat.name;
            }
        }
        return null;
    }

    // after every seat has started: co-commanders take on their seat's faction, soldiers join their team's commander
    private static void startJoins(MinecraftServer server) {
        for (SeatJoin join : joins) {
            if (join.pending)
                continue; // never answered: they stay a spectator
            ServerPlayer player = server.getPlayerList().getPlayerByName(join.playerName);
            StartPos seat = startPoses.stream()
                    .filter(sp -> !sp.playerName.isBlank() && PlayerServerEvents.inRtsList(sp.playerName) &&
                            (join.isSoldier() ? team(sp) == join.colorId : sp.pos.equals(join.seat)))
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

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loadPositions(ServerStartedEvent evt) {
        // after RTSMapInfoServerEvents.loadInfo (normal priority) has read rtsMapInfo
        loadPositionsFromMapInfo();
        ReignOfNether.LOGGER.info("loaded " + startPoses.size() + " start positions from map.json");
    }

    public static void loadPositionsFromMapInfo() {
        if (!RTSMapInfoServerEvents.usingMapInfoStartPositions())
            return;
        if (!hasMapSeats()) { // a map.json that no longer has any
            startPoses.forEach(sp -> StartPosClientboundPacket.removePos(sp.pos));
            startPoses.clear();
            syncAll();
            return;
        }
        RTSMapInfoServerEvents.syncTeamMode();
        int playerColorIndex = 0;
        List<StartPos> oldStartPoses = new ArrayList<>(startPoses);
        startPoses.clear();
        RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo;
        List<List<BlockPos>> teams = info.getTeams();
        if (teams.isEmpty()) // mode "none": every seat, each its own colour, like start blocks
            teams = info.getSeats().stream().map(seat -> List.of(seat.toBlockPos())).toList();
        for (List<BlockPos> teamStartPoses : teams) {
            for (BlockPos startBlockPos : teamStartPoses) {
                RTSMapInfo.Seat seat = info.getSeat(startBlockPos);
                // "none" isn't offered to players, its units just stay owned by the seat name
                if (seat != null && seat.getControl().equals("none"))
                    continue;
                StartPos startPos = new StartPos(startBlockPos, PlayerColors.colors[playerColorIndex].hexCode);
                if (seat != null) {
                    startPos.seatName = seat.name == null ? "" : seat.name;
                    startPos.fixedFaction = Factions.getFaction(seat.getFactionKey());
                    // closed until fillMapBots seats its bot, so no player takes it meanwhile
                    startPos.mapBot = seat.getControl().equals("bot");
                    startPos.enabled = !startPos.mapBot;
                }
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
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player)
            GuideServer.loggedOut(player);
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
