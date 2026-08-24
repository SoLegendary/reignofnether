package com.solegendary.reignofnether.keybinds;

import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

// Splits the mod's bindings into disjoint "contexts" so the Controls UI
// doesn't flag intentional default-key overlaps (LEFT for both panPlusX
// and rotCCW; M for minimapToggle/move) as conflicts.
public enum RtsConflictContext implements IKeyConflictContext {
    PAN,
    ROTATE,
    COMMAND;

    @Override
    public boolean isActive() {
        return KeyConflictContext.IN_GAME.isActive();
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return other == this;
    }
}
