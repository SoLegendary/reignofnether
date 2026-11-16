package com.solegendary.reignofnether.util;

import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

// Statics that belong to one world. Every world opened in a game launch shares the same statics, so wrap a world's
// collections here where they're declared (and register a reset next to any other field) and they clear when that
// world closes: client side on logout (PlayerClientEvents.resetAll), server side on ServerStoppedEvent.
// Client and server are kept apart because singleplayer runs both in one JVM.
public final class WorldState {
    private static final List<Runnable> CLIENT = new CopyOnWriteArrayList<>();
    private static final List<Runnable> SERVER = new CopyOnWriteArrayList<>();

    private WorldState() { }

    public static <T extends Collection<?>> T client(T c) {
        CLIENT.add(c::clear);
        return c;
    }

    public static <T extends Map<?, ?>> T client(T m) {
        CLIENT.add(m::clear);
        return m;
    }

    public static void onClientReset(Runnable reset) {
        CLIENT.add(reset);
    }

    public static <T extends Collection<?>> T server(T c) {
        SERVER.add(c::clear);
        return c;
    }

    public static <T extends Map<?, ?>> T server(T m) {
        SERVER.add(m::clear);
        return m;
    }

    public static void onServerReset(Runnable reset) {
        SERVER.add(reset);
    }

    public static void resetClient() {
        CLIENT.forEach(Runnable::run);
    }

    public static void resetServer() {
        SERVER.forEach(Runnable::run);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent evt) {
        resetServer();
    }
}
