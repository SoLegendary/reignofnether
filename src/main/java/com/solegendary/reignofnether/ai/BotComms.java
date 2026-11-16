package com.solegendary.reignofnether.ai;

import com.solegendary.reignofnether.chat.TeamChatServerEvents;
import com.solegendary.reignofnether.chat.TeamChatServerEvents.Scope;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.ping.PingServerEvents;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.ping.PingType;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// How a bot talks to its team (players, FPV soldiers and other bots): it pings and chats when it plans an attack,
// attacks, pulls back, is attacked or holds, and answers allies' pings by checking them first, then acting on them.
// Bots never answer ON_MY_WAY, so two bots can't loop.
public class BotComms {

    private static final int VARIANTS = 2; // lang keys end in .1 / .2
    private static final long HELP_COOLDOWN = 60 * 20, ATTACK_COOLDOWN = 60 * 20, ENEMY_COOLDOWN = 90 * 20;
    private static final long REPLY_COOLDOWN = 10 * 20;
    private static final int BIG_ARMY_POP = 10;

    private final BotBrain bot;
    private long lastHelp = -HELP_COOLDOWN, lastAttack = -ATTACK_COOLDOWN, lastEnemy = -ENEMY_COOLDOWN;
    private final Map<PingType, Long> lastReply = new EnumMap<>(PingType.class);
    private boolean greeted = false;
    // an ally's ping is acted on after a human-like reaction time (1-4 s), not on the very next think
    private static final int REACT_MIN = 20, REACT_SPREAD = 60;
    private record Heard(long at, String sender, PingType type, BlockPos pos) {}
    private final List<Heard> heard = new java.util.ArrayList<>();

    public BotComms(BotBrain bot) {
        this.bot = bot;
    }

    void say(String key, Scope scope, String... args) {
        if (bot.takeover)
            return; // never chat under a human's name; pings still go out
        int v = 1 + (bot.level != null ? bot.level.random.nextInt(VARIANTS) : 0);
        TeamChatServerEvents.send(bot.name, Component.literal(bot.name), scope,
                Component.translatable("bot.reignofnether.chat." + key + "." + v, (Object[]) args));
    }

    // pings land on the ground under pos, like a player's click: army centres, building centres and rally points
    // are often in the air or inside a building
    private boolean ping(PingType type, BlockPos pos) {
        return PingServerEvents.ping(bot.name, type, ground(bot, pos));
    }

    static BlockPos ground(BotBrain bot, BlockPos pos) {
        return MiscUtil.getHighestNonAirBlockFromHeightmap(bot.level, pos, true);
    }

    public void think() {
        if (!greeted && bot.tick - bot.firstThinkTick > 5 * 20) {
            greeted = true;
            say("glhf", Scope.ALL);
        }
        for (Heard h : List.copyOf(heard))
            if (bot.tick >= h.at()) {
                heard.remove(h);
                react(h.sender(), h.type(), h.pos());
            }
        spotArmy();
    }

    // a big enemy army away from our base: tell the team where it is (at home the guard handles it). Without fog
    // of war everyone already sees it
    private void spotArmy() {
        if (!FogOfWarServerEvents.isEnabled() || bot.tick - lastEnemy < ENEMY_COOLDOWN)
            return;
        List<LivingEntity> far = bot.memory.visibleEnemies.stream()
                .filter(e -> e.blockPosition().distSqr(bot.home) > 48 * 48).toList();
        if (far.isEmpty())
            return;
        BlockPos at = BotMath.centroid(far);
        List<LivingEntity> group = bot.memory.enemiesNear(Vec3.atCenterOf(at), 16);
        if (BotMath.popOf(group) >= BIG_ARMY_POP && ping(PingType.ENEMY, at))
            lastEnemy = bot.tick;
    }

    public void underAttack(BlockPos pos, int enemyPop) {
        if (bot.tick - lastHelp < HELP_COOLDOWN || enemyPop < 4)
            return;
        lastHelp = bot.tick;
        if (ping(PingType.HELP, pos))
            say("help", Scope.TEAM);
    }

