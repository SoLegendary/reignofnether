package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class IntegerButton extends Button {
    private final String label;
    private static Minecraft MC = Minecraft.getInstance();

    public IntegerButton(String label, Runnable onLeftClick, Runnable onRightClick, List<FormattedCharSequence> tooltips) {
        super(
                "Integer Button",
                10,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/blocks/command_block_back.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                onLeftClick,
                onRightClick,
                tooltips
        );
        this.label = label;
    }

    public IntegerButton(String label, Runnable onLeftClick, Runnable onRightClick, String tooltip) {
        this(label, onLeftClick, onRightClick, tooltip != null ? List.of(fcs(tooltip)) : null);
    }
    public IntegerButton(String label, Runnable onLeftClick, Runnable onRightClick) {
        this(label, onLeftClick, onRightClick, List.of());
    }
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        super.render(guiGraphics, x, y, mouseX, mouseY);
        if (!label.isBlank())
            guiGraphics.drawString(MC.font, label, x + 23, y + 7, 0xFFFFFF);
    }
    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (tooltipLines != null)
            MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY - 10);
    }
}