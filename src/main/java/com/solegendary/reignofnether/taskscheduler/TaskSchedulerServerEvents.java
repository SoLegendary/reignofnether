package com.solegendary.reignofnether.taskscheduler;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaskSchedulerServerEvents {

    private static final List<ScheduledTask> tasks = new ArrayList<>();

    private record ScheduledTask(int ticksRemaining, Runnable task) {}

    public static void schedule(int delayTicks, Runnable task) {
        tasks.add(new ScheduledTask(delayTicks, task));
    }

    @SubscribeEvent
    // Forge's TickEvent.ServerTickEvent is gone in NeoForge; ServerTickEvent.Post is the same
    // "end of the server tick" point and is what the rest of this mod already uses.
    public static void onServerTick(ServerTickEvent.Post evt) {
        Iterator<ScheduledTask> it = tasks.iterator();
        List<ScheduledTask> next = new ArrayList<>();
        while (it.hasNext()) {
            ScheduledTask t = it.next();
            if (t.ticksRemaining() <= 0) {
                t.task().run();
            } else {
                next.add(new ScheduledTask(t.ticksRemaining() - 1, t.task()));
            }
        }
        tasks.clear();
        tasks.addAll(next);
    }
}
