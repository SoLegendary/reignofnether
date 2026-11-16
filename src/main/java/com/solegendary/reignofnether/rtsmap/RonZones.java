package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Map areas that fire ron/events/zone_entered/<name> and zone_left/<name> as a unit walks in or out (@s the unit,
 * $owner its owner). Set by scripts (rtsapi zone add), so define them in map_loaded to have them in every session.
 */
public class RonZones {

    private static final Map<String, AABB> zones = WorldState.server(new HashMap<>());
    private static final Map<String, Set<Integer>> inside = WorldState.server(new HashMap<>());

    public static void add(String name, BlockPos from, BlockPos to) {
        zones.put(name, new AABB(from).minmax(new AABB(to)));
        inside.put(name, new HashSet<>());
    }

    public static boolean remove(String name) {
        inside.remove(name);
        return zones.remove(name) != null;
    }

    public static void clear() {
        zones.clear();
        inside.clear();
    }

    // whether a unit of owner's is in the zone now 
    public static boolean has(String name, @Nullable String owner) {
        AABB box = zones.get(name);
        if (box == null || owner == null)
            return false;
        return UnitServerEvents.getAllUnits().stream().anyMatch(e -> e instanceof Unit u && owner.equals(u.getOwnerName()) && box.contains(e.position()));
    }

    public static Set<String> names() {
        return zones.keySet();
    }

    // ponytail: every unit against every zone each check; a spatial index if maps ever have hundreds of zones
    public static void tick(MinecraftServer server) {
        if (zones.isEmpty())
            return;
        for (Map.Entry<String, AABB> zone : zones.entrySet()) {
            Set<Integer> was = inside.computeIfAbsent(zone.getKey(), k -> new HashSet<>());
            Set<Integer> now = new HashSet<>();
            for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                if (!(entity instanceof Unit unit) || !zone.getValue().contains(entity.position()))
                    continue;
                now.add(entity.getId());
                if (!was.contains(entity.getId()))
                    RonFiles.fire(server, "zone_entered/" + zone.getKey(), entity.createCommandSourceStack(), unit.getOwnerName());
            }
            for (int id : was)
                if (!now.contains(id)) {
                    var entity = server.overworld().getEntity(id);
                    if (entity instanceof Unit unit)
                        RonFiles.fire(server, "zone_left/" + zone.getKey(), entity.createCommandSourceStack(), unit.getOwnerName());
                }
            inside.put(zone.getKey(), now);
        }
    }
}
