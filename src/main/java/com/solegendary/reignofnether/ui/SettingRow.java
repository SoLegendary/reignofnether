package com.solegendary.reignofnether.ui;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * One line of a settings list: a name with an ON/OFF toggle (decrease == null) or a value with − and +.
 *
 * @param tag      small label shown in the tooltip (e.g. MATCH / SERVER), or null
 * @param lockedHint why the row can't be changed right now, shown when editable is false
 */
public record SettingRow(String name, String description, @Nullable String tag,
                         Supplier<String> value, Supplier<Boolean> on,
                         Runnable increase, @Nullable Runnable decrease,
                         Supplier<Boolean> editable, String lockedHint) {

    public boolean isToggle() {
        return decrease == null;
    }
}
