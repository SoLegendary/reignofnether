package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.units.UnitClientEvents;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.Minecraft;

// static list of generic unit actions (attack, move, stop, etc.)
public class ActionButtons {

    private static final int itemIconSize = 14;
    private static final int iconFrameSize = 22;
    private static final int iconFrameSelectedSize = 24;

    private static final Minecraft MC = Minecraft.getInstance();

    public static final Button attack = new Button(
            0,0,
            itemIconSize,
            iconFrameSize,
            iconFrameSelectedSize,
            "textures/icons/items/sword.png",
            "textures/hud/icon_frame.png",
            "textures/hud/icon_frame_selected.png",
            Keybinds.keyA,
            CursorClientEvents::getAttackFlag,
            () -> {
                if (UnitClientEvents.getSelectedUnitIds().size() > 0)
                    CursorClientEvents.setAttackFlag(true);
            }
    );
    public static final Button stop = new Button(
            0,0,
            itemIconSize,
            iconFrameSize,
            iconFrameSelectedSize,
            "textures/icons/items/barrier.png",
            "textures/hud/icon_frame.png",
            "textures/hud/icon_frame_selected.png",
            Keybinds.keyS,
            () -> false,
            UnitClientEvents::sendStopCommand
    );
    public static final Button hold = new Button(
            0,0,
            itemIconSize,
            iconFrameSize,
            iconFrameSelectedSize,
            "textures/icons/items/chestplate.png",
            "textures/hud/icon_frame.png",
            "textures/hud/icon_frame_selected.png",
            Keybinds.keyH,
            () -> false,
            () -> { }
    );
    public static final Button move = new Button(
            0,0,
            itemIconSize,
            iconFrameSize,
            iconFrameSelectedSize,
            "textures/icons/items/boots.png",
            "textures/hud/icon_frame.png",
            "textures/hud/icon_frame_selected.png",
            Keybinds.keyM,
            () -> false,
            () -> { }
    );
}
