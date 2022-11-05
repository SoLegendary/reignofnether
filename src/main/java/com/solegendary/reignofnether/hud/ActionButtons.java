package com.solegendary.reignofnether.hud;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.registrars.PacketHandler;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.UnitServerboundPacket;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

// static list of generic unit actions (attack, move, stop, etc.)
public class ActionButtons {

    public static final Button ATTACK = new Button(
        "Attack",
        Button.itemIconSize,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sword.png"),
        Keybinding.attack,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.ATTACK,
        () -> false,
        () -> true,
        () -> CursorClientEvents.setLeftClickAction(UnitAction.ATTACK),
        List.of(FormattedCharSequence.forward("Attack", Style.EMPTY))

    );
    public static final Button STOP = new Button(
        "Stop",
        Button.itemIconSize,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
        Keybinding.stop,
        () -> false, // except if currently clicked on
        () -> false,
        () -> true,
        () -> PacketHandler.INSTANCE.sendToServer(new UnitServerboundPacket(
            UnitAction.STOP,
            -1,
            UnitClientEvents.getSelectedUnitIds().stream().mapToInt(i -> i).toArray(),
            CursorClientEvents.getPreselectedBlockPos()
        )),
        List.of(FormattedCharSequence.forward("Stop", Style.EMPTY))
    );
    public static final Button HOLD = new Button(
        "Hold Position",
        Button.itemIconSize,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/chestplate.png"),
        Keybinding.hold,
        () -> false, // TODO: if all selected units are holding position (but would need to get this from server?)
        () -> false,
        () -> true,
        () -> PacketHandler.INSTANCE.sendToServer(new UnitServerboundPacket(
            UnitAction.HOLD,
            -1,
            UnitClientEvents.getSelectedUnitIds().stream().mapToInt(i -> i).toArray(),
            CursorClientEvents.getPreselectedBlockPos()
        )),
        List.of(FormattedCharSequence.forward("Hold Position", Style.EMPTY))
    );
    public static final Button MOVE = new Button(
        "Move",
        Button.itemIconSize,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/boots.png"),
        Keybinding.move,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.MOVE,
        () -> false,
        () -> true,
        () -> CursorClientEvents.setLeftClickAction(UnitAction.MOVE),
        List.of(FormattedCharSequence.forward("Move", Style.EMPTY))
    );
}
