package com.solegendary.reignofnether.config.elements;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Checkbox bound to a {@code ModConfigSpec.ConfigValue<Boolean>}, with a different label for the
 * on and off states.
 *
 * <p>In 1.20.1 this subclassed {@code Checkbox} and called its {@code (x, y, w, h, message,
 * selected, showTooltip)} constructor. 1.21.1 dropped that constructor — the checkbox now sizes
 * itself from a {@code Font} and is built through {@code Checkbox.builder} — and the result is no
 * longer a type we can subclass usefully, since the constructor is package-private. So this is
 * now a small builder that wraps a vanilla {@link Checkbox} and forwards positioning to it.
 */
public class ConfigCheckbox {

    private final ModConfigSpec.ConfigValue<Boolean> configValue;
    private final String labelOn;
    private final String labelOff;
    private final Checkbox checkbox;

    public ConfigCheckbox(ModConfigSpec.ConfigValue<Boolean> configValue, String label) {
        this(configValue, label, label);
    }

    public ConfigCheckbox(ModConfigSpec.ConfigValue<Boolean> configValue, String labelOn, String labelOff) {
        this.configValue = configValue;
        this.labelOn = labelOn;
        this.labelOff = labelOff;

        this.checkbox = Checkbox
                .builder(Component.literal(configValue.get() ? labelOn : labelOff), Minecraft.getInstance().font)
                .selected(configValue.get())
                .onValueChange((button, selected) -> {
                    configValue.set(selected);
                    button.setMessage(Component.literal(selected ? labelOn : labelOff));
                })
                .build();
    }

    /**
     * Positions the wrapped checkbox and keeps the chain going; {@link #size(int, int)} finishes it
     * and hands back the widget to add to the screen.
     */
    public ConfigCheckbox pos(int x, int y) {
        checkbox.setX(x);
        checkbox.setY(y);
        return this;
    }

    public Checkbox size(int w, int h) {
        checkbox.setWidth(w);
        checkbox.setHeight(h);
        return checkbox;
    }
}