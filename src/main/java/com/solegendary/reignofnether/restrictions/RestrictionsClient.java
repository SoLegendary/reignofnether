package com.solegendary.reignofnether.restrictions;

import com.solegendary.reignofnether.player.PlayerRolesClient;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// client copy of RestrictionsServerEvents, for greying out denied production and building buttons
public class RestrictionsClient {

    private static final Map<String, Set<ResourceLocation>> denied = WorldState.client(new HashMap<>());

    public static void set(Map<String, Set<ResourceLocation>> newDenied) {
        denied.clear();
        newDenied.forEach((p, ids) -> denied.put(p, new HashSet<>(ids)));
    }

    public static boolean isDenied(@Nullable ResourceLocation id) {
        return id != null && (denied.getOrDefault(PlayerRolesClient.myOwner(), Set.of()).contains(id)
                || denied.getOrDefault("*", Set.of()).contains(id)); // "*": everyone
    }
}
