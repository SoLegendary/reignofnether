package com.solegendary.reignofnether.orthoview;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;

/**
 * Embeddium/Rubidium's block face culling hides faces based on where the camera is, which leaves holes
 * in the map from the RTS camera. It is turned off while the RTS view is on and the player's own value
 * is put back after. Reflection, so neither mod is a dependency.
 */
public final class FaceCulling {
    private FaceCulling() { }

    private static Boolean saved = null;

    public static void setRtsView(boolean rts) {
        if (!ModList.get().isLoaded("embeddium") && !ModList.get().isLoaded("rubidium"))
            return;
        try {
            Object options = Class.forName("me.jellysquid.mods.sodium.client.SodiumClientMod").getMethod("options").invoke(null);
            Object performance = options.getClass().getField("performance").get(options);
            Field culling = performance.getClass().getField("useBlockFaceCulling");
            boolean target;
            if (rts) {
                if (saved == null)
                    saved = culling.getBoolean(performance);
                target = false;
            } else {
                if (saved == null)
                    return;
                target = saved;
                saved = null;
            }
            if (culling.getBoolean(performance) != target) {
                culling.setBoolean(performance, target); // in memory only, the player's file is never changed
                Minecraft.getInstance().levelRenderer.allChanged();
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            ReignOfNether.LOGGER.warn("Couldn't toggle Embeddium block face culling: {}", e.toString());
        }
    }
}
