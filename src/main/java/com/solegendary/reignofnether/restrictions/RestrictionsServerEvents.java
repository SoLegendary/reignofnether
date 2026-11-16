package com.solegendary.reignofnether.restrictions;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// units, research and buildings a map denies to a player (or to everyone with "*"), set with /rtsapi restrict
public class RestrictionsServerEvents {

    public static final String ALL = "*";

    // player name, map seat name, or "*" -> production item and building ids (see RonIds)
    private static final Map<String, Set<ResourceLocation>> denied = WorldState.server(new HashMap<>());
    private static Map<String, Set<ResourceLocation>> lastSynced = Map.of();

    private static ServerLevel serverLevel = null;
    static {
        WorldState.onServerReset(() -> {
            lastSynced = Map.of();
            serverLevel = null;
        });
    }

    // the copy synced last (resolved at most once a second), not rebuilt on every check: bots and packets check often
    public static boolean isDenied(String playerName, @Nullable ResourceLocation id) {
        return id != null && (lastSynced.getOrDefault(playerName, Set.of()).contains(id) || lastSynced.getOrDefault(ALL, Set.of()).contains(id));
    }

    // a seat or role name counts for whoever plays it now (only known once someone takes it), so it's resolved when
    // checked, not when the command ran
    private static Map<String, Set<ResourceLocation>> resolved() {
        Map<String, Set<ResourceLocation>> map = new HashMap<>();
        denied.forEach((name, ids) -> {
            String player = name.equals(ALL) ? null : StartPosServerEvents.seatOccupant(name);
            map.computeIfAbsent(player != null ? player : name, p -> new HashSet<>()).addAll(ids);
        });
        return map;
    }

    // clients get the resolved copy, sent again when someone takes a named seat or role
    private static void sync() {
        lastSynced = resolved();
        RestrictionsClientboundPacket.syncAll(lastSynced);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END && evt.getServer().getTickCount() % 20 == 0 && !denied.isEmpty() && !resolved().equals(lastSynced))
            sync();
    }

    public static Map<String, Set<ResourceLocation>> getDenied() {
        return denied;
    }

    public static void deny(String playerName, ResourceLocation id) {
        denied.computeIfAbsent(playerName, p -> new HashSet<>()).add(id);
        changed();
    }

    public static void allow(String playerName, ResourceLocation id) {
        Set<ResourceLocation> ids = denied.get(playerName);
        if (ids != null && ids.remove(id) && ids.isEmpty())
            denied.remove(playerName);
        changed();
    }

    public static void clear(String playerName) {
        denied.remove(playerName);
        changed();
    }

    // on match reset: the map's editor/main files set them again at the next match start
    public static void clearAll() {
        denied.clear();
        changed();
    }

    private static void changed() {
        sync();
        if (serverLevel != null) {
            RestrictionsSaveData data = RestrictionsSaveData.getInstance(serverLevel);
            data.denied.clear();
            denied.forEach((p, ids) -> data.denied.put(p, new HashSet<>(ids)));
            data.save();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        serverLevel = evt.getServer().overworld();
        denied.clear();
        RestrictionsSaveData.getInstance(serverLevel).denied.forEach((p, ids) -> denied.put(p, new HashSet<>(ids)));
        lastSynced = resolved();
        if (!denied.isEmpty())
            ReignOfNether.LOGGER.info("loaded restrictions for {} player(s)", denied.size());
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player)
            RestrictionsClientboundPacket.syncTo(player, resolved());
    }
}
