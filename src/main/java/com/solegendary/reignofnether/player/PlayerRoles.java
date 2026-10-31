package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// the only place that decides who plays for which faction. Units and buildings keep a single owner name; a
// co-commander acts as that owner and a soldier fights for them
public class PlayerRoles {

    private static final RoleTable roles = new RoleTable(PlayerServerEvents::inRtsList);

    public static PlayerRole roleOf(String name) {
        return roles.roleOf(name);
    }

    // the owner name this player acts as: the lead for a co-commander, themselves otherwise
    public static String ownerOf(String name) {
        return roles.ownerOf(name);
    }

    // the faction this player plays for: their commander for a soldier, ownerOf otherwise
    public static String commanderOf(String name) {
        return roles.commanderOf(name);
    }

    public static List<String> soldiersOf(String owner) {
        return RoleTable.namesFor(roles.soldierOf, owner);
    }

    public static List<String> coCommandersOf(String owner) {
        return RoleTable.namesFor(roles.coCommanderOf, owner);
    }

    public static void setSoldier(String name, String owner) {
        roles.set(name, PlayerRole.SOLDIER, owner);
        changed(name, PlayerRole.SOLDIER, owner);
    }

    public static void setCoCommander(String name, String owner) {
        roles.set(name, PlayerRole.COMMANDER, owner);
        changed(name, PlayerRole.COMMANDER, owner);
    }

    public static void remove(String name) {
        if (roles.soldierOf.remove(name) != null | roles.coCommanderOf.remove(name) != null)
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
        roles.clear();
        FogOfWarServerEvents.invalidateRtsCache();
        PlayerRoleClientboundPacket.clear();
    }

    // saved with rtsPlayers so a mid-match restart keeps soldiers and co-commanders in the match
    static void saveTo(Map<String, String> soldiers, Map<String, String> coCommanders) {
        soldiers.clear();
        soldiers.putAll(roles.soldierOf);
        coCommanders.clear();
        coCommanders.putAll(roles.coCommanderOf);
    }

    // server start: replaces whatever a previous (integrated) world left behind
    static void loadFrom(Map<String, String> soldiers, Map<String, String> coCommanders) {
        roles.clear();
        roles.soldierOf.putAll(soldiers);
        roles.coCommanderOf.putAll(coCommanders);
        FogOfWarServerEvents.invalidateRtsCache();
    }

    public static void syncTo(ServerPlayer player) {
        roles.soldierOf.forEach((n, o) -> PlayerRoleClientboundPacket.setFor(player, n, PlayerRole.SOLDIER, o));
        roles.coCommanderOf.forEach((n, o) -> PlayerRoleClientboundPacket.setFor(player, n, PlayerRole.COMMANDER, o));
    }

    private static void changed(String name, PlayerRole role, String owner) {
        FogOfWarServerEvents.invalidateRtsCache();
        PlayerRoleClientboundPacket.set(name, role, owner);
    }
}
