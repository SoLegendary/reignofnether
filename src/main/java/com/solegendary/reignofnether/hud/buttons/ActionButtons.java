package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;// I18n

import java.util.List;

import static com.solegendary.reignofnether.unit.UnitClientEvents.sendUnitCommand;

// static list of generic unit actions (build, attack, move, stop, etc.)
public class ActionButtons {

    public static final Button BUILD_REPAIR = new Button(
            Component.translatable("button.build_repair").getString(),
            Button.itemIconSize,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/shovel.png"),
            Keybindings.build,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.BUILD_REPAIR,
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.BUILD_REPAIR),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.build_repair").getString(),
                    Style.EMPTY)));
            
    public static final Button GATHER = new Button(
            Component.translatable("button.gather").getString(),
            Button.itemIconSize,
            null,
            Keybindings.gather,
            () -> UnitClientEvents.getSelectedUnitResourceTarget() != ResourceName.NONE,
            () -> false,
            () -> true,
            () -> sendUnitCommand(UnitAction.TOGGLE_GATHER_TARGET), null,
            null);

    public static final Button ATTACK = new Button(
            Component.translatable("button.attack").getString(),
            Button.itemIconSize,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/sword.png"),
            Keybindings.attack,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.ATTACK,
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.ATTACK),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.attack").getString(), Style.EMPTY)));

    public static final Button STOP = new Button(
            Component.translatable("button.stop").getString(),
            Button.itemIconSize,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/barrier.png"),
            Keybindings.stop,
            () -> false,
            () -> false,
            () -> true,
            () -> sendUnitCommand(UnitAction.STOP),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.stop").getString(), Style.EMPTY)));

    public static final Button HOLD = new Button(
            Component.translatable("button.hold_position").getString(),
            Button.itemIconSize,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/chestplate.png"),
            Keybindings.hold,
            () -> {
                LivingEntity entity = HudClientEvents.hudSelectedEntity;
                return entity instanceof Unit unit && unit.getHoldPosition();
            },
            () -> false,
            () -> true,
            () -> sendUnitCommand(UnitAction.HOLD),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.hold_position").getString(), Style.EMPTY)));

    public static final Button MOVE = new Button(
            Component.translatable("button.move").getString(),
            Button.itemIconSize,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/boots.png"),
            Keybindings.move,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.MOVE,
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.MOVE),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.move").getString(), Style.EMPTY)));

    public static final Button GARRISON = new Button(
            Component.translatable("button.garrison").getString(),
            Button.itemIconSize,
            new ResourceLocation("minecraft", "textures/block/ladder.png"),
            Keybindings.garrison,
            () -> CursorClientEvents.getLeftClickAction() == UnitAction.GARRISON,
            () -> false,
            () -> true,
            () -> CursorClientEvents.setLeftClickAction(UnitAction.GARRISON),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.garrison").getString(), Style.EMPTY)));

    public static final Button UNGARRISON = new Button(
            Component.translatable("button.ungarrison").getString(),
            Button.itemIconSize,
            new ResourceLocation("minecraft", "textures/block/oak_trapdoor.png"),
            Keybindings.garrison,
            () -> false,
            () -> false,
            () -> true,
            () -> sendUnitCommand(UnitAction.UNGARRISON),
            null,
            List.of(FormattedCharSequence.forward(Component.translatable("button.ungarrison").getString(), Style.EMPTY)));
}
