package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.registrars.Keybinds;
import com.solegendary.reignofnether.units.UnitClientEvents;
import net.minecraft.client.Minecraft;

// static list of generic unit actions (attack, move, stop, etc.)
public class ActionButtons {

    private static final int itemIconSize = 14;
    private static final int iconFrameSize = Button.iconFrameSize;

    public static final Button attack = new Button(
            "Attack",
            itemIconSize,
            "textures/icons/items/sword.png",
            Keybinds.keyA,
            () -> CursorClientEvents.getLeftClickAction() == ActionName.ATTACK,
            () -> CursorClientEvents.setLeftClickAction(ActionName.ATTACK)
    );
    public static final Button stop = new Button(
            "Stop",
            itemIconSize,
            "textures/icons/items/barrier.png",
            Keybinds.keyS,
            () -> false,
            UnitClientEvents::sendStopCommand
    );
    public static final Button hold = new Button(
            "Hold Position",
            itemIconSize,
            "textures/icons/items/chestplate.png",
            Keybinds.keyH,
            () -> false,
            () -> { } // TODO: implement
    );
    public static final Button move = new Button(
            "Move",
            itemIconSize,
            "textures/icons/items/boots.png",
            Keybinds.keyM,
            () -> CursorClientEvents.getLeftClickAction() == ActionName.MOVE,
            () -> CursorClientEvents.setLeftClickAction(ActionName.MOVE)
    );
}
