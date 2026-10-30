package com.solegendary.reignofnether.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// who plays for which faction, by name. PlayerRoles (server) and PlayerRolesClient each keep one
class RoleTable {

    final Map<String, String> soldierOf = new ConcurrentHashMap<>();     // soldier -> owner
    final Map<String, String> coCommanderOf = new ConcurrentHashMap<>(); // co-commander -> owner
    private final Predicate<String> inRtsList;

    RoleTable(Predicate<String> inRtsList) {
        this.inRtsList = inRtsList;
    }

    PlayerRole roleOf(String name) {
        if (coCommanderOf.containsKey(name) || inRtsList.test(name))
            return PlayerRole.COMMANDER;
        if (soldierOf.containsKey(name))
            return PlayerRole.SOLDIER;
        return PlayerRole.SPECTATOR;
    }

    String ownerOf(String name) {
        return coCommanderOf.getOrDefault(name, name);
    }

    String commanderOf(String name) {
        String owner = soldierOf.get(name);
        return owner != null ? owner : ownerOf(name);
    }

    // SOLDIER or COMMANDER (a co-commander) under owner; anything else removes the name
    void set(String name, PlayerRole role, String owner) {
        soldierOf.remove(name);
        coCommanderOf.remove(name);
        if (role == PlayerRole.SOLDIER)
            soldierOf.put(name, owner);
        else if (role == PlayerRole.COMMANDER)
            coCommanderOf.put(name, owner);
    }

    void clear() {
        soldierOf.clear();
        coCommanderOf.clear();
    }

    static List<String> namesFor(Map<String, String> map, String owner) {
        List<String> names = new ArrayList<>();
        map.forEach((n, o) -> { if (o.equals(owner)) names.add(n); });
        return names;
    }
}