    public void attacking(BlockPos target) {
        if (bot.tick - lastAttack < ATTACK_COOLDOWN)
            return;
        lastAttack = bot.tick;
        if (ping(PingType.ATTACK, target))
            say("attacking", Scope.TEAM);
    }

    public void gg() {
        say("gg", Scope.ALL);
    }

    // an ally pinged: react after a moment, like a player reading the minimap
    public void onPing(String sender, PingType type, BlockPos pos) {
        if (type == PingType.ON_MY_WAY)
            return;
        heard.add(new Heard(bot.tick + REACT_MIN + bot.level.random.nextInt(REACT_SPREAD), sender, type, pos));
    }

    // act first, then say what we're doing. A ping that makes no sense (no enemy there) gets an answer only from a
    // human: bots ping each other often, and "I see nothing there" between two bots is just noise
    private void react(String sender, PingType type, BlockPos pos) {
        if (bot.tick - lastReply.getOrDefault(type, -REPLY_COOLDOWN) < REPLY_COOLDOWN)
            return;
        lastReply.put(type, bot.tick);
        boolean human = !PlayerServerEvents.isBot(sender);
        switch (type) {
            case HELP, DEFEND -> {
                BotArmy.Reply r = bot.army.answerHelp(pos);
                switch (r.answer()) {
                    case COMING -> {
                        ping(PingType.ON_MY_WAY, pos);
                        say("on_my_way", Scope.TEAM);
                    }
                    case NO_ENEMY -> { if (human) say("no_enemy", Scope.TEAM); }
                    case TOO_STRONG -> say("too_strong_help", Scope.TEAM);
                    case DEFENDING -> say("under_attack_too", Scope.TEAM);
                    default -> say("cant_help", Scope.TEAM);
                }
            }
            case ATTACK -> {
                BotArmy.Reply r = bot.army.answerAttack(pos, sender);
                switch (r.answer()) {
                    case JOINING -> {
                        ping(PingType.ON_MY_WAY, pos);
                        say("joining", Scope.TEAM);
                    }
                    case NOTHING_THERE -> { if (human) say("nothing_to_attack", Scope.TEAM); }
                    case TOO_STRONG -> say("too_strong_attack", Scope.TEAM, String.valueOf(r.seconds()));
                    case DEFENDING -> say("under_attack_too", Scope.TEAM);
                    default -> say("cant_help", Scope.TEAM);
                }
            }
            case ENEMY -> {
                if (bot.scout.answerEnemy(pos))
                    say("scouting", Scope.TEAM);
            }
            default -> {}
        }
    }

    // ---------------------------------------------------------------- what the army is doing

    // the wave is grouping up: where it's going and in about how long, so allies and soldiers can come along
    void planningAttack(BlockPos target, int seconds) {
        if (target == null || !ping(PingType.ATTACK, target))
            return;
        lastAttack = bot.tick; // the attack ping as it leaves would just repeat this one
        say("attack_soon", Scope.TEAM, String.valueOf(seconds));
    }

    void attackOff(boolean underAttack) {
        say(underAttack ? "attack_off_defending" : "attack_off", Scope.TEAM);
    }

    // losing the fight: allies shouldn't keep fighting it alone
    void pullingBack() {
        if (once("pulling_back", 30 * 20)) {
            ping(PingType.DEFEND, bot.army.rallyPoint());
            say("pulling_back", Scope.TEAM);
        }
    }

    void armyLost() {
        if (once("army_lost", 60 * 20))
            say("army_lost", Scope.TEAM);
    }

    void targetDown() {
        if (once("target_down", 20 * 20))
            say("target_down", Scope.TEAM);
    }

    void campCleared() {
        if (once("camp_cleared", 60 * 20))
            say("camp_cleared", Scope.TEAM);
    }

    void held() {
        if (once("held", 60 * 20))
            say("held", Scope.TEAM);
    }

    private final Map<String, Long> lastSaid = new java.util.HashMap<>();

    // true at most once per cooldown for this line
    private boolean once(String key, long cooldown) {
        if (bot.tick - lastSaid.getOrDefault(key, -cooldown) < cooldown)
            return false;
        lastSaid.put(key, bot.tick);
        return true;
    }
}
