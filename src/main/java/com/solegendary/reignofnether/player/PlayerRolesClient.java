package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import net.minecraft.client.Minecraft;

import java.util.List;

// client mirror of PlayerRoles
public class PlayerRolesClient {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final RoleTable roles = new RoleTable(PlayerClientEvents::inRtsList);

    public static PlayerRole roleOf(String name) {
        return roles.roleOf(name);
    }

    public static PlayerRole myRole() {
        return MC.player == null ? PlayerRole.SPECTATOR : roleOf(myName());
    }

    public static String ownerOf(String name) {
        return roles.ownerOf(name);
    }

    public static String commanderOf(String name) {
        return roles.commanderOf(name);
    }

    // the owner name the local player acts as
    public static String myOwner() {
        return ownerOf(myName());
    }

    public static String myCommander() {
        return commanderOf(myName());
    }

    public static List<String> soldiersOf(String owner) {
        return RoleTable.namesFor(roles.soldierOf, owner);
    }

    static void set(String name, PlayerRole role, String owner) {
        roles.set(name, role, owner);
        FogOfWarClientEvents.refreshLocalIsRTSPlayer();
    }

    public static void clear() {
        roles.clear();
        FogOfWarClientEvents.refreshLocalIsRTSPlayer();
    }

    private static String myName() {
        return MC.player == null ? "" : MC.player.getName().getString();
    }
}
