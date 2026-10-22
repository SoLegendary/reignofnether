package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// the only place that decides who plays for which faction. Units and buildings keep a single owner name; a
// co-commander acts as that owner and a soldier fights for them
public class PlayerRoles {

    private static final Map<String, String> soldierOf = new ConcurrentHashMap<>();     // soldier -> owner
    private static final Map<String, String> coCommanderOf = new ConcurrentHashMap<>(); // co-commander -> owner

    public static PlayerRole roleOf(String name) {
        if (coCommanderOf.containsKey(name) || PlayerServerEvents.inRtsList(name))
            return PlayerRole.COMMANDER;
        if (soldierOf.containsKey(name))
            return PlayerRole.SOLDIER;
        return PlayerRole.SPECTATOR;
    }

    // the owner name this player acts as: the lead for a co-commander, themselves otherwise
    public static String ownerOf(String name) {
        return coCommanderOf.getOrDefault(name, name);
    }

    // the faction this player plays for: their commander for a soldier, ownerOf otherwise
    public static String commanderOf(String name) {
        String owner = soldierOf.get(name);
        return owner != null ? owner : ownerOf(name);
    }

    public static List<String> soldiersOf(String owner) {
        return namesFor(soldierOf, owner);
    }

    public static List<String> coCommandersOf(String owner) {
        return namesFor(coCommanderOf, owner);
    }

    public static void setSoldier(String name, String owner) {
        coCommanderOf.remove(name);
        soldierOf.put(name, owner);
        changed(name, PlayerRole.SOLDIER, owner);
    }

    public static void setCoCommander(String name, String owner) {
        soldierOf.remove(name);
        coCommanderOf.put(name, owner);
        changed(name, PlayerRole.COMMANDER, owner);
    }

    public static void remove(String name) {
        if (soldierOf.remove(name) != null | coCommanderOf.remove(name) != null)
            changed(name, PlayerRole.SPECTATOR, name);
    }

    // an owner was defeated: everyone playing for them becomes a spectator. Returns who was dropped
    public static List<String> dropFaction(String owner) {
        List<String> dropped = new ArrayList<>(soldiersOf(owner));
        dropped.addAll(coCommandersOf(owner));
        dropped.forEach(PlayerRoles::remove);
        return dropped;
    }

    public static void clear() {
        soldierOf.clear();
        coCommanderOf.clear();
        FogOfWarServerEvents.invalidateRtsCache();
        PlayerRoleClientboundPacket.clear();
    }

    public static void syncTo(ServerPlayer player) {
        soldierOf.forEach((n, o) -> PlayerRoleClientboundPacket.setFor(player, n, PlayerRole.SOLDIER, o));
        coCommanderOf.forEach((n, o) -> PlayerRoleClientboundPacket.setFor(player, n, PlayerRole.COMMANDER, o));
    }

    private static void changed(String name, PlayerRole role, String owner) {
        FogOfWarServerEvents.invalidateRtsCache();
        PlayerRoleClientboundPacket.set(name, role, owner);
    }

    private static List<String> namesFor(Map<String, String> map, String owner) {
        List<String> names = new ArrayList<>();
        map.forEach((n, o) -> { if (o.equals(owner)) names.add(n); });
        return names;
    }
}
