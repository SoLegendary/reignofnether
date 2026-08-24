package com.solegendary.reignofnether.keybinds;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.DistExecutor;

public class Keybinding {
    private KeyMapping mapping;        // null for raw GLFW bindings or on serverside
    private boolean mappingInitialised = false; // don't initialise mapping until needed (so that it remains null on server)
    private final String translationKey;
    private final String category;
    private final IKeyConflictContext ctx;
    private final int rawKey;               // fallback when mapping == null
    public final String buttonLabel;        // static display label (also parsed as int for nums[])
    public final String description;        // legacy descriptor, kept for back-compat

    // Forge-backed binding: appears in vanilla Controls UI, persisted via options.txt
    public Keybinding(String translationKey, int defaultKey, String category,
                      IKeyConflictContext ctx, String buttonLabel) {
        this.category = category;
        this.translationKey = translationKey;
        this.ctx = ctx;
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
        this.translationKey = null;
        this.category = null;
        this.ctx = null;
    }

    public String getCurrentLabel() {
        if (mapping == null) {
            return buttonLabel;
        }
        InputConstants.Key key = mapping.getKey();
        if (key.equals(InputConstants.UNKNOWN)) {
            return "";
        }
        String full = key.getDisplayName().getString();
        return full.substring(0, Math.min(3, full.length()));
    }

    public KeyMapping getMapping() {
        if (!mappingInitialised) {
            mappingInitialised = true;
            if (translationKey != null && ctx != null && category != null) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::initMapping);
            }
        }
        return mapping;
    }

    @OnlyIn(Dist.CLIENT)
    private void initMapping() {
        this.mapping = new KeyMapping(translationKey, ctx,
                InputConstants.Type.KEYSYM.getOrCreate(rawKey), category);
    }

    public int getKey() {
        return getMapping() != null ? getMapping().getKey().getValue() : rawKey;
    }

    public boolean isDown() {
        int k = getKey();
        if (k == InputConstants.UNKNOWN.getValue()) return false;
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), k);
    }
}
