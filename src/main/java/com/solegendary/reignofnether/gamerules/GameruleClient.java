package com.solegendary.reignofnether.gamerules;

import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.items.RandomItemDropRule;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.resources.ResourceCosts;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.ui.SettingRow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;


public class GameruleClient {

    private static final Minecraft MC = Minecraft.getInstance();

    public static boolean doLogFalling = true; // only for GUI
    public static boolean neutralAggro = false;
    public static int maxPopulation = ResourceCosts.DEFAULT_MAX_POPULATION;
    public static boolean doUnitGriefing = false; // only for GUI
    public static boolean doPlayerGriefing = true; // only for GUI
    public static double groundYLevel = -320;
    public static double flyingMaxYLevel = 320;
    public static boolean allowBeacons = true;
    public static double beaconWinMinutes = 10;
    public static boolean slantedBuilding = true;
    public static int allowedHeroes = 2;
    public static boolean lockAlliances = false;
    public static boolean hideEnemyPicks = true;
    public static boolean buildingsOutsideBorder = false;
    public static boolean rtsPathfinding = false; // only for GUI
    public static int animalSpawnYDiff = 5;
    public static RandomItemDropRule randomItemDrops = RandomItemDropRule.ENABLED_SAME_RARITY;
    public static final int MAX_SOLDIERS_PER_TEAM = GameRuleRegistrar.MAX_SOLDIERS_PER_TEAM;
    public static int soldiersPerTeam = 0;
    public static boolean coCommanders = false;
    public static int soldierRespawnSeconds = 30;
    public static boolean allChat = true;

    public static boolean gamerulesMenuOpen = false;

    /**
     * One match rule as the UI shows it, its value as a number (booleans 0/1). Toggles flip; number rules step with
     * − and + (shift for bigger steps). The match host can change them in the lobby, unless the map locks them.
     * advanced: in the "Advanced" group, folded away in the match menus until shown, else "Basic".
     */
    public record Rule(String id, boolean advanced, int min, int max, int small, int big,
                       Supplier<Integer> get, Consumer<Integer> set) {

        public boolean isToggle() {
            return max == 1;
        }

        public String name() {
            return I18n.get("gamerule.reignofnether.name." + id);
        }

        public String description() {
            return I18n.get("commands.reignofnether.gamerule." + id);
        }

        /** Its id in map.json rules and lockedRules: the gamerule's (fog_of_war, which isn't one, is fogOfWar). */
        public String mapId() {
            return GameruleClient.mapId(id);
        }

        /** The same rule, read from and written to somewhere else (the Editor's map defaults). */
        public Rule withValue(Supplier<Integer> get, Consumer<Integer> set) {
            return new Rule(id, advanced, min, max, small, big, get, set);
        }

        public boolean canEdit() {
            if (MC.player == null || StartPosClientEvents.isStarting || teamLocked() || picksLocked() || isMapLocked(id))
                return false;
            return MC.player.hasPermissions(4) || StartPosClientEvents.isHost();
        }

        public SettingRow toRow() {
            return toRow(this::canEdit, I18n.get(teamLocked() ? "hud.gamerule.reignofnether.team_locked"
                    : picksLocked() ? "hud.gamerule.reignofnether.picks_locked"
                    : isMapLocked(id) ? "hud.gamerule.reignofnether.map_locked" : "hud.gamerule.reignofnether.host_only"));
        }

        public SettingRow toRow(Supplier<Boolean> editable, String lockedHint) {
            boolean drops = id.equals("random_item_drops"); // cycles through its 4 settings
            return new SettingRow(name(), description(), null,
                    () -> isToggle() ? I18n.get(get.get() == 1 ? "options.on" : "options.off")
                            : drops ? I18n.get("gamerule.reignofnether.random_item_drops." + get.get()) : String.valueOf(get.get()),
                    () -> !isToggle() && !drops || get.get() != 0,
                    () -> set.accept(isToggle() ? 1 - get.get() : drops ? (get.get() + 1) % 4 : Math.min(max, get.get() + step(small, big))),
                    isToggle() ? null : () -> set.accept(drops ? (get.get() + 3) % 4 : Math.max(min, get.get() - step(small, big))),
                    editable, lockedHint);
        }

        private boolean teamLocked() {
            return id.equals("lock_alliances") && RTSMapInfoClientEvents.teamMode;
        }

        private boolean picksLocked() {
            return id.equals("hide_enemy_picks") && seatJoinsOn();
        }
    }

    // soldiers and co-commanders pick a seat by name, so picks can't be hidden (StartPosServerEvents.syncTo)
    private static boolean seatJoinsOn() {
        return coCommanders || soldiersPerTeam > 0;
    }

    /** The map locks this rule (map.json lockedRules), so the lobby can't change it. Takes the snake case id. */
    public static boolean isMapLocked(String id) {
        return RTSMapInfoClientEvents.lockedRules.contains(mapId(id));
    }

    private static String mapId(String id) {
        return Pattern.compile("_([a-z])").matcher(id).replaceAll(m -> m.group(1).toUpperCase(Locale.ROOT));
    }

    /** The match rules for the match setup and Esc menu: the Game group, then Teams and players, each with its header. */
    // the lobby's More options: every rule but its quick toggles (skip), in one list
    public static List<SettingRow> ruleRows(Set<String> skip) {
        return rules().stream().filter(r -> !skip.contains(r.id())).map(Rule::toRow).toList();
    }

    public static String groupName(boolean advanced) {
        return I18n.get(advanced ? "sandbox.reignofnether.rules.group.advanced" : "sandbox.reignofnether.rules.group.basic");
    }

    private static int step(int small, int big) {
        return Keybindings.shiftMod.isDown() ? big : small;
    }

    private static Rule toggle(String id, boolean teams, Supplier<Boolean> value, Consumer<Boolean> set) {
        return new Rule(id, teams, 0, 1, 1, 1, () -> value.get() ? 1 : 0, v -> set.accept(v == 1));
    }

    private static Rule number(String id, boolean teams, Supplier<Integer> value, int min, int max, int small, int big,
                               Consumer<Long> set) {
        return new Rule(id, teams, min, max, small, big, value, v -> set.accept((long) v));
    }

    /**
     * The match rules, Basic ones first. Map rules (ground level, griefing, beacons...) are set in the Editor's Map tab
     * and technical ones (rtsPathfinding, doLogFalling) only by /gamerule.
     */
    public static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(toggle("fog_of_war", false, FogOfWarClientEvents::isMatchFogOn, GameruleServerboundPacket::setFogOfWar));
        rules.add(toggle("lock_alliances", false, () -> lockAlliances, GameruleServerboundPacket::setLockAlliances));
        rules.add(toggle("co_commanders", false, () -> coCommanders, GameruleServerboundPacket::setCoCommanders));
        rules.add(number("soldiers_per_team", false, () -> soldiersPerTeam, 0, MAX_SOLDIERS_PER_TEAM, 1, 1, GameruleServerboundPacket::setSoldiersPerTeam));
        rules.add(toggle("hide_enemy_picks", false, () -> hideEnemyPicks && !seatJoinsOn(), GameruleServerboundPacket::setHideEnemyPicks));
        rules.add(toggle("all_chat", false, () -> allChat, GameruleServerboundPacket::setAllChat));
        rules.add(number("max_population", true, () -> maxPopulation, 0, 10000, 5, 25, GameruleServerboundPacket::setMaxPopulation));
        rules.add(number("allowed_heroes", true, () -> allowedHeroes, 0, 2, 1, 1, GameruleServerboundPacket::setAllowedHeroes));
        rules.add(number("random_item_drops", true, () -> randomItemDrops.getValue(), 0, 3, 1, 1, GameruleServerboundPacket::setRandomItemDrops));
        rules.add(toggle("neutral_aggro", true, () -> neutralAggro, GameruleServerboundPacket::setNeutralAggro));
        rules.add(number("beacon_win_minutes", true, () -> (int) beaconWinMinutes, 1, 1000, 1, 10, GameruleServerboundPacket::setBeaconWinMinutes));
        rules.add(number("soldier_respawn_seconds", true, () -> soldierRespawnSeconds, 0, GameRuleRegistrar.MAX_SOLDIER_RESPAWN_SECONDS, 5, 30, GameruleServerboundPacket::setSoldierRespawnSeconds));
        return rules;
    }
}
