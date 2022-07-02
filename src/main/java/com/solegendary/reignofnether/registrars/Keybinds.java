package com.solegendary.reignofnether.registrars;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class Keybinds {

    private static final String KEY_CATEGORY = "key.categories.reignofnether";

    public static final KeyMapping escape = new KeyMapping("key.reignofnether.orthoview.escape", GLFW.GLFW_KEY_ESCAPE, KEY_CATEGORY);
    public static final KeyMapping zoomIn = new KeyMapping("key.reignofnether.orthoview.zoomIn", GLFW.GLFW_KEY_KP_ADD, KEY_CATEGORY);
    public static final KeyMapping zoomOut = new KeyMapping("key.reignofnether.orthoview.zoomOut", GLFW.GLFW_KEY_KP_SUBTRACT, KEY_CATEGORY);
    public static final KeyMapping panPlusX = new KeyMapping("key.reignofnether.orthoview.panPlusZ", GLFW.GLFW_KEY_LEFT, KEY_CATEGORY);
    public static final KeyMapping panMinusX = new KeyMapping("key.reignofnether.orthoview.panMinusZ", GLFW.GLFW_KEY_RIGHT, KEY_CATEGORY);
    public static final KeyMapping panPlusZ = new KeyMapping("key.reignofnether.orthoview.panPlusX", GLFW.GLFW_KEY_UP, KEY_CATEGORY);
    public static final KeyMapping panMinusZ = new KeyMapping("key.reignofnether.orthoview.panMinusX", GLFW.GLFW_KEY_DOWN, KEY_CATEGORY);
    public static final KeyMapping reset = new KeyMapping("key.reignofnether.orthoview.reset", GLFW.GLFW_KEY_RIGHT_CONTROL, KEY_CATEGORY);
    public static final KeyMapping shiftMod = new KeyMapping("key.reignofnether.orthoview.shiftMod", GLFW.GLFW_KEY_LEFT_SHIFT, KEY_CATEGORY);
    public static final KeyMapping ctrlMod = new KeyMapping("key.reignofnether.orthoview.ctrlMod", GLFW.GLFW_KEY_LEFT_CONTROL, KEY_CATEGORY);
    public static final KeyMapping altMod = new KeyMapping("key.reignofnether.orthoview.altMod", GLFW.GLFW_KEY_LEFT_ALT, KEY_CATEGORY);
    public static final KeyMapping keyP = new KeyMapping("key.reignofnether.orthoview.keyP", GLFW.GLFW_KEY_P, KEY_CATEGORY);
    public static final KeyMapping keyO = new KeyMapping("key.reignofnether.orthoview.keyO", GLFW.GLFW_KEY_O, KEY_CATEGORY);
    public static final KeyMapping keyA = new KeyMapping("key.reignofnether.orthoview.keyA", GLFW.GLFW_KEY_A, KEY_CATEGORY);
    public static final KeyMapping keyS = new KeyMapping("key.reignofnether.orthoview.keyS", GLFW.GLFW_KEY_S, KEY_CATEGORY);
    public static final KeyMapping keyH = new KeyMapping("key.reignofnether.orthoview.keyH", GLFW.GLFW_KEY_H, KEY_CATEGORY);
    public static final KeyMapping keyM = new KeyMapping("key.reignofnether.orthoview.keyM", GLFW.GLFW_KEY_M, KEY_CATEGORY);
    public static final KeyMapping keyQ = new KeyMapping("key.reignofnether.orthoview.keyQ", GLFW.GLFW_KEY_Q, KEY_CATEGORY);
    public static final KeyMapping keyW = new KeyMapping("key.reignofnether.orthoview.keyW", GLFW.GLFW_KEY_W, KEY_CATEGORY);
    public static final KeyMapping keyE = new KeyMapping("key.reignofnether.orthoview.keyE", GLFW.GLFW_KEY_E, KEY_CATEGORY);
    public static final KeyMapping keyR = new KeyMapping("key.reignofnether.orthoview.keyR", GLFW.GLFW_KEY_R, KEY_CATEGORY);

    public static final KeyMapping[] nums = {
            new KeyMapping("key.reignofnether.orthoview.key0", GLFW.GLFW_KEY_0, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key1", GLFW.GLFW_KEY_1, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key2", GLFW.GLFW_KEY_2, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key3", GLFW.GLFW_KEY_3, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key4", GLFW.GLFW_KEY_4, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key5", GLFW.GLFW_KEY_5, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key6", GLFW.GLFW_KEY_6, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key7", GLFW.GLFW_KEY_7, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key8", GLFW.GLFW_KEY_8, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.key9", GLFW.GLFW_KEY_9, KEY_CATEGORY)
    };
    public static final KeyMapping[] fnums = {
            null,
            new KeyMapping("key.reignofnether.orthoview.keyF1", GLFW.GLFW_KEY_F1, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF2", GLFW.GLFW_KEY_F2, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF3", GLFW.GLFW_KEY_F3, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF4", GLFW.GLFW_KEY_F4, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF5", GLFW.GLFW_KEY_F5, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF6", GLFW.GLFW_KEY_F6, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF7", GLFW.GLFW_KEY_F7, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF8", GLFW.GLFW_KEY_F8, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF9", GLFW.GLFW_KEY_F9, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF10", GLFW.GLFW_KEY_F10, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF11", GLFW.GLFW_KEY_F11, KEY_CATEGORY),
            new KeyMapping("key.reignofnether.orthoview.keyF12", GLFW.GLFW_KEY_F12, KEY_CATEGORY)
    };


    public static void init() {
        ClientRegistry.registerKeyBinding(escape);
        ClientRegistry.registerKeyBinding(zoomIn);
        ClientRegistry.registerKeyBinding(zoomOut);
        ClientRegistry.registerKeyBinding(reset);
        ClientRegistry.registerKeyBinding(shiftMod);
        ClientRegistry.registerKeyBinding(ctrlMod);
        ClientRegistry.registerKeyBinding(altMod);
        ClientRegistry.registerKeyBinding(keyP);
        ClientRegistry.registerKeyBinding(keyO);
        ClientRegistry.registerKeyBinding(keyA);
        ClientRegistry.registerKeyBinding(keyS);
        ClientRegistry.registerKeyBinding(keyH);
        ClientRegistry.registerKeyBinding(keyM);
        ClientRegistry.registerKeyBinding(keyQ);
        ClientRegistry.registerKeyBinding(keyW);
        ClientRegistry.registerKeyBinding(keyE);
        ClientRegistry.registerKeyBinding(keyR);

        for (KeyMapping keyMapping : nums)
            ClientRegistry.registerKeyBinding(keyMapping);
        for (KeyMapping keyMapping : fnums)
            if (keyMapping != null)
                ClientRegistry.registerKeyBinding(keyMapping);
    }
}
