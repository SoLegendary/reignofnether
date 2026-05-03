package com.solegendary.reignofnether.hud;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DummyScreen extends Screen {
    public DummyScreen() {
        super(Component.literal(""));
    }
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {}
}