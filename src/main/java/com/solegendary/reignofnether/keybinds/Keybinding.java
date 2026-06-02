package com.solegendary.reignofnether.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

public class Keybinding {
    public final KeyMapping mapping;        // null for raw GLFW bindings
    private final int rawKey;               // fallback when mapping == null
    public final String buttonLabel;        // static display label (also parsed as int for nums[])
    public final String description;        // legacy descriptor, kept for back-compat

    // Forge-backed binding: appears in vanilla Controls UI, persisted via options.txt
    public Keybinding(String translationKey, int defaultKey, String category,
                      IKeyConflictContext ctx, String buttonLabel) {
        this.mapping = new KeyMapping(translationKey, ctx,
                InputConstants.Type.KEYSYM.getOrCreate(defaultKey), category);
        this.rawKey = defaultKey;
        this.buttonLabel = buttonLabel;
        this.description = translationKey;
    }

    // Convenience overload defaulting to IN_GAME conflict context
    public Keybinding(String translationKey, int defaultKey, String category, String buttonLabel) {
        this(translationKey, defaultKey, category, KeyConflictContext.IN_GAME, buttonLabel);
    }

    // Raw GLFW binding: not rebindable, not shown in Controls UI.
    // Used for modifier keys (shift/ctrl/alt held-state) and ESC/pause.
    public Keybinding(int rawKey, String buttonLabel, String description) {
        this.mapping = null;
        this.rawKey = rawKey;
        this.buttonLabel = buttonLabel;
        this.description = description;
    }

    public int getKey() {
        return mapping != null ? mapping.getKey().getValue() : rawKey;
    }

    public boolean isDown() {
        int k = getKey();
        if (k == InputConstants.UNKNOWN.getValue()) return false;
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), k);
    }
}
