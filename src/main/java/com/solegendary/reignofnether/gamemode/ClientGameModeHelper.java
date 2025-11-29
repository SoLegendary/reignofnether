package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.survival.WaveDifficulty;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

import static com.solegendary.reignofnether.gamerules.GameruleClient.pvpModesOnly;

public class ClientGameModeHelper {

    public static GameMode DEFAULT_GAMEMODE = GameMode.CLASSIC;
    public static GameMode gameMode = DEFAULT_GAMEMODE;
    public static boolean gameModeLocked = false; // locked with startRTS() in any gamemode, unlocked with /rts-reset

    public static void cycleGameMode() {
        if (gameModeLocked || pvpModesOnly)
            return;

        if (StartPosClientEvents.hasReservedPos()) {
            StartPosClientEvents.selectedFaction = Faction.NONE;
            StartPosServerboundPacket.unreservePos(StartPosClientEvents.getPos().pos);
        }
        switch (gameMode) {
            case CLASSIC -> gameMode = GameMode.SURVIVAL;
            case SURVIVAL -> gameMode = GameMode.SANDBOX;
            default -> gameMode = GameMode.CLASSIC;
        }
    }

    public static void cycleWaveDifficulty() {
        switch (SurvivalClientEvents.difficulty) {
            case BEGINNER -> SurvivalClientEvents.difficulty = WaveDifficulty.EASY;
            case EASY -> SurvivalClientEvents.difficulty = WaveDifficulty.MEDIUM;
            case MEDIUM -> SurvivalClientEvents.difficulty = WaveDifficulty.HARD;
            case HARD -> SurvivalClientEvents.difficulty = WaveDifficulty.EXTREME;
            case EXTREME -> SurvivalClientEvents.difficulty = WaveDifficulty.BEGINNER;
        }
    }

    private static String getLockedString() {
        return gameModeLocked || pvpModesOnly ? " " + I18n.get("hud.gamemode.reignofnether.locked") : "";
    }

    private static boolean isKotB() {
        BeaconPlacement beacon = BuildingUtils.getBeacon(true);
        return (beacon != null && beacon.getBuilding().capturable);
    }

    private static Button getClassicButton() {
        return new Button(
                "Classic",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/grass_block_side.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> !gameModeLocked && !pvpModesOnly,
                null,
                ClientGameModeHelper::cycleGameMode,
                List.of(
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.classic1") +
                                getLockedString(), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.classic2"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.classic3"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.changemode"), Style.EMPTY)
                )
        );
    }

    private static Button getKotbButton() {
        return new Button(
                "King of the Beacon",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png"),
                (Keybinding) null,
                () -> false,
                () -> false,
                () -> !gameModeLocked && !pvpModesOnly,
                null,
                ClientGameModeHelper::cycleGameMode,
                List.of(
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.kotb1") +
                                getLockedString(), Style.EMPTY.withBold(true)),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.kotb2"), Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.kotb3"), Style.EMPTY),
                        FormattedCharSequence.forward("", Style.EMPTY),
                        FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.changemode"), Style.EMPTY)
                )
        );
    }

    // all gamemodes are controlled by 1 button, cycled with right-click
    // left click provides functionality specific to the gamemode, eg. changing wave survival difficulty
    public static Button getButton() {
        Button button = switch (gameMode) {
            case CLASSIC -> isKotB() ? getKotbButton() : getClassicButton();
            case SURVIVAL -> new Button(
                    "Survival",
                    Button.itemIconSize,
                    switch (SurvivalClientEvents.difficulty) {
                        case BEGINNER -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/wooden_sword.png");
                        case EASY -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/stone_sword.png");
                        case MEDIUM -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/iron_sword.png");
                        case HARD -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png");
                        case EXTREME -> ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/netherite_sword.png");
                    },
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> !gameModeLocked && !pvpModesOnly,
                    ClientGameModeHelper::cycleWaveDifficulty,
                    ClientGameModeHelper::cycleGameMode,
                    List.of(
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival1") +
                                    getLockedString(), Style.EMPTY.withBold(true)),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival5",
                                    SurvivalClientEvents.difficulty, SurvivalClientEvents.getMinutesPerDay()), Style.EMPTY),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival2"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival3"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival4"), Style.EMPTY),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.survival6"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.changemode"), Style.EMPTY)
                    )
            );
            case SANDBOX -> new Button(
                    "Sandbox",
                    Button.itemIconSize,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crafting_table_front.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> !gameModeLocked && !pvpModesOnly,
                    null,
                    ClientGameModeHelper::cycleGameMode,
                    List.of(
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.sandbox1") +
                                    getLockedString(), Style.EMPTY.withBold(true)),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.sandbox2"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.sandbox3"), Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.sandbox4"), Style.EMPTY),
                            FormattedCharSequence.forward("", Style.EMPTY),
                            FormattedCharSequence.forward(I18n.get("hud.gamemode.reignofnether.changemode"), Style.EMPTY)
                    )
            );
            default -> null;
        };
        if (button != null)
            button.tooltipOffsetY = 15;
        return button;
    }
}
