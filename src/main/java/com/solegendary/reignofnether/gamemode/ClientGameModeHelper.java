package com.solegendary.reignofnether.gamemode;

import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.BeaconPlacement;
import com.solegendary.reignofnether.editor.EditorTest;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.hud.buttons.ButtonBuilder;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.matchstart.MatchStartScreen;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.replay.ReplayClientEvents;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.startpos.StartPosServerboundPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ClientGameModeHelper {

    public static GameMode DEFAULT_GAMEMODE = GameMode.CLASSIC;
    public static GameMode gameMode = DEFAULT_GAMEMODE;
    public static boolean gameModeLocked = false; // locked with startRTS() in any gamemode, unlocked with /rts-reset

    public static void cycleGameMode() {
        if (gameModeLocked || !RTSMapInfoClientEvents.isEditable())
            return; // an old or newer format map is Editor-only

        if (StartPosClientEvents.hasReservedPos()) {
            StartPosClientEvents.selectedFaction = Factions.NONE;
            StartPosServerboundPacket.unreservePos(StartPosClientEvents.getPos().pos);
        }
        // a game, or the editor: wave survival and scenarios are maps with scripts now
        gameMode = gameMode == GameMode.SANDBOX ? GameMode.CLASSIC : GameMode.SANDBOX;
    }

    public static boolean endMatchArmed = false; // the mode button was clicked once mid-match: the next click ends it

    private static boolean inEditor() {
        return SandboxClientEvents.isSandboxPlayer();
    }

    private static boolean inMatch() {
        return PlayerClientEvents.isRTSPlayer() && !inEditor();
    }

    /**
     * Game or Editor, in the top bar: in diplomacy's place before a match and in the Editor, just after it in a match.
     * Before a match it switches (the host goes straight into the Editor); in the Editor it leaves it for the game
     * lobby (not on an old or newer format map, which is Editor-only); in a match the host can end it, on a second click.
     */
    public static final Button modeButton = new ButtonBuilder("Game or Editor")
            .iconResource(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crafting_table_front.png"))
            .isSelected(() -> endMatchArmed)
            .isHidden(() -> EditorTest.isTesting()
                    || ReplayClientEvents.isActive()
                    || (inMatch() || inEditor()) && !StartPosClientEvents.isHost())
            .isEnabled(() -> inMatch() || RTSMapInfoClientEvents.isEditable() && (inEditor() || !gameModeLocked))
            .onLeftClick(ClientGameModeHelper::switchMode)
            .onRightClick(() -> endMatchArmed = false)
            .build();

    private static void switchMode() {
        if (inMatch()) {
            if (endMatchArmed)
                PlayerServerboundPacket.resetRTS();
            endMatchArmed = !endMatchArmed;
        } else if (inEditor()) {
            PlayerServerboundPacket.resetRTS(); // back to the lobby, in game mode
        } else if (gameMode != GameMode.SANDBOX && StartPosClientEvents.isHost()) {
            cycleGameMode();
            if (gameMode == GameMode.SANDBOX)
                PlayerServerboundPacket.startRTS(Factions.NONE, 0d, 0d, 0d);
        } else {
            cycleGameMode();
        }
    }

    private static final ResourceLocation EDITOR_ICON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crafting_table_front.png");
    private static final ResourceLocation GAME_ICON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/grass_block_side.png");

    // what it shows depends on where the player is: called each frame before the top bar draws it
    public static void refreshModeButton() {
        boolean editor = inEditor() || !inMatch() && gameMode == GameMode.SANDBOX;
        modeButton.iconResource = editor ? EDITOR_ICON : GAME_ICON;
        modeButton.tooltipLines = modeTooltip();
    }

    private static List<FormattedCharSequence> modeTooltip() {
        String key = "hud.gamemode.reignofnether.";
        if (!RTSMapInfoClientEvents.isEditable() && !inMatch())
            return List.of(fcs(I18n.get(key + "mode_editor"), true), fcs(I18n.get(key + "mode_editor_only")));
        if (inMatch())
            return List.of(fcs(I18n.get(key + "mode_game"), true),
                    fcs(I18n.get(endMatchArmed ? key + "mode_end_confirm" : key + "mode_end_match")));
        if (inEditor())
            return List.of(fcs(I18n.get(key + "mode_editor"), true), fcs(I18n.get(key + "mode_to_game")));
        return List.of(fcs(I18n.get(gameMode == GameMode.SANDBOX ? key + "mode_editor" : key + "mode_game"), true),
                fcs(I18n.get(gameMode == GameMode.SANDBOX ? key + "mode_to_game" : key + "mode_to_editor")));
    }

    private static String getLockedString() {
        return gameModeLocked ? " " + I18n.get("hud.gamemode.reignofnether.locked") : "";
    }

    private static boolean isKotB() {
        BeaconPlacement beacon = BuildingUtils.getBeacon(true);
        return (beacon != null && beacon.getBuilding().capturable);
    }

    private static Button getClassicButton() {
        boolean hasStartPoses = !StartPosClientEvents.startPoses.isEmpty();
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>();
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.classic1") +
                getLockedString(), true));
        tooltips.add(fcs(""));
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.classic2")));
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.classic3")));
        tooltips.add(fcs(""));
        if (hasStartPoses)
            tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.start_menu")));
        if (!gameModeLocked)
            tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.changemode")));

        return new Button(
                "Classic",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/grass_block_side.png"),
                hasStartPoses ? Keybindings.getFnum(9) : null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    if (hasStartPoses) Minecraft.getInstance().setScreen(new MatchStartScreen());
                },
                ClientGameModeHelper::cycleGameMode,
                tooltips
        );
    }

    private static Button getKotbButton() {
        boolean hasStartPoses = !StartPosClientEvents.startPoses.isEmpty();
        ArrayList<FormattedCharSequence> tooltips = new ArrayList<>();
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.kotb1") +
                getLockedString(), true));
        tooltips.add(fcs(""));
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.kotb2")));
        tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.kotb3")));
        tooltips.add(fcs(""));
        if (hasStartPoses)
            tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.start_menu")));
        if (!gameModeLocked)
            tooltips.add(fcs(I18n.get("hud.gamemode.reignofnether.changemode")));

        return new Button(
                "King of the Beacon",
                Button.itemIconSize,
                ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/nether_star.png"),
                hasStartPoses ? Keybindings.getFnum(9) : null,
                () -> false,
                () -> false,
                () -> true,
                () -> {
                    if (hasStartPoses) Minecraft.getInstance().setScreen(new MatchStartScreen());
                },
                ClientGameModeHelper::cycleGameMode,
                tooltips
        );
    }

    // game or editor, switched with right-click
    public static Button getButton() {
        Button button = switch (gameMode) {
            case CLASSIC -> isKotB() ? getKotbButton() : getClassicButton();
            case SANDBOX -> new Button(
                    "Sandbox",
                    Button.itemIconSize,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/crafting_table_front.png"),
                    (Keybinding) null,
                    () -> false,
                    () -> false,
                    () -> !gameModeLocked,
                    null,
                    ClientGameModeHelper::cycleGameMode,
                    List.of(
                            fcs(I18n.get("hud.gamemode.reignofnether.sandbox1") +
                                    getLockedString(), true),
                            fcs(""),
                            fcs(I18n.get("hud.gamemode.reignofnether.sandbox2")),
                            fcs(I18n.get("hud.gamemode.reignofnether.sandbox3")),
                            fcs(I18n.get("hud.gamemode.reignofnether.sandbox4")),
                            fcs(""),
                            fcs(I18n.get("hud.gamemode.reignofnether.changemode"))
                    )
            );
            default -> null;
        };
        if (button != null)
            button.tooltipOffsetY = 15;
        return button;
    }
}
