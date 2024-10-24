package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.UnitAction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class StartButtons {

    public static final int ICON_SIZE = 14;

    public static Button villagerStartButton = new Button(
        "Villagers",
            ICON_SIZE,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_VILLAGERS,
        () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.PLACE_WORKERS_B) || !PlayerClientEvents.canStartRTS,
        () -> true,
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_VILLAGERS),
        () -> { },
        List.of(
            FormattedCharSequence.forward(I18n.get("hud.startbuttons.villagers.reignofnether.first"), Style.EMPTY),
            FormattedCharSequence.forward(I18n.get("hud.startbuttons.villagers.reignofnether.second"), Style.EMPTY)
        )
    );

    public static Button monsterStartButton = new Button(
        "Monsters",
            ICON_SIZE,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_MONSTERS,
        () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS,
        () -> !TutorialClientEvents.isEnabled(),
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_MONSTERS),
        () -> { },
        List.of(
            FormattedCharSequence.forward(I18n.get("hud.startbuttons.monsters.reignofnether.first"), Style.EMPTY),
            FormattedCharSequence.forward(I18n.get("hud.startbuttons.monsters.reignofnether.second"), Style.EMPTY)
        )
    );

    public static Button piglinStartButton = new Button(
        "Piglins",
            ICON_SIZE,
        new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_PIGLINS,
        () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS,
        () -> !TutorialClientEvents.isEnabled(),
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_PIGLINS),
        () -> { },
        List.of(
            FormattedCharSequence.forward(I18n.get("hud.startbuttons.piglins.reignofnether.first"), Style.EMPTY),
            FormattedCharSequence.forward(I18n.get("hud.startbuttons.piglins.reignofnether.second"), Style.EMPTY)
        )
    );
}
