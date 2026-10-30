package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.alliance.AllyCommand;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

// A bot's alliances, made the way a player makes them (AllyCommand). It accepts a request from someone who hasn't
// hit it lately, as long as another enemy is left to fight; asks an enemy that's leaving it alone for an alliance
// when it's clearly losing; and walks out on an ally whose units attack it.
public class BotDiplomacy {

    private static final long HOSTILE_MEMORY = 3 * 60 * 20; // how long being attacked by someone is held against them
    private static final long PROPOSE_EVERY = 60 * 20, ASK_AGAIN = 5 * 60 * 20;
    private static final int REACT_MIN = 40, REACT_SPREAD = 60; // 2-5 s to "read" a request

    private final BotBrain bot;
    private final Map<String, Long> lastHurtBy = new HashMap<>(); // player -> tick their units last hurt ours
    private final Map<String, Long> asked = new HashMap<>(); // player -> tick we last proposed to them
    private record Request(long at, String from) {}
    private final List<Request> requests = new ArrayList<>();
    private long lastPropose = 0;

    public BotDiplomacy(BotBrain bot) {
        this.bot = bot;
    }

    public void onRequest(String from) {
        long now = bot.tick;
        int delay = REACT_MIN + (bot.level != null ? bot.level.random.nextInt(REACT_SPREAD) : 0);
        requests.add(new Request(now + delay, from));
    }

    public void onDisbanded(String by) {
        bot.comms.say("ally_disbanded", Scope.ALL, by);
    }

    public void think() {
        noteAttackers();
        answerRequests();
        propose();
    }

    private boolean hostile(String player) {
        return bot.tick - lastHurtBy.getOrDefault(player, -HOSTILE_MEMORY) < HOSTILE_MEMORY;
    }

    // who's been hitting our units; an ally doing it ends the alliance
    private void noteAttackers() {
        for (LivingEntity e : bot.units) {
            LivingEntity by = e.getLastHurtByMob();
            if (!(by instanceof Unit u) || e.tickCount - e.getLastHurtByMobTimestamp() > 40)
                continue;
            String owner = u.getOwnerName();
            if (owner.isBlank() || owner.equals(bot.name))
                continue;
            lastHurtBy.put(owner, bot.tick);
            if (AlliancesServerEvents.isAllied(bot.name, owner) && AllyCommand.lockedReason(bot.name, owner) == null) {
                AllyCommand.disbandAlliance(bot.name, owner);
                bot.comms.say("ally_betrayed", Scope.ALL, owner);
            }
        }
    }

    private List<String> enemies() {
        List<String> list = new ArrayList<>();
        synchronized (PlayerServerEvents.rtsPlayers) {
            for (RTSPlayer p : PlayerServerEvents.rtsPlayers)
                if (bot.memory.isEnemy(p.name))
                    list.add(p.name);
        }
        return list;
    }

    private void answerRequests() {
        for (Request r : List.copyOf(requests)) {
            if (bot.tick < r.at())
                continue;
            requests.remove(r);
            if (!AllyCommand.hasRequest(bot.name, r.from()) || AllyCommand.lockedReason(bot.name, r.from()) != null)
                continue; // taken back meanwhile, or alliances got locked
            boolean othersLeft = enemies().stream().anyMatch(n -> !n.equals(r.from()));
            if (!hostile(r.from()) && othersLeft) {
                AllyCommand.confirmAlliance(bot.name, r.from());
                bot.comms.say("ally_accept", Scope.ALL, r.from());
            } else {
                AllyCommand.cancelRequest(bot.name, r.from());
                bot.comms.say(hostile(r.from()) ? "ally_decline_hostile" : "ally_decline_last", Scope.ALL, r.from());
            }
        }
    }

    // clearly losing with more than one enemy around: ask one that's been leaving us alone to team up
    private void propose() {
        if (bot.tick - lastPropose < PROPOSE_EVERY)
            return;
        lastPropose = bot.tick;
        int theirs = bot.memory.recentEnemyArmyPop(), ours = BotMath.popOf(bot.fighters);
        List<String> enemies = enemies();
        if (enemies.size() < 2 || theirs < 8 || theirs * 2 < ours * 3)
            return;
        List<String> candidates = enemies.stream()
                .filter(n -> !hostile(n) && bot.tick - asked.getOrDefault(n, -ASK_AGAIN) >= ASK_AGAIN &&
                        !AllyCommand.hasRequest(n, bot.name) && AllyCommand.lockedReason(bot.name, n) == null)
                .toList();
        if (candidates.isEmpty())
            return;
        String to = candidates.get(bot.level.random.nextInt(candidates.size()));
        asked.put(to, bot.tick);
        bot.comms.say("ally_propose", Scope.ALL, to);
        AllyCommand.requestAlliance(bot.name, to);
    }
}
