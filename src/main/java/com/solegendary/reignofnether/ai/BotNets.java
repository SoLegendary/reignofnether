package com.solegendary.reignofnether.ai;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// Loads data/<namespace>/bot_nets/<faction>.json, the supervisor nets RoN-Bots exports. No file: that faction's
// bots play the script. A broken file logs and counts as none
public class BotNets extends SimpleJsonResourceReloadListener {

    private static final Map<ResourceLocation, BotGridNet> nets = new HashMap<>(); // faction key -> net

    public BotNets() {
        super(new Gson(), "bot_nets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        nets.clear();
        files.forEach((id, json) -> {
            try {
                nets.put(id, BotGridNet.parse(json.getAsJsonObject()));
                ReignOfNether.LOGGER.info("[Bot] net {} loaded", id);
            } catch (RuntimeException e) {
                ReignOfNether.LOGGER.error("[Bot] net {} is broken, bots play that faction with the script", id, e);
            }
        });
    }

    @Nullable
    public static BotGridNet get(Faction faction) {
        return nets.get(faction.key);
    }
}
