package com.solegendary.reignofnether.ui;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * One line of a settings list: a name with an ON/OFF toggle (decrease == null) or a value with − and +.
 *
 * @param tag      small label shown in the tooltip (e.g. MATCH / SERVER), or null
 * @param lockedHint why the row can't be changed right now, shown when editable is false
 * @param swatch   RGB colour drawn beside the value (colour pickers), or null
 */
public record SettingRow(String name, String description, @Nullable String tag,
                         Supplier<String> value, Supplier<Boolean> on,
                         Runnable increase, @Nullable Runnable decrease,
                         Supplier<Boolean> editable, String lockedHint, @Nullable Supplier<Integer> swatch) {

    public SettingRow(String name, String description, @Nullable String tag, Supplier<String> value, Supplier<Boolean> on,
                      Runnable increase, @Nullable Runnable decrease, Supplier<Boolean> editable, String lockedHint) {
        this(name, description, tag, value, on, increase, decrease, editable, lockedHint, null);
    }

    public boolean isToggle() {
        return decrease == null;
    }
}
