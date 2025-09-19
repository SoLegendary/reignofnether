package com.solegendary.reignofnether.config;

import com.solegendary.reignofnether.config.elements.ConfigCheckbox;
import com.solegendary.reignofnether.config.elements.ConfigColorButton;
import com.solegendary.reignofnether.config.elements.Label;
import com.solegendary.reignofnether.player.PlayerColors;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

public class ReignOfNetherConfigScreen extends Screen {

    protected static final Minecraft MC = Minecraft.getInstance();
    protected Screen parent;

    public ReignOfNetherConfigScreen(Screen parent) {
        super(Component.literal("Reign of Nether"));
        this.parent = parent;
        //this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.buildColorSettings(50);
        this.addRenderableWidget(button(this.width / 2 - 75, this.height - 29, 150, 20, CommonComponents.GUI_BACK, button -> this.minecraft.setScreen(this.parent)));
    }

    public static Button button(int x, int y, int width, int height, Component label, Button.OnPress onPress) {
        return Button.builder(label, onPress).pos(x, y).size(width, height).build();
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderDirtBackground(pGuiGraphics);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }


    private void buildColorSettings(int y) {
        int totalColors = 4;
        int widthColors = 200;
        int labelHeight = 20;
        int colorHeight = 30;
        var colorRect = new ScreenRectangle(this.width / 2 - widthColors / 2, y + 20, widthColors, colorHeight);

        // area header
        addRenderableOnly(new Label("Alliance colors").pos(colorRect.left(), colorRect.top() - 20).size(widthColors, labelHeight));

        // for color cycle buttons, on the same row
        addRenderableWidget(new ConfigColorButton(ReignOfNetherClientConfigs.PLAYER_COLOR_SELF, "Self")
                .pos(0 * (colorRect.width() / totalColors) + colorRect.left(), colorRect.top())
                .size(colorRect.width() / totalColors, colorRect.height()));
        addRenderableWidget(new ConfigColorButton(ReignOfNetherClientConfigs.PLAYER_COLOR_ALLY, "Ally")
                .pos(1 * (colorRect.width() / totalColors) + colorRect.left(), colorRect.top())
                .size(colorRect.width() / totalColors, colorRect.height()));
        addRenderableWidget(new ConfigColorButton(ReignOfNetherClientConfigs.PLAYER_COLOR_NEUTRAL, "Neutral")
                .pos(2 * (colorRect.width() / totalColors) + colorRect.left(), colorRect.top())
                .size(colorRect.width() / totalColors, colorRect.height()));
        addRenderableWidget(new ConfigColorButton(ReignOfNetherClientConfigs.PLAYER_COLOR_ENEMY, "Enemy")
                .pos(3 * (colorRect.width() / totalColors) + colorRect.left(), colorRect.top())
                .size(colorRect.width() / totalColors, colorRect.height()));

        // checkbox to toggle between player color and alliance color
        addRenderableWidget(new ConfigCheckbox(ReignOfNetherClientConfigs.USE_PLAYER_COLORS, "Using player colors", "Using alliance colors")
                .pos(colorRect.left(), colorRect.bottom())
                .size(colorRect.width(), labelHeight));
    }
}
