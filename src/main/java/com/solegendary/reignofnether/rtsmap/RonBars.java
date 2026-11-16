package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// a map's objectives and countdowns, kept as vanilla bossbars (reignofnether:objective/<id>, reignofnether:timer/<id>):
// they save with the world, follow bossbar set ... players / value and go with bossbar remove. The name's insertion
// ("ron:objective <goal>", "ron:timer <seconds left>") tells the RTS view to draw them as its Objectives panel and
// countdown (ObjectivesHud) instead of plain bars; it's invisible everywhere else
public class RonBars {

    private record Timer(String title, String file, CommandSourceStack source, @Nullable String owner) { }

    private static final Map<ResourceLocation, Timer> timers = WorldState.server(new HashMap<>());

    @Nullable
    public static ResourceLocation id(String kind, String id) {
        return ResourceLocation.tryBuild("reignofnether", kind + "/" + id);
    }

    private static Component name(String text, String tag) {
        return Component.literal(text).withStyle(s -> s.withInsertion("ron:" + tag));
    }

    private static CustomBossEvent bar(MinecraftServer server, ResourceLocation id, Component name) {
        CustomBossEvent bar = server.getCustomBossEvents().get(id);
        if (bar == null)
            bar = server.getCustomBossEvents().create(id, name);
        bar.setName(name);
        return bar;
    }

    // goal 0: no count, just done or not; done is a full bar, so bossbar set ... value <goal> completes it too
    public static void objective(MinecraftServer server, ResourceLocation id, int goal, String text, Collection<ServerPlayer> players) {
        CustomBossEvent bar = bar(server, id, name(text, "objective " + goal));
        bar.setColor(BossEvent.BossBarColor.WHITE);
        bar.setMax(Math.max(1, goal));
        bar.setValue(0);
        bar.setPlayers(players);
    }

    public static boolean progress(MinecraftServer server, ResourceLocation id, int amount) {
        CustomBossEvent bar = server.getCustomBossEvents().get(id);
        if (bar == null)
            return false;
        bar.setValue(Math.max(0, Math.min(bar.getMax(), bar.getValue() + amount)));
        return true;
    }

    public static boolean complete(MinecraftServer server, ResourceLocation id) {
        CustomBossEvent bar = server.getCustomBossEvents().get(id);
        if (bar == null)
            return false;
        bar.setValue(bar.getMax());
        return true;
    }

    // counts down once a second during a match (tick), shown to everyone, then runs ron/<file> as source and owner
    public static void timer(MinecraftServer server, ResourceLocation id, int seconds, String title, String file,
                             CommandSourceStack source, @Nullable String owner) {
        CustomBossEvent bar = bar(server, id, name(title, "timer " + seconds));
        bar.setColor(BossEvent.BossBarColor.YELLOW);
        bar.setMax(Math.max(1, seconds));
        bar.setValue(seconds);
        bar.setPlayers(server.getPlayerList().getPlayers());
        timers.put(id, new Timer(title, file, source, owner));
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<ResourceLocation, Timer> e : List.copyOf(timers.entrySet())) {
            CustomBossEvent bar = server.getCustomBossEvents().get(e.getKey());
            Timer t = e.getValue();
            if (bar == null) { // bossbar remove stops it
                timers.remove(e.getKey());
                continue;
            }
            int left = bar.getValue() - 1;
            if (left <= 0) {
                timers.remove(e.getKey());
                server.getCustomBossEvents().remove(bar);
                bar.removeAllPlayers();
                RonFiles.run(server, t.file, t.source, t.owner);
                continue;
            }
            bar.setValue(left);
            bar.setName(name(t.title, "timer " + left));
            bar.setPlayers(server.getPlayerList().getPlayers()); // and whoever joined since
        }
    }

    // at match end: the map's bars don't carry over into the next match on this world
    public static void clear(MinecraftServer server) {
        timers.clear();
        for (CustomBossEvent bar : List.copyOf(server.getCustomBossEvents().getEvents()))
            if (bar.getTextId().getNamespace().equals("reignofnether")) {
                bar.removeAllPlayers();
                server.getCustomBossEvents().remove(bar);
            }
    }
}
