package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.chat.TeamChatServerEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.ping.PingType;
import net.minecraft.core.BlockPos;
import com.solegendary.reignofnether.player.PlayerRoles;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.startpos.StartPos;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Runs one BotBrain per AI-controlled RTS player. Bots are RTSPlayers with negative ids (see startRTSBot)
// whose names start with BOT_PREFIX; '-' can't appear in Minecraft usernames so they never clash with humans.
public class BotServerEvents {

    public static final String BOT_PREFIX = "Bot-";
    private static final List<String> NAMES = List.of("Ava", "Bram", "Cora", "Dax", "Edda", "Finn", "Gale", "Hugo",
            "Iris", "Jax", "Kira", "Lux", "Mira", "Nox", "Orin", "Pia");

    private static final Map<String, BotBrain> brains = new ConcurrentHashMap<>();
    private static long tick = 0;
    private static boolean restored = false;
    // botTakeover: humans who left mid-match -> tick they left; a quick reconnect cancels the takeover
    private static final Map<String, Long> leftAt = new ConcurrentHashMap<>();
    private static final int TAKEOVER_DELAY = 15 * 20;

    public static boolean isBotName(String name) {
        return name != null && name.startsWith(BOT_PREFIX);
    }

    public static String getUnusedBotName() {
        Set<String> used = new HashSet<>(brains.keySet());
        for (StartPos sp : StartPosServerEvents.startPoses)
            used.add(sp.playerName);
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                used.add(p.name);
        }
        for (String n : NAMES)
            if (!used.contains(BOT_PREFIX + n))
                return BOT_PREFIX + n;
        int i = 1;
        while (used.contains(BOT_PREFIX + i))
            i++;
        return BOT_PREFIX + i;
    }

    @Nullable
    static BotBrain brain(String name) {
        return brains.get(name);
    }

    // true while a brain plays this name: a bot, or a disconnected human taken over
    public static boolean isControlled(String name) {
        return brains.containsKey(name);
    }

    public static void addBot(String name, BotDifficulty difficulty) {
        brains.put(name, new BotBrain(name, difficulty));
    }

    // bots need a bounded map and the RTS pathfinder: they plan with it and their armies walk it. The match start
    // screen says why when they're not available (MatchStartScreen.botsUnavailable)
    public static boolean botsAvailable() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null && WorldBorderServerEvents.isRtsOptimisedMap(server.overworld()) && UnitServerEvents.rtsPathfinding;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;
        ServerLevel level = PlayerServerEvents.serverLevel;
        if (level == null)
            return;
        tick++;
        if (!restored) {
            restored = true;
            restoreBrains();
        }
        takeOverLeavers(level);
        if (brains.isEmpty())
            return;
        for (BotBrain brain : brains.values()) {
            if (!PlayerServerEvents.inRtsList(brain.name)) { // defeated or match reset
                brains.remove(brain.name);
                if (brain.memory != null && PlayerServerEvents.isGameActive())
                    brain.comms.gg();
                continue;
            }
            // each bot gets its own random rhythm (and a 1-5 s late start), so bots don't all act on the same tick
            if (brain.startTick < 0)
                brain.startTick = tick + 20 + level.random.nextInt(80);
            if (tick < brain.startTick || (tick - brain.startTick) % brain.difficulty.ticksPerThink != 0)
                continue;
            try {
                brain.think(level, tick);
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("[Bot] {} think failed", brain.name, e);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        String name = evt.getEntity().getName().getString();
        if (PlayerServerEvents.inRtsList(name) && PlayerServerEvents.isGameActive())
            leftAt.put(name, tick);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        String name = evt.getEntity().getName().getString();
        leftAt.remove(name);
        BotBrain brain = brains.get(name);
        if (brain != null && brain.takeover) {
            brains.remove(name);
            PlayerServerEvents.sendMessageToAllPlayers("server.reignofnether.bot_takeover_end", true, name);
        }
    }

    // a human gone longer than TAKEOVER_DELAY gets a brain on their base (botTakeover gamerule, off by default),
    // unless a co-commander still plays it or the bots can't play their faction right now (BotPlans)
    // ponytail: takeovers aren't saved, so a server restart leaves the base idle until the player returns
    private static void takeOverLeavers(ServerLevel level) {
        if (leftAt.isEmpty())
            return;
        boolean enabled = level.getGameRules().getBoolean(GameRuleRegistrar.BOT_TAKEOVER);
        leftAt.entrySet().removeIf(e -> {
            if (tick - e.getValue() < TAKEOVER_DELAY)
                return false;
            String name = e.getKey();
            if (enabled && PlayerServerEvents.inRtsList(name) && PlayerServerEvents.isGameActive() && !brains.containsKey(name) &&
                    BotPlans.isAvailable(factionOf(name)) && !coCommanderOnline(level, name)) {
                BotBrain brain = new BotBrain(name, BotDifficulty.NORMAL);
                brain.takeover = true;
                brains.put(name, brain);
                PlayerServerEvents.sendMessageToAllPlayers("server.reignofnether.bot_takeover", true, name);
                ReignOfNether.LOGGER.info("[Bot] taking over {}'s base", name);
            }
            return true;
        });
    }

    private static Faction factionOf(String name) {
        RTSPlayer p = PlayerServerEvents.getRTSPlayer(name);
        return p != null ? p.faction : Factions.NONE;
    }

    private static boolean coCommanderOnline(ServerLevel level, String owner) {
        for (String co : PlayerRoles.coCommandersOf(owner))
            if (level.getServer().getPlayerList().getPlayerByName(co) != null)
                return true;
        return false;
    }

    // bots hear every ping their team makes, soldiers' and co-commanders' too (see PingServerEvents)
    public static void onPing(String sender, PingType type, BlockPos pos) {
        for (BotBrain brain : brains.values()) {
            if (brain.name.equals(sender) || !TeamChatServerEvents.sameTeam(sender, brain.name) || brain.memory == null)
                continue;
            try {
                brain.comms.onPing(sender, type, pos);
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("[Bot] {} failed to answer a ping", brain.name, e);
            }
        }
    }

    // AllyCommand: someone asked a bot for an alliance, or ended one with it
    public static void onAllyRequest(String from, String to) {
        BotBrain brain = brains.get(to);
        if (brain != null && !brain.takeover && brain.memory != null)
            brain.diplomacy.onRequest(from);
    }

    public static void onDisbanded(String by, String with) {
        BotBrain brain = brains.get(with);
        if (brain != null && !brain.takeover && brain.memory != null)
            brain.diplomacy.onDisbanded(by);
    }

    // a reloaded world keeps its bot RTSPlayers, so give them their brains back (Normal is the only difficulty)
    private static void restoreBrains() {
        if (TutorialServerEvents.isEnabled())
            return;
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                if (p.isBot() && isBotName(p.name) && !brains.containsKey(p.name))
                    brains.put(p.name, new BotBrain(p.name, BotDifficulty.NORMAL));
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        brains.clear();
        BotFighters.clear();
        leftAt.clear();
        restored = false;
        tick = 0;
    }
}
