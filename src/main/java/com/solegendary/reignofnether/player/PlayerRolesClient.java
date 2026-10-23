package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// client mirror of PlayerRoles
public class PlayerRolesClient {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final Map<String, String> soldierOf = new ConcurrentHashMap<>();
    private static final Map<String, String> coCommanderOf = new ConcurrentHashMap<>();

    public static PlayerRole roleOf(String name) {
        if (coCommanderOf.containsKey(name) || PlayerClientEvents.inRtsList(name))
            return PlayerRole.COMMANDER;
        if (soldierOf.containsKey(name))
            return PlayerRole.SOLDIER;
        return PlayerRole.SPECTATOR;
    }

    public static PlayerRole myRole() {
        return MC.player == null ? PlayerRole.SPECTATOR : roleOf(myName());
    }

    public static String ownerOf(String name) {
        return coCommanderOf.getOrDefault(name, name);
    }

    public static String commanderOf(String name) {
        String owner = soldierOf.get(name);
        return owner != null ? owner : ownerOf(name);
    }

    // the owner name the local player acts as
    public static String myOwner() {
        return ownerOf(myName());
    }

    public static String myCommander() {
        return commanderOf(myName());
    }

    public static List<String> soldiersOf(String owner) {
        List<String> names = new ArrayList<>();
        soldierOf.forEach((n, o) -> { if (o.equals(owner)) names.add(n); });
        return names;
    }

    static void set(String name, PlayerRole role, String owner) {
        soldierOf.remove(name);
        coCommanderOf.remove(name);
        if (role == PlayerRole.SOLDIER)
            soldierOf.put(name, owner);
        else if (role == PlayerRole.COMMANDER)
            coCommanderOf.put(name, owner);
        FogOfWarClientEvents.refreshLocalIsRTSPlayer();
    }

    public static void clear() {
        soldierOf.clear();
        coCommanderOf.clear();
        FogOfWarClientEvents.refreshLocalIsRTSPlayer();
    }

    private static String myName() {
        return MC.player == null ? "" : MC.player.getName().getString();
    }
}
