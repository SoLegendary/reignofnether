package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.scenario.ScenarioRole;
import com.solegendary.reignofnether.scenario.ScenarioServerEvents;
import com.solegendary.reignofnether.scenario.ScenarioUtils;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

public class AlliancesServerEvents {
    private static final Map<String, Set<String>> alliances = new HashMap<>();
    public static final Map<String, Set<String>> pendingAlliances = new HashMap<>();
    public static final HashSet<String> playersWithAlliedControl = new HashSet<>();

    public static boolean canControlAlly(String player, LivingEntity entity) {
        return entity instanceof Unit unit && canControlAlly(player, unit.getOwnerName());
    }
    public static boolean canControlAlly(String player, Unit unit) {
        return canControlAlly(player, unit.getOwnerName());
    }
    public static boolean canControlAlly(String player, String ownerName) {
        return (AlliancesServerEvents.isAllied(player, ownerName) &&
                AlliancesServerEvents.playersWithAlliedControl.contains(ownerName));
    }

    public static void addAlliance(String owner1, String owner2) {
        if (!owner1.equals(owner2)) {
            alliances.computeIfAbsent(owner1, k -> new HashSet<>()).add(owner2);
            alliances.computeIfAbsent(owner2, k -> new HashSet<>()).add(owner1);
            AllianceClientboundPacket.addAlliance(owner1, owner2);
        }
    }

    public static void removeAlliance(String owner1, String owner2) {
        Set<String> alliesOfOwner1 = alliances.get(owner1);
        if (alliesOfOwner1 != null) {
            alliesOfOwner1.remove(owner2);
            if (alliesOfOwner1.isEmpty()) {
                alliances.remove(owner1);
            }
        }
        Set<String> alliesOfOwner2 = alliances.get(owner2);
        if (alliesOfOwner2 != null) {
            alliesOfOwner2.remove(owner1);
            if (alliesOfOwner2.isEmpty()) {
                alliances.remove(owner2);
            }
        }
        AllianceClientboundPacket.removeAlliance(owner1, owner2);
    }

    public static boolean isAlliedOrOwned(String owner1, String owner2) {
        return isAllied(owner1, owner2) || owner1.equals(owner2);
    }

    public static boolean isAllied(String owner1, String owner2) {
        return alliances.getOrDefault(owner1, Collections.emptySet()).contains(owner2);
    }

    // New method to retrieve direct allies
    public static Set<String> getAllAllies(String owner) {
        return alliances.getOrDefault(owner, Collections.emptySet());
    }

    // New method to retrieve all connected allies in an alliance
    public static Set<String> getAllConnectedAllies(String owner) {
        Set<String> allAllies = new HashSet<>();
        findAllConnectedAllies(owner, allAllies);
        return allAllies;
    }

    private static void findAllConnectedAllies(String owner, Set<String> visited) {
        if (!visited.contains(owner)) {
            visited.add(owner);
            for (String ally : getAllAllies(owner)) {
                findAllConnectedAllies(ally, visited);
            }
        }
    }

    public static void applyScenarioAlliances() {
        alliances.clear();
        Map<String, Integer> nameAndTeam = new HashMap<>();
        for (ScenarioRole role : ScenarioServerEvents.scenarioRoles) {
            nameAndTeam.put(role.name, role.teamNumber);
        }
        for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers) {
            ScenarioRole role = ScenarioUtils.getScenarioRole(false, rtsPlayer.scenarioRoleIndex);
            if (role != null)
                nameAndTeam.put(rtsPlayer.name, role.teamNumber);
        }

        for (String name1 : nameAndTeam.keySet()) {
            int team1 = nameAndTeam.get(name1);

            for (String name2 : nameAndTeam.keySet()) {
                int team2 = nameAndTeam.get(name2);

                if (!name1.equals(name2)) {
                    if (team1 == team2 &&
                        !AlliancesServerEvents.isAllied(name1, name2)) {
                        addAlliance(name1, name2);
                    } else if (team1 != team2 &&
                        AlliancesServerEvents.isAllied(name1, name2)) {
                        removeAlliance(name1, name2);
                    }
                }
            }
        }
    }

    // ally all RTS players, unless they have an NPC scenario role
    public static void applyCoopAlliances() {
        alliances.clear();

        List<String> eligiblePlayers = new ArrayList<>();

        for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers) {
            ScenarioRole role = ScenarioUtils.getScenarioRole(false, rtsPlayer.scenarioRoleIndex);
            if (role == null || !role.isNpc)
                eligiblePlayers.add(rtsPlayer.name);
        }
        for (String player1 : eligiblePlayers)
            for (String player2 : eligiblePlayers)
                if (!player1.equals(player2))
                    addAlliance(player1, player2);
    }

    public static void resetAllAlliances() {
        alliances.clear();
        AllianceClientboundPacket.resetAlliances();
    }

    public static void syncAlliances() {
        for (String player1 : alliances.keySet())
            for (String player2 : alliances.get(player1))
                AllianceClientboundPacket.addAlliance(player1, player2);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        syncAlliances();
        for (String playerName : playersWithAlliedControl) {
            AllianceClientboundPacket.setAllyControl(playerName, true);
        }
    }
}
