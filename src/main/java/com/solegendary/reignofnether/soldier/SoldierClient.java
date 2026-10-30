package com.solegendary.reignofnether.soldier;

import com.solegendary.reignofnether.ping.PingType;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

// the local soldier's state from SoldierStateClientboundPacket
public class SoldierClient {

    public static boolean dead = false;
    public static int respawnSeconds = 0;
    public static long respawnReceivedMs = 0;
    public static int respawnTotalSeconds = 0;
    public static String killer = "";
    public static String respawnBuilding = "";
    @Nullable public static PingType order = null;
    public static BlockPos orderPos = BlockPos.ZERO;
    public static long orderReceivedMs = 0;
    public static int orderAgeSeconds = 0;

    public static void reset() {
        dead = false;
        order = null;
        killer = "";
        respawnBuilding = "";
    }

    static void update(SoldierStateClientboundPacket p) {
        if (!p.active) {
            reset();
            return;
        }
        if (p.dead && !dead)
            respawnTotalSeconds = Math.max(1, p.respawnSeconds);
        dead = p.dead;
        respawnSeconds = p.respawnSeconds;
        respawnReceivedMs = System.currentTimeMillis();
        killer = p.killer;
        respawnBuilding = p.respawnBuilding;
        order = p.order;
        orderPos = p.orderPos;
        orderAgeSeconds = p.orderAgeSeconds;
        orderReceivedMs = System.currentTimeMillis();
    }

    // seconds until respawn, counting down between server updates
    public static float respawnSecondsLeft() {
        return Math.max(0, respawnSeconds - (System.currentTimeMillis() - respawnReceivedMs) / 1000f);
    }

    public static int orderAge() {
        return orderAgeSeconds + (int) ((System.currentTimeMillis() - orderReceivedMs) / 1000);
    }
}
