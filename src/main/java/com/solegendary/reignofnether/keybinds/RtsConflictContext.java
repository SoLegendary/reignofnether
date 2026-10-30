package com.solegendary.reignofnether.keybinds;

import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

// Splits the mod's bindings into disjoint "contexts" so the Controls UI
// doesn't flag intentional default-key overlaps (LEFT for both panPlusX
// and rotCCW) as conflicts.
public enum RtsConflictContext implements IKeyConflictContext {
    PAN,
    ROTATE,
    COMMAND,
    ITEM, // hero item keys share the command card's bottom row on purpose
    SOLDIER; // soldier keys only fire with no screen open, RTS keys only in the RTS view

    @Override
    public boolean isActive() {
        return KeyConflictContext.IN_GAME.isActive();
    }

    @Override
    public boolean conflicts(IKeyConflictContext other) {
        return other == this;
    }
}
