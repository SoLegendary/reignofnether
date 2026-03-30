package com.solegendary.reignofnether.hud.buttons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.LanguageUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class StartButtons {

    public static final int ICON_SIZE = 14;

    public static Button sandboxStartButton = new Button(
            "Sandbox",
            ICON_SIZE,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
            (Keybinding) null,
            () -> false,
            () -> false,
            () -> true,
            () -> PlayerServerboundPacket.startRTS(Faction.NONE, 0d,0d,0d),
            null,
            List.of(
                    fcs(LanguageUtil.getTranslation("hud.gamemode.reignofnether.sandbox_confirm"))
            )
    );
    public static Button villagerStartButton = new Button(
        "Villagers",
            ICON_SIZE,
        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_VILLAGERS,
        () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.PLACE_WORKERS_B) || !PlayerClientEvents.canStartRTS,
        () -> true,
        () -> {
            CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_VILLAGERS);
        },
        null,
        List.of(
            fcs(LanguageUtil.getTranslation("hud.startbuttons.villagers.reignofnether.first"), true),
            fcs(LanguageUtil.getTranslation("hud.startbuttons.villagers.reignofnether.second"))
        )
    );

    public static Button monsterStartButton = new Button(
        "Monsters",
            ICON_SIZE,
        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_MONSTERS,
        () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS,
        () -> !TutorialClientEvents.isEnabled(),
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_MONSTERS),
        null,
        List.of(
            fcs(LanguageUtil.getTranslation("hud.startbuttons.monsters.reignofnether.first"), true),
            fcs(LanguageUtil.getTranslation("hud.startbuttons.monsters.reignofnether.second"))
        )
    );

    public static Button piglinStartButton = new Button(
        "Piglins",
        ICON_SIZE,
        ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"),
        (Keybinding) null,
        () -> CursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_PIGLINS,
        () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS,
        () -> !TutorialClientEvents.isEnabled(),
        () -> CursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_PIGLINS),
        null,
        List.of(
            fcs(LanguageUtil.getTranslation("hud.startbuttons.piglins.reignofnether.first"), true),
            fcs(LanguageUtil.getTranslation("hud.startbuttons.piglins.reignofnether.second"))
        )
    );
}
