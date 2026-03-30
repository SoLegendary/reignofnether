package com.solegendary.reignofnether.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TextInputClientEvents {

    private static final List<EditBox> registeredInputs = new ArrayList<>();
    private static final Map<EditBox, Consumer<String>> defocusResponders = new HashMap<>();

    public static EditBox register(int x, int y, int width, int height) {
        return register(x, y, width, height, null);
    }

    public static EditBox register(int x, int y, int width, int height, Consumer<String> onDefocus) {
        EditBox input = new EditBox(Minecraft.getInstance().font, x, y, width, height, Component.literal(""));
        registeredInputs.add(input);
        if (onDefocus != null)
            defocusResponders.put(input, onDefocus);
        return input;
    }

    public static void unregister(EditBox input) {
        if (defocusResponders.containsKey(input))
            defocusResponders.get(input).accept(input.getValue());
        registeredInputs.remove(input);
        defocusResponders.remove(input);
    }

    public static boolean isAnyInputFocused() {
        for (EditBox input : registeredInputs)
            if (input.isFocused())
                return true;
        return false;
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        for (EditBox input : registeredInputs) {
            if (input.isHovered()) {
                evt.getGuiGraphics().fill(
                        input.getX() - 1, input.getY() - 1,
                        input.getX() + input.getWidth() + 1, input.getY() + input.getHeight() + 1,
                        0x30FFFFFF
                );
            }
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post evt) {
        for (EditBox input : registeredInputs) {
            input.keyPressed(evt.getKeyCode(), evt.getScanCode(), evt.getModifiers());
            if (evt.getKeyCode() == GLFW.GLFW_KEY_ENTER)
                input.setFocused(false);
        }
    }

    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Post evt) {
        for (EditBox input : registeredInputs)
            if (input.isFocused())
                input.charTyped(evt.getCodePoint(), evt.getModifiers());
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        for (EditBox input : registeredInputs) {
            input.mouseClicked(evt.getMouseX(), evt.getMouseY(), evt.getButton());
            boolean isMouseOver = input.isMouseOver(evt.getMouseX(), evt.getMouseY());
            boolean wasFocused = input.isFocused();
            input.setFocused(isMouseOver);
            if (wasFocused && !input.isFocused()) {
                Consumer<String> responder = defocusResponders.get(input);
                if (responder != null)
                    responder.accept(input.getValue());
            }
        }
    }
}