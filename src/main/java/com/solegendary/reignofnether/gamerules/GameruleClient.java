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
import java.util.function.Supplier;
import javax.annotation.Nullable;


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
    public static boolean pvpModesOnly = false;
    public static double beaconWinMinutes = 10;
    public static boolean slantedBuilding = true;
    public static int allowedHeroes = 2;
    public static boolean lockAlliances = false;
    public static boolean hideEnemyPicks = true;
    public static boolean scenarioMode = false;
    public static boolean coopMode = false;
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
     * One game rule as the UI shows it. Boolean rules toggle; number rules step with − and + (shift for
     * bigger steps). Lobby rules can also be changed by the match host, the rest need operator rights.
     */
    public record Rule(String id, boolean lobby, Supplier<String> value, Supplier<Boolean> on,
                       Runnable increase, @Nullable Runnable decrease) {

        public boolean isToggle() {
            return decrease == null;
        }

        public String name() {
            return I18n.get("gamerule.reignofnether.name." + id);
        }

        public String description() {
            return I18n.get("commands.reignofnether.gamerule." + id);
        }

        public boolean canEdit() {
            if (MC.player == null || StartPosClientEvents.isStarting || teamLocked() || picksLocked())
                return false;
            return MC.player.hasPermissions(4) || (lobby && StartPosClientEvents.isHost());
        }

        public SettingRow toRow() {
            return new SettingRow(name(), description(),
                    I18n.get(lobby ? "hud.gamerule.reignofnether.tag.match" : "hud.gamerule.reignofnether.tag.server"),
                    value, on, increase, decrease, this::canEdit,
                    I18n.get(teamLocked() ? "hud.gamerule.reignofnether.team_locked"
                            : picksLocked() ? "hud.gamerule.reignofnether.picks_locked"
                            : lobby ? "hud.gamerule.reignofnether.host_only" : "hud.gamerule.reignofnether.op_only"));
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

    public static List<SettingRow> ruleRows() {
        return ruleRows(true);
    }

    public static List<SettingRow> mapmakerRuleRows() {
        return ruleRows(false);
    }

    private static List<SettingRow> ruleRows(boolean lobby) {
        return rules().stream().filter(r -> r.lobby() == lobby).map(Rule::toRow).toList();
    }

    private static int step(int small, int big) {
        return Keybindings.shiftMod.isDown() ? big : small;
    }

    private static Rule toggle(String id, boolean lobby, Supplier<Boolean> value, java.util.function.Consumer<Boolean> set) {
        return new Rule(id, lobby, () -> I18n.get(value.get() ? "options.on" : "options.off"), value,
                () -> set.accept(!value.get()), null);
    }

    private static Rule number(String id, boolean lobby, Supplier<Integer> value, int min, int max, int small, int big,
                               java.util.function.Consumer<Integer> set) {
        return new Rule(id, lobby, () -> String.valueOf(value.get()), () -> true,
                () -> set.accept(Math.min(max, value.get() + step(small, big))),
                () -> set.accept(Math.max(min, value.get() - step(small, big))));
    }

    /** All rules in display order; the match setup shows the lobby ones first. */
    public static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        rules.add(toggle("fog_of_war", true, FogOfWarClientEvents::isMatchFogOn, GameruleServerboundPacket::setFogOfWar));
        rules.add(toggle("hide_enemy_picks", true, () -> hideEnemyPicks && !seatJoinsOn(), GameruleServerboundPacket::setHideEnemyPicks));
        rules.add(number("max_population", true, () -> maxPopulation, 0, 10000, 5, 25, GameruleServerboundPacket::setMaxPopulation));
        rules.add(number("allowed_heroes", true, () -> allowedHeroes, 0, 2, 1, 1, GameruleServerboundPacket::setAllowedHeroes));
        rules.add(new Rule("random_item_drops", true,
                () -> I18n.get("gamerule.reignofnether.random_item_drops." + randomItemDrops.getValue()),
                () -> randomItemDrops != RandomItemDropRule.DISABLED,
                () -> GameruleServerboundPacket.setRandomItemDrops((randomItemDrops.getValue() + 1) % 4),
                () -> GameruleServerboundPacket.setRandomItemDrops((randomItemDrops.getValue() + 3) % 4)));
        rules.add(toggle("neutral_aggro", true, () -> neutralAggro, GameruleServerboundPacket::setNeutralAggro));
        rules.add(toggle("lock_alliances", true, () -> lockAlliances, GameruleServerboundPacket::setLockAlliances));
        rules.add(number("soldiers_per_team", true, () -> soldiersPerTeam, 0, MAX_SOLDIERS_PER_TEAM, 1, 1, GameruleServerboundPacket::setSoldiersPerTeam));
        rules.add(toggle("co_commanders", true, () -> coCommanders, GameruleServerboundPacket::setCoCommanders));
        rules.add(number("soldier_respawn_seconds", true, () -> soldierRespawnSeconds, 0, GameRuleRegistrar.MAX_SOLDIER_RESPAWN_SECONDS, 5, 30, GameruleServerboundPacket::setSoldierRespawnSeconds));
        rules.add(toggle("all_chat", true, () -> allChat, GameruleServerboundPacket::setAllChat));
        rules.add(toggle("allow_beacons", true, () -> allowBeacons, GameruleServerboundPacket::setAllowBeacons));
        rules.add(number("beacon_win_minutes", true, () -> (int) beaconWinMinutes, 1, 1000, 1, 10, GameruleServerboundPacket::setBeaconWinMinutes));
        rules.add(toggle("coop_mode", false, () -> coopMode, GameruleServerboundPacket::setCoopMode));
        rules.add(toggle("pvp_modes_only", false, () -> pvpModesOnly, GameruleServerboundPacket::setPvpModesOnly));
        rules.add(toggle("do_log_falling", false, () -> doLogFalling, GameruleServerboundPacket::setLogFalling));
        rules.add(toggle("unit_griefing", false, () -> doUnitGriefing, GameruleServerboundPacket::setUnitGriefing));
        rules.add(toggle("player_griefing", false, () -> doPlayerGriefing, GameruleServerboundPacket::setPlayerGriefing));
        rules.add(toggle("slanted_buildings", false, () -> slantedBuilding, GameruleServerboundPacket::setSlantedBuilding));
        rules.add(toggle("rts_pathfinding", false, () -> rtsPathfinding, GameruleServerboundPacket::setRtsPathfinding));
        rules.add(number("ground_y_level", false, () -> (int) groundYLevel, -320, 320, 1, 10, GameruleServerboundPacket::setGroundYLevel));
        rules.add(number("flying_max_y_level", false, () -> (int) flyingMaxYLevel, -320, 320, 1, 10, GameruleServerboundPacket::setFlyingMaxYLevel));
        rules.add(number("animal_spawn_y_diff", false, () -> animalSpawnYDiff, 1, 100, 1, 5, GameruleServerboundPacket::setAnimalSpawnYDiff));
        return rules;
    }
}
