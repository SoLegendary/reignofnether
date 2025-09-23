package com.solegendary.reignofnether.config.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

public class ConfigColorButton implements Renderable, GuiEventListener, NarratableEntry {

    protected static final Minecraft MC = Minecraft.getInstance();

    private final ForgeConfigSpec.ConfigValue<Integer> configValue;
    private final String label;
    private int x = 0;
    private int y = 0;
    private int w = 10;
    private int h = 10;
    private boolean focused = false;

    public ConfigColorButton(ForgeConfigSpec.ConfigValue<Integer> configValue, String label) {
        this.configValue = configValue;
        this.label = label;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        var hovering = isMouseOver(pMouseX, pMouseY);
        var texture = PlayerColors.getColorIcon(configValue.get());

        if (!hovering && PlayerColors.isUsingPlayerColors()) {
            RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.5f);
        }

        guiGraphics.blit(texture,
                x, y + h / 2, 0,
                0, 0, // where on texture to start drawing from
                w, h / 2, // dimensions of blit texture
                w, h / 2 // size of texture itself (if < dimensions, texture is repeated)
        );

        if (hovering) {
            MyRenderer.renderFrameWithBg(guiGraphics, x, y + h / 2, w, h / 2, 0x00000000);
            guiGraphics.drawCenteredString(MC.font, (configValue.get() + 1) + "/" + (PlayerColors.colors.length), this.x + this.w / 2, this.y + 3 * this.h / 4 - MC.font.lineHeight / 2, 0x99FFFFFF);
        }

        guiGraphics.drawCenteredString(MC.font, this.label, this.x + this.w / 2, this.y + this.h / 4 - MC.font.lineHeight / 2, 0xFFFFFFFF);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 2 || !isMouseOver(pMouseX, pMouseY)) {
            return false;
        }

        if (pButton == 0) {
            configValue.set((configValue.get() + 1) % PlayerColors.PLAYER_COLOR_TOTAL_COUNT);
        } else if (pButton == 1) {
            configValue.set((configValue.get() + PlayerColors.PLAYER_COLOR_TOTAL_COUNT - 1) % PlayerColors.PLAYER_COLOR_TOTAL_COUNT);
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return !(pMouseX < x || pMouseY < y || pMouseX > x + w || pMouseY > y + h);
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    @Override
    public @NotNull ScreenRectangle getRectangle() {
        return new ScreenRectangle(x, y, w, h);
    }

    @Override
    public @NotNull NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.HINT, Component.literal(this.label));
    }

    public ConfigColorButton pos(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public ConfigColorButton size(int w, int h) {
        this.w = w;
        this.h = h;
        return this;
    }
}
