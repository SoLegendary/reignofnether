package com.solegendary.reignofnether.alliance;

import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

public class AlliancesClient {
    private static final Minecraft MC = Minecraft.getInstance();

    private static final Map<String, Set<String>> alliances = new HashMap<>();
    public static final Set<String> inboundPendingAlliances = new HashSet<>();
    public static final Set<String> outboundPendingAlliances = new HashSet<>();
    public static final HashSet<String> playersWithAlliedControl = new HashSet<>();

    public static boolean sharingAllyControl() {
        return MC.player != null && AlliancesClient.playersWithAlliedControl.contains(MC.player.getName().getString());
    }
    public static boolean canControlAlly(LivingEntity entity) {
        return entity instanceof Unit unit && canControlAlly(unit.getOwnerName());
    }
    public static boolean canControlAlly(Unit unit) {
        return canControlAlly(unit.getOwnerName());
    }
    public static boolean canControlAlly(String ownerName) {
        return MC.player != null &&
                (AlliancesClient.isAllied(MC.player.getName().getString(), ownerName) &&
                        AlliancesClient.playersWithAlliedControl.contains(ownerName));
    }

    public static void addAlliance(String owner1, String owner2) {
        alliances.computeIfAbsent(owner1, k -> new HashSet<>()).add(owner2);
        alliances.computeIfAbsent(owner2, k -> new HashSet<>()).add(owner1);
        inboundPendingAlliances.removeIf(p -> p.equals(owner1) || p.equals(owner2));
        outboundPendingAlliances.removeIf(p -> p.equals(owner1) || p.equals(owner2));
    }

    public static void addPendingAlliance(String toPlayer, String fromPlayer) {
        if (MC.player != null && MC.player.getName().getString().equals(toPlayer))
            inboundPendingAlliances.add(fromPlayer);
    }

    public static void cancelPendingAlliance(String toPlayer, String fromPlayer) {
        if (MC.player != null && MC.player.getName().getString().equals(toPlayer))
            inboundPendingAlliances.removeIf(p -> p.equals(fromPlayer));
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
    }

    public static boolean isAllied(String owner1, String owner2) {
        return alliances.getOrDefault(owner1, Collections.emptySet()).contains(owner2);
    }

    public static Set<String> getAllAllies(String owner) {
        return alliances.getOrDefault(owner, Collections.emptySet());
    }

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
    public static void resetAllAlliances() {
        alliances.clear();
    }

}
