package com.solegendary.reignofnether.taskscheduler;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

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
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;

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
