package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.hud.layout.HudGeometry;
import com.solegendary.reignofnether.hud.playerdisplay.DiplomacyPlayerDisplay;
import com.solegendary.reignofnether.ui.CodeEditor;
import com.solegendary.reignofnether.ui.Theme;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
        if (DiplomacyPlayerDisplay.isTyping())
            return true;
        if (CodeEditor.focusedEditor() != null)
            return true;
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
            if (input.hudUnits) {
                input.setVisible(input.placed);
                if (!input.placed && input.isFocused()) {
                    input.setFocused(false);
                    handleDefocus(input);
                }
                input.placed = false;
            }
            float k = scale(input);
            var g = evt.getGuiGraphics();
            g.pose().pushPose();
            g.pose().scale(k, k, 1);
            input.render(g, (int) (evt.getMouseX() / k), (int) (evt.getMouseY() / k), 0f);
            if (input.isHovered())
                g.fill(input.getX() - 1, input.getY() - 1,
                        input.getX() + input.getWidth() + 1, input.getY() + input.getHeight() + 1,
                        Theme.HOVER);
            g.pose().popPose();
            if (input.isHovered())
                MyRenderer.renderTooltip(g, input.tooltipLines, evt.getMouseX(), evt.getMouseY());
            if (input.isFocused() && input.commandSuggestions != null) {
                input.commandSuggestions.updateCommandInfo();
                g.pose().pushPose();
                g.pose().scale(k, k, 1); // placed from the box, so in the box's units too
                input.commandSuggestions.render(g, (int) (evt.getMouseX() / k), (int) (evt.getMouseY() / k));
                g.pose().popPose();
            }
        }
    }

    // code editors (ui/CodeEditor) take their input before the screen and every other handler: while one is focused
    // keys and typing are all its own (Escape unfocuses it rather than leaving the screen), and the wheel over one scrolls it
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCodeKeyPressed(ScreenEvent.KeyPressed.Pre evt) {
        if (CodeEditor.keyPressed(evt.getKeyCode()))
            evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCodeCharTyped(ScreenEvent.CharacterTyped.Pre evt) {
        if (CodeEditor.charTyped(evt.getCodePoint()))
            evt.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCodeMouseClick(ScreenEvent.MouseButtonPressed.Pre evt) {
        if (!CodeEditor.mouseClicked(evt.getMouseX(), evt.getMouseY(), evt.getButton()))
            return;
        evt.setCanceled(true);
        for (MyEditBox input : registeredInputs)
            if (input.isFocused()) {
                input.setFocused(false);
                handleDefocus(input);
            }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onCodeMouseScroll(ScreenEvent.MouseScrolled.Pre evt) {
        if (CodeEditor.mouseScrolled(evt.getMouseX(), evt.getMouseY(), evt.getScrollDelta()))
            evt.setCanceled(true);
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
            float k = scale(input);
            input.mouseClicked(evt.getMouseX() / k, evt.getMouseY() / k, evt.getButton());
            boolean isMouseOver = input.isMouseOver(evt.getMouseX() / k, evt.getMouseY() / k);
            boolean wasFocused = input.isFocused();
            input.setFocused(isMouseOver);
            if (wasFocused && !input.isFocused())
                handleDefocus(input);
        }
    }

    // GUI pixels per box unit: 1 for plain boxes, the HUD scale for boxes laid out in HUD units
    private static float scale(MyEditBox input) {
        return input.hudUnits ? HudGeometry.scale().poseScale() : 1f;
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