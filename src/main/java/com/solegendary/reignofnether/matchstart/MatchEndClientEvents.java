package com.solegendary.reignofnether.matchstart;

import com.solegendary.reignofnether.player.MatchStatsClientboundPacket.MatchStatRow;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

// Receives the end-of-match scoreboard from the server and opens MatchEndScreen.
// The data is cached so the popup can be dismissed and reopened, and survives until
// the next match end or logout.
public class MatchEndClientEvents {

    private static long gameDurationTicks = 0;
    private static List<MatchStatRow> rows = new ArrayList<>();
    private static boolean pendingOpen = false;

    public static long getGameDurationTicks() {
        return gameDurationTicks;
    }

    public static List<MatchStatRow> getRows() {
        return rows;
    }

    public static boolean hasResults() {
        return !rows.isEmpty();
    }

    // called on the client main thread from the packet handler
    public static void receive(long ticks, List<MatchStatRow> newRows) {
        /*
        gameDurationTicks = ticks;
        rows = newRows != null ? newRows : new ArrayList<>();
        pendingOpen = true;
         */
    }

    public static void dismiss() {
        /*
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MatchEndScreen) {
            mc.setScreen(null);
        }
         */
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        /*
        if (evt.phase != TickEvent.Phase.END) return;
        if (!pendingOpen) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        pendingOpen = false;
        mc.setScreen(new MatchEndScreen());
         */
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        /*
        gameDurationTicks = 0;
        rows = new ArrayList<>();
        pendingOpen = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MatchEndScreen) {
            mc.setScreen(null);
        }
         */
    }
}
