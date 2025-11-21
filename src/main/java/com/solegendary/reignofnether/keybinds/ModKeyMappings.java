package com.solegendary.reignofnether.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {

    private ModKeyMappings() {
    }

    public static final String CATEGORY = "key.category.reignofnether";

    public static final KeyMapping MARKER_KEY = new KeyMapping(
        "key.reignofnether.marker",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        CATEGORY
    );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(MARKER_KEY);
    }
}

