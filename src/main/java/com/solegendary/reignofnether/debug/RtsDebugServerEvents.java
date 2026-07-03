package com.solegendary.reignofnether.debug;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RtsDebugServerEvents {

    private static final long[] UNLOADED = new long[]{0L};

    private static int updateTicks = 0;

    // Debug counter: incremented each time a goal calls mob.getNavigation().createPath().
    // Sampled and reset once per second by the rts-debug stats tick handler.
    public static int debugPathCalcsThisSecond = 0;

    // 5-second rolling buffers for the debug overlay. Indexed mod STATS_WINDOW.
    private static final int STATS_WINDOW = 5;
    private static final int[] pathsHistory = new int[STATS_WINDOW];
    private static final int[] queueHistory = new int[STATS_WINDOW];
    private static final int[] stuckHistory = new int[STATS_WINDOW];
    private static int statsIndex = 0;
    // Queue is sampled every tick (cheap, captures bursts) and averaged at second boundaries.
    private static long queueSumThisSecond = 0;
    private static int queueSamplesThisSecond = 0;

    // logic borrowed from net.minecraftforge.server.command.TPSCommand
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END)
            return;

        MinecraftServer server = evt.getServer();
        long[] times = server.getTickTime(Level.OVERWORLD);
        if (times == null)
            times = UNLOADED;

        double worldTickTime = (double)mean(times) * 1.0E-6;

        updateTicks += 1;
        if (updateTicks < 20) return;
        updateTicks = 0;
        int paths = debugPathCalcsThisSecond;
        debugPathCalcsThisSecond = 0;
        int stuck = 0;
        for (LivingEntity e : UnitServerEvents.getAllUnits()) {
            if (e instanceof Unit u) {
                var mg = u.getMoveGoal();
                if (mg != null && mg.isInBackoff()) stuck += 1;
            }
        }
        int avgQueueThisSec = queueSamplesThisSecond > 0 ? (int) (queueSumThisSecond / queueSamplesThisSecond) : 0;
        queueSumThisSecond = 0;
        queueSamplesThisSecond = 0;
        pathsHistory[statsIndex] = paths;
        queueHistory[statsIndex] = avgQueueThisSec;
        stuckHistory[statsIndex] = stuck;
        statsIndex = (statsIndex + 1) % STATS_WINDOW;
        RtsDebugStatsClientboundPacket.broadcast(
                avg(pathsHistory), avg(queueHistory), avg(stuckHistory), worldTickTime);
    }

    private static long mean(long[] values) {
        long sum = 0L;
        for (long v : values)
            sum += v;
        return sum / (long)values.length;
    }

    private static int avg(int[] buf) {
        int sum = 0;
        for (int v : buf) sum += v;
        return sum / buf.length;
    }
}
