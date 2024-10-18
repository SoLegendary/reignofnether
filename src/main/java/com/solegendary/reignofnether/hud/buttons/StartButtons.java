package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;// I18n

import java.util.List;

public class StartButtons {

    public static final int ICON_SIZE = 14;

    public static Button villagerStartButton = new Button(
            Component.translatable("button.villagers").getString(),
            ICON_SIZE,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_VILLAGERS,
        () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.PLACE_WORKERS_B),
        () -> true,
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_VILLAGERS),
        () -> { },
        List.of(
                    FormattedCharSequence.forward(Component.translatable("button.villagers.info1").getString(),Style.EMPTY),
                    FormattedCharSequence.forward(Component.translatable("button.villagers.info2").getString(),Style.EMPTY)
        )
    );

    public static Button monsterStartButton = new Button(
            Component.translatable("button.monsters").getString(),
            ICON_SIZE,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_MONSTERS,
        TutorialClientEvents::isEnabled,
        () -> !TutorialClientEvents.isEnabled(),
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_MONSTERS),
        () -> { },
        List.of(FormattedCharSequence.forward(Component.translatable("button.monsters.info1").getString(),Style.EMPTY),
                    FormattedCharSequence.forward(Component.translatable("button.monsters.info2").getString(),Style.EMPTY)
        )
    );

    public static Button piglinStartButton = new Button(
            Component.translatable("button.piglins").getString(),
            ICON_SIZE,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_PIGLINS,
        TutorialClientEvents::isEnabled,
        () -> !TutorialClientEvents.isEnabled(),
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_PIGLINS),
        () -> { },
        List.of(
                FormattedCharSequence.forward(Component.translatable("button.piglins.info1").getString(),Style.EMPTY),
                FormattedCharSequence.forward(Component.translatable("button.piglins.info2").getString(),Style.EMPTY)
        )
    );
}
