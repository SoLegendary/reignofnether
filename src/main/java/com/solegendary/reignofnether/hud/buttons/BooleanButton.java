package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class BooleanButton extends Button {
    private final String label;
    private static Minecraft MC = Minecraft.getInstance();

    public BooleanButton(String label, boolean enabled, Runnable onLeftClick, String tooltip) {
        super(
                "Boolean Button",
                10,
                enabled ? ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png") :
                        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> true,
                onLeftClick,
                null,
                tooltip != null ? List.of(fcs(tooltip)) : null
        );
        this.label = label;
        this.frameResource = null;
    }
    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        super.render(guiGraphics, x, y, mouseX, mouseY);
        if (!label.isBlank())
            guiGraphics.drawString(MC.font, label,x + 23, y + 7, 0xFFFFFF);
    }
    @Override
    public void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (tooltipLines != null)
            MyRenderer.renderTooltip(guiGraphics, tooltipLines, mouseX, mouseY + tooltipOffsetY - 10);
    }
}