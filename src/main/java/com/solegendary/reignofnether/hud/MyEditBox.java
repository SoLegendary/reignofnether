package com.solegendary.reignofnether.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Consumer;

public class MyEditBox extends EditBox {

    public Consumer<String> onDefocus;
    public List<FormattedCharSequence> tooltipLines;
    public boolean isPositiveNumber;
    public CommandSuggestions commandSuggestions;

    private MyEditBox(Builder builder) {
        super(Minecraft.getInstance().font, builder.x, builder.y, builder.width, builder.height, Component.literal(""));
        this.onDefocus = builder.onDefocus;
        this.tooltipLines = builder.tooltipLines;
        this.isPositiveNumber = builder.isNumber;

        setBordered(builder.bordered);
        setMaxLength(builder.maxLength);

        if (builder.isNumber)
            setFilter(value -> value.matches("\\d*"));
        if (builder.filter != null)
            setFilter(builder.filter);
        if (builder.value != null)
            setValue(builder.value);
        if (builder.responder != null)
            setResponder(builder.responder);

        /*
        if (builder.commandSuggestions) {
            DummyScreen dummy = new DummyScreen();
            dummy.init(Minecraft.getInstance(),
                    Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                    Minecraft.getInstance().getWindow().getGuiScaledHeight());
            commandSuggestions = new CommandSuggestions(
                    Minecraft.getInstance(),
                    dummy,
                    this,
                    Minecraft.getInstance().font,
                    true,   // isClientSide
                    false, // pOnlyShowIfCursorPastError
                    0,      // commandStart - 0 means the whole string is the command
                    10,     // maxSuggestions
                    true,   // pAnchorToBottom
                    -805306368 // background colour (vanilla default)
            );
            commandSuggestions.updateCommandInfo();
        } */
    }

    public int getIntValue(int fallback) {
        try {
            return Integer.parseInt(getValue());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static class Builder {
        // required
        private final int x, y, width, height;

        // optional
        private Consumer<String> onDefocus = null;
        private List<FormattedCharSequence> tooltipLines = null;
        private boolean isNumber = false;
        private boolean bordered = true;
        private int maxLength = 32;
        private java.util.function.Predicate<String> filter = null;
        private Consumer<String> responder = null;
        private String value = null;
        private boolean commandSuggestions = false;

        public Builder(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Builder onDefocus(Consumer<String> onDefocus) { this.onDefocus = onDefocus; return this; }
        public Builder tooltipLines(List<FormattedCharSequence> tooltipLines) { this.tooltipLines = tooltipLines; return this; }
        public Builder isNumber(boolean isNumber) { this.isNumber = isNumber; return this; }
        public Builder bordered(boolean bordered) { this.bordered = bordered; return this; }
        public Builder maxLength(int maxLength) { this.maxLength = maxLength; return this; }
        public Builder responder(Consumer<String> responder) { this.responder = responder; return this; }
        public Builder value(String value) { this.value = value; return this; }
        public Builder commandSuggestions(boolean commandSuggestions) {
            this.commandSuggestions = commandSuggestions;
            return this;
        }

        public MyEditBox build() {
            return new MyEditBox(this);
        }
    }
}