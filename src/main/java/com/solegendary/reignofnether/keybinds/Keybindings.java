package com.solegendary.reignofnether.keybinds;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Keybindings {

    public static final String CATEGORY_CAMERA = "key.categories.reignofnether.camera";
    public static final String CATEGORY_RTS    = "key.categories.reignofnether.rts";

    // Raw GLFW bindings (not rebindable, not surfaced in vanilla Controls UI).
    // - pause: ESC must stay raw or it fights vanilla's pause screen
    // - shiftMod/ctrlMod/altMod: held-state qualifiers, not action keys
    public static final Keybinding pause    = new Keybinding(GLFW.GLFW_KEY_ESCAPE,        "ESC", "Pause");
    public static final Keybinding shiftMod = new Keybinding(GLFW.GLFW_KEY_LEFT_SHIFT,    "",    "Shift");
    public static final Keybinding ctrlMod  = new Keybinding(GLFW.GLFW_KEY_LEFT_CONTROL,  "LC",  "Ctrl");
    public static final Keybinding altMod   = new Keybinding(GLFW.GLFW_KEY_LEFT_ALT,      "ALT", "Alt");

    // Camera category
    public static final Keybinding zoomIn    = new Keybinding("key.reignofnether.zoom_in",     GLFW.GLFW_KEY_KP_ADD,      CATEGORY_CAMERA, "+");
    public static final Keybinding zoomOut   = new Keybinding("key.reignofnether.zoom_out",    GLFW.GLFW_KEY_KP_SUBTRACT, CATEGORY_CAMERA, "-");
    public static final Keybinding panPlusX  = new Keybinding("key.reignofnether.pan_plus_x",  GLFW.GLFW_KEY_LEFT,        CATEGORY_CAMERA, RtsConflictContext.PAN,    "X+");
    public static final Keybinding panMinusX = new Keybinding("key.reignofnether.pan_minus_x", GLFW.GLFW_KEY_RIGHT,       CATEGORY_CAMERA, RtsConflictContext.PAN,    "X-");
    public static final Keybinding panPlusZ  = new Keybinding("key.reignofnether.pan_plus_z",  GLFW.GLFW_KEY_UP,          CATEGORY_CAMERA, RtsConflictContext.PAN,    "Z+");
    public static final Keybinding panMinusZ = new Keybinding("key.reignofnether.pan_minus_z", GLFW.GLFW_KEY_DOWN,        CATEGORY_CAMERA, RtsConflictContext.PAN,    "Z-");
    public static final Keybinding rotCW     = new Keybinding("key.reignofnether.rot_cw",      GLFW.GLFW_KEY_RIGHT,       CATEGORY_CAMERA, RtsConflictContext.ROTATE, "");
    public static final Keybinding rotCCW    = new Keybinding("key.reignofnether.rot_ccw",     GLFW.GLFW_KEY_LEFT,        CATEGORY_CAMERA, RtsConflictContext.ROTATE, "");
    public static final Keybinding reset     = new Keybinding("key.reignofnether.reset",       GLFW.GLFW_KEY_RIGHT_CONTROL, CATEGORY_CAMERA, "RC");

    // RTS category
    public static final Keybinding cancelBuild = new Keybinding("key.reignofnether.cancel_build", GLFW.GLFW_KEY_DELETE,        CATEGORY_RTS, "DEL");
    public static final Keybinding abilitySlot1        = new Keybinding("key.reignofnether.ability_1",    GLFW.GLFW_KEY_Q,             CATEGORY_RTS, "Q");
    public static final Keybinding abilitySlot2        = new Keybinding("key.reignofnether.ability_2",    GLFW.GLFW_KEY_W,             CATEGORY_RTS, "W");
    public static final Keybinding abilitySlot3        = new Keybinding("key.reignofnether.ability_3",    GLFW.GLFW_KEY_E,             CATEGORY_RTS, "E");
    public static final Keybinding abilitySlot4        = new Keybinding("key.reignofnether.ability_4",    GLFW.GLFW_KEY_R,             CATEGORY_RTS, "R");
    public static final Keybinding abilitySlot5        = new Keybinding("key.reignofnether.ability_5",    GLFW.GLFW_KEY_T,             CATEGORY_RTS, "T");
    public static final Keybinding abilitySlot6        = new Keybinding("key.reignofnether.ability_6",    GLFW.GLFW_KEY_Y,             CATEGORY_RTS, "Y");
    public static final Keybinding abilitySlot7        = new Keybinding("key.reignofnether.ability_7",    GLFW.GLFW_KEY_U,             CATEGORY_RTS, "U");
    public static final Keybinding abilitySlot8        = new Keybinding("key.reignofnether.ability_8",    GLFW.GLFW_KEY_I,             CATEGORY_RTS, "I");
    public static final Keybinding abilitySlot9        = new Keybinding("key.reignofnether.ability_9",    GLFW.GLFW_KEY_O,             CATEGORY_RTS, "O");
    public static final Keybinding abilitySlot10       = new Keybinding("key.reignofnether.ability_10",   GLFW.GLFW_KEY_P,             CATEGORY_RTS, "P");
    public static final Keybinding hotkey1        = new Keybinding("key.reignofnether.hotkey_1",        GLFW.GLFW_KEY_V,             CATEGORY_RTS, "V");
    public static final Keybinding hotkey2        = new Keybinding("key.reignofnether.hotkey_2",        GLFW.GLFW_KEY_L,             CATEGORY_RTS, "L");
    public static final Keybinding minimapToggle  = new Keybinding("key.reignofnether.minimap_toggle",        GLFW.GLFW_KEY_M,             CATEGORY_RTS, "M");
    public static final Keybinding hotkey3        = new Keybinding("key.reignofnether.hotkey_3",        GLFW.GLFW_KEY_F,             CATEGORY_RTS, "F");
    public static final Keybinding hotkey4        = new Keybinding("key.reignofnether.hotkey_4",        GLFW.GLFW_KEY_C,             CATEGORY_RTS, "C");
    public static final Keybinding hotkey5        = new Keybinding("key.reignofnether.hotkey_5",        GLFW.GLFW_KEY_D,             CATEGORY_RTS, "D");
    public static final Keybinding hotkey6        = new Keybinding("key.reignofnether.hotkey_6",        GLFW.GLFW_KEY_J,             CATEGORY_RTS, "J");
    public static final Keybinding hotkey7        = new Keybinding("key.reignofnether.hotkey_7",        GLFW.GLFW_KEY_K,             CATEGORY_RTS, "K");
    public static final Keybinding hotkey8        = new Keybinding("key.reignofnether.hotkey_8",        GLFW.GLFW_KEY_Z,             CATEGORY_RTS, "Z");
    public static final Keybinding hotkey9        = new Keybinding("key.reignofnether.hotkey_9",        GLFW.GLFW_KEY_X,             CATEGORY_RTS, "X");
    public static final Keybinding attack      = new Keybinding("key.reignofnether.attack",       GLFW.GLFW_KEY_A,             CATEGORY_RTS, "A");
    public static final Keybinding stop        = new Keybinding("key.reignofnether.stop",         GLFW.GLFW_KEY_S,             CATEGORY_RTS, "S");
    public static final Keybinding hold        = new Keybinding("key.reignofnether.hold",         GLFW.GLFW_KEY_H,             CATEGORY_RTS, "H");
    public static final Keybinding build       = new Keybinding("key.reignofnether.build",        GLFW.GLFW_KEY_B,             CATEGORY_RTS, "B");
    public static final Keybinding gather      = new Keybinding("key.reignofnether.gather",       GLFW.GLFW_KEY_G,             CATEGORY_RTS, "G");
    public static final Keybinding garrison    = new Keybinding("key.reignofnether.garrison",     GLFW.GLFW_KEY_N,             CATEGORY_RTS, "N");
    public static final Keybinding chat        = new Keybinding("key.reignofnether.chat",         GLFW.GLFW_KEY_ENTER,         CATEGORY_RTS, "");
    public static final Keybinding deselect    = new Keybinding("key.reignofnether.deselect",     GLFW.GLFW_KEY_GRAVE_ACCENT,  CATEGORY_RTS, "~");
    public static final Keybinding tab         = new Keybinding("key.reignofnether.tab",          GLFW.GLFW_KEY_TAB,           CATEGORY_RTS, "");

    public static final Keybinding[] nums = {
        new Keybinding("key.reignofnether.control_group_0", GLFW.GLFW_KEY_0, CATEGORY_RTS, "0"),
        new Keybinding("key.reignofnether.control_group_1", GLFW.GLFW_KEY_1, CATEGORY_RTS, "1"),
        new Keybinding("key.reignofnether.control_group_2", GLFW.GLFW_KEY_2, CATEGORY_RTS, "2"),
        new Keybinding("key.reignofnether.control_group_3", GLFW.GLFW_KEY_3, CATEGORY_RTS, "3"),
        new Keybinding("key.reignofnether.control_group_4", GLFW.GLFW_KEY_4, CATEGORY_RTS, "4"),
        new Keybinding("key.reignofnether.control_group_5", GLFW.GLFW_KEY_5, CATEGORY_RTS, "5"),
        new Keybinding("key.reignofnether.control_group_6", GLFW.GLFW_KEY_6, CATEGORY_RTS, "6"),
        new Keybinding("key.reignofnether.control_group_7", GLFW.GLFW_KEY_7, CATEGORY_RTS, "7"),
        new Keybinding("key.reignofnether.control_group_8", GLFW.GLFW_KEY_8, CATEGORY_RTS, "8"),
        new Keybinding("key.reignofnether.control_group_9", GLFW.GLFW_KEY_9, CATEGORY_RTS, "9")
    };
    public static final Keybinding[] fnums = {
        new Keybinding("key.reignofnether.fn_1",  GLFW.GLFW_KEY_F1,  CATEGORY_RTS, "F1"),
        new Keybinding("key.reignofnether.fn_2",  GLFW.GLFW_KEY_F2,  CATEGORY_RTS, "F2"),
        new Keybinding("key.reignofnether.fn_3",  GLFW.GLFW_KEY_F3,  CATEGORY_RTS, "F3"),
        new Keybinding("key.reignofnether.fn_4",  GLFW.GLFW_KEY_F4,  CATEGORY_RTS, "F4"),
        new Keybinding("key.reignofnether.fn_5",  GLFW.GLFW_KEY_F5,  CATEGORY_RTS, "F5"),
        new Keybinding("key.reignofnether.fn_6",  GLFW.GLFW_KEY_F6,  CATEGORY_RTS, "F6"),
        new Keybinding("key.reignofnether.fn_7",  GLFW.GLFW_KEY_F7,  CATEGORY_RTS, "F7"),
        new Keybinding("key.reignofnether.fn_8",  GLFW.GLFW_KEY_F8,  CATEGORY_RTS, "F8"),
        new Keybinding("key.reignofnether.fn_9",  GLFW.GLFW_KEY_F9,  CATEGORY_RTS, "F9"),
        new Keybinding("key.reignofnether.fn_10", GLFW.GLFW_KEY_F10, CATEGORY_RTS, "F10"),
        new Keybinding("key.reignofnether.fn_11", GLFW.GLFW_KEY_F11, CATEGORY_RTS, "F11"),
        new Keybinding("key.reignofnether.fn_12", GLFW.GLFW_KEY_F12, CATEGORY_RTS, "F12")
    };

    private static final List<Keybinding> ALL;
    static {
        List<Keybinding> list = new ArrayList<>();
        list.add(pause);
        list.add(shiftMod); list.add(ctrlMod); list.add(altMod);
        list.add(zoomIn); list.add(zoomOut);
        list.add(panPlusX); list.add(panMinusX); list.add(panPlusZ); list.add(panMinusZ);
        list.add(rotCW); list.add(rotCCW); list.add(reset);
        list.add(cancelBuild);
        list.add(abilitySlot1); list.add(abilitySlot2); list.add(abilitySlot3); list.add(abilitySlot4); list.add(abilitySlot5);
        list.add(abilitySlot6); list.add(abilitySlot7); list.add(abilitySlot8); list.add(abilitySlot9); list.add(abilitySlot10);
        list.add(hotkey1); list.add(hotkey2); list.add(minimapToggle); list.add(hotkey3); list.add(hotkey4);
        list.add(hotkey5); list.add(hotkey6); list.add(hotkey7);
        list.add(attack); list.add(stop); list.add(hold);
        list.add(build); list.add(gather); list.add(garrison);
        list.add(chat); list.add(deselect); list.add(tab);
        list.add(hotkey8); list.add(hotkey9);
        Collections.addAll(list, nums);
        Collections.addAll(list, fnums);
        ALL = Collections.unmodifiableList(list);
    }

    public static List<Keybinding> all() {
        return ALL;
    }

    public static Keybinding getNum(int i) {
        return nums[i];
    }
    public static Keybinding getFnum(int i) {
        return fnums[i-1];
    }
}
