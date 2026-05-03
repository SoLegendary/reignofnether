package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TextInputClientEvents {

    private static final List<MyEditBox> registeredInputs = new ArrayList<>();

    public static void registerEditBox(MyEditBox box) {
        if (box == null)
            return;
        registeredInputs.add(box);
    }

    public static void deregisterEditBox(MyEditBox input) {
        if (input == null)
            return;
        if (input.isFocused() && input.onDefocus != null)
            input.onDefocus.accept(input.getValue());
        registeredInputs.remove(input);
    }

    public static boolean isAnyInputFocused() {
        for (MyEditBox input : registeredInputs)
            if (input.isFocused())
                return true;
        return false;
    }

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (Minecraft.getInstance().isPaused())
            return;
        for (MyEditBox input : registeredInputs) {
            input.render(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY(), 0f);
            if (input.isHovered()) {
                evt.getGuiGraphics().fill(
                        input.getX() - 1, input.getY() - 1,
                        input.getX() + input.getWidth() + 1, input.getY() + input.getHeight() + 1,
                        0x30FFFFFF
                );
                MyRenderer.renderTooltip(evt.getGuiGraphics(), input.tooltipLines, evt.getMouseX(), evt.getMouseY());
            }
            if (input.isFocused() && input.commandSuggestions != null) {
                input.commandSuggestions.updateCommandInfo();
                input.commandSuggestions.render(evt.getGuiGraphics(), evt.getMouseX(), evt.getMouseY());
            }
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Post evt) {
        for (MyEditBox input : registeredInputs) {
            input.keyPressed(evt.getKeyCode(), evt.getScanCode(), evt.getModifiers());
            boolean wasFocused = input.isFocused();
            if (evt.getKeyCode() == GLFW.GLFW_KEY_ENTER)
                input.setFocused(false);
            if (wasFocused && !input.isFocused())
                handleDefocus(input);
        }
    }

    @SubscribeEvent
    public static void onCharTyped(ScreenEvent.CharacterTyped.Post evt) {
        for (MyEditBox input : registeredInputs) {
            if (input.isFocused())
                input.charTyped(evt.getCodePoint(), evt.getModifiers());
        }
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        for (MyEditBox input : registeredInputs) {
            input.mouseClicked(evt.getMouseX(), evt.getMouseY(), evt.getButton());
            boolean isMouseOver = input.isMouseOver(evt.getMouseX(), evt.getMouseY());
            boolean wasFocused = input.isFocused();
            input.setFocused(isMouseOver);
            if (wasFocused && !input.isFocused())
                handleDefocus(input);
        }
    }

    private static void handleDefocus(MyEditBox input) {
        if (input.onDefocus != null) {
            if (input.isPositiveNumber) {
                input.setValue(input.getValue().replaceFirst("^0+(?!$)", ""));
                try {
                    int intValue = Integer.parseInt(input.getValue());
                    if (intValue <= 0)
                        input.setValue("1");
                } catch (NumberFormatException e) {
                    input.setValue("1");
                }
            }
            input.onDefocus.accept(input.getValue());
        }
    }
}