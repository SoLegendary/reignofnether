package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.Buildings;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.player.PlayerServerboundPacket;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class StartPosClientEvents {

    // client player is considered to have reserved a spot if selectedFaction != NONE && startPosIndex >= 0
    public static ArrayList<StartPos> startPoses = new ArrayList<>();
    public static int startPosIndex = -1;
    public static Faction selectedFaction = Faction.NONE;
    public static boolean isStarting = false; // game is counting down to start

    public static boolean isEnabled() {
        return ClientGameModeHelper.gameMode == GameMode.CLASSIC && !startPoses.isEmpty();
    }

    public static boolean isSelectedPosReservedByOther() {
        return getPos() != null &&
               !getPos().playerName.isEmpty() &&
               MC.player != null &&
               !getPos().playerName.equals(MC.player.getName().getString()) &&
               getPos().faction != Faction.NONE;
    }

    public static boolean hasReservedPos() {
        return getPos() != null && selectedFaction != Faction.NONE;
    }

    public static Button getPositionsButton() {
        return new Button("Starting Positions",
                14,
                getIcon(),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                Keybindings.stop,
                () -> false,
                () -> !isEnabled(),
                () -> !isStarting,
                () -> {
                    if (Keybindings.shiftMod.isDown()) {
                        if (hasReservedPos())
                            OrthoviewClientEvents.centreCameraOnPos(getPos().pos);
                    } else
                        cycleStartBlock(true);
                },
                () -> cycleStartBlock(false),
                getPosButtonTooltip()
        );
    }

    private static List<FormattedCharSequence> getPosButtonTooltip() {
        StartPos startPos = getPos();
        ArrayList<FormattedCharSequence> fcsList = new ArrayList<>();

        fcsList.add(fcs(I18n.get("startpos.reignofnether.positions_button.tooltip1"), true));
        int startPosesSize = 0;
        for (StartPos startPose : startPoses) {
            if (startPose.faction != Faction.NONE) startPosesSize++;
        }
        fcsList.add(fcs(I18n.get("startpos.reignofnether.positions_button.tooltip2", startPosesSize, startPoses.size())));
        fcsList.add(fcs(I18n.get("startpos.reignofnether.positions_button.tooltip3")));

        if (isSelectedPosReservedByOther() && startPos != null) {
            fcsList.add(fcs(I18n.get("startpos.reignofnether.positions_button.tooltip4", startPos.playerName), true));
        } else if (hasReservedPos())
            fcsList.add(fcs(I18n.get("startpos.reignofnether.positions_button.tooltip5"), false));

        return fcsList;
    }

    public static Button getStartButton() {
        return new Button("Start Game",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !isEnabled() || isStarting,
                () -> {
                    if (PlayerClientEvents.rtsLocked) return false;
                    for (StartPos startPos : startPoses) {
                        if (startPos.faction != Faction.NONE) {
                            return true;
                        }
                    }
                    return false;
                },
                PlayerServerboundPacket::startRTSEveryone,
                null,
                getStartButtonTooltip()
        );
    }

    private static List<FormattedCharSequence> getStartButtonTooltip() {
        ArrayList<FormattedCharSequence> fcsList = new ArrayList<>();
        fcsList.add(fcs(I18n.get("startpos.reignofnether.start_button.tooltip1"), true));
        int startPosesSize = 0;
        for (StartPos startPose : startPoses) {
            if (startPose.faction != Faction.NONE) startPosesSize++;
        }
        fcsList.add(fcs(I18n.get("startpos.reignofnether.start_button.tooltip2",
                startPosesSize, startPoses.size())));
        if (!hasReservedPos())
            fcsList.add(fcs(I18n.get("startpos.reignofnether.start_button.tooltip3")));
        return fcsList;
    }

    public static Button getCancelStartButton() {
        return new Button("Cancel Start Game",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !isEnabled() || !isStarting,
                () -> {
                    for (StartPos startPos : startPoses) {
                        if (startPos.faction != Faction.NONE) {
                            return true;
                        }
                    }
                    return false;
                },
                PlayerServerboundPacket::cancelStartRTSEveryone,
                null,
                List.of(
                        fcs(I18n.get("startpos.reignofnether.cancel_start_button"), true)
                )
        );
    }

    public static StartPos getPos() {
        if (startPosIndex < 0 || startPosIndex >= startPoses.size()) {
            return null;
        } else {
            return startPoses.get(startPosIndex);
        }
    }

    private static ResourceLocation getIcon() {
        if (getPos() == null)
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/rts_start_block_white.png");
        else
            return getPos().getIcon();
    }

    private static void cycleStartBlock(boolean forward) {
        try {
            StartPos originalStartPos = getPos();
            if (selectedFaction != Faction.NONE && originalStartPos != null) {
                selectedFaction = Faction.NONE;
                StartPosServerboundPacket.unreservePos(originalStartPos.pos);
            }
            if (forward) {
                startPosIndex += 1;
                if (startPosIndex >= startPoses.size())
                    startPosIndex = 0;
            } else {
                startPosIndex -= 1;
                if (startPosIndex < 0)
                    startPosIndex = startPoses.size() - 1;
            }
            if (!startPoses.isEmpty()) {
                StartPos startPos = getPos();
                if (startPos != null)
                    OrthoviewClientEvents.centreCameraOnPos(startPos.pos);
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println("IndexOutOfBoundsException in cycleStartBlock");
        }
    }

    @SubscribeEvent
    public static void onChangeGamemode(PlayerEvent.PlayerChangeGameModeEvent evt) {
        StartPos startPos = getPos();
        if (evt.getEntity() == MC.player && startPos != null && MC.player != null) {
            selectedFaction = Faction.NONE;
            StartPosServerboundPacket.unreservePos(startPos.pos);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (evt.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
            return;
        if (!OrthoviewClientEvents.isEnabled() || MC.player == null)
            return;

        for (StartPos startPos : startPoses) {
            if (startPos.faction != Faction.NONE) {
                switch (startPos.faction) {
                    case VILLAGERS -> BuildingClientEvents.setBuildingToPlace(Buildings.TOWN_CENTRE);
                    case MONSTERS -> BuildingClientEvents.setBuildingToPlace(Buildings.MAUSOLEUM);
                    case PIGLINS -> BuildingClientEvents.setBuildingToPlace(Buildings.CENTRAL_PORTAL);
                }
                int forceColour = 2;
                if (startPos.playerName.equals(MC.player.getName().getString()))
                    forceColour = 1;
                BuildingClientEvents.drawBuildingToPlace(evt.getPoseStack(), BuildingClientEvents.getBuildingOriginPos(startPos.pos), forceColour);
                BuildingClientEvents.setBuildingToPlace(null);
            }
        }
    }

    private static final Minecraft MC = Minecraft.getInstance();

    public static void resetAll() {
        startPosIndex = -1;
        selectedFaction = Faction.NONE;
        isStarting = false;
        for (StartPos startPos : startPoses)
            startPos.reset();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        // LOG OUT FROM SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer() != null && evt.getPlayer().getId() == MC.player.getId()) {
            resetAll();
            startPoses.clear();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent evt) {
        // LOG OUT FROM SINGLEPLAYER WORLD ONLY
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId()) {
            resetAll();
            startPoses.clear();
        }
    }

    public static Button villagerReadyButton = new Button(
            "Villagers",
            14,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            (Keybinding) null,
            () -> selectedFaction == Faction.VILLAGERS,
            () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS || !isEnabled(),
            () -> !isSelectedPosReservedByOther() && getPos() != null && !isStarting,
            () -> {
                StartPos startPos = getPos();
                if (startPos != null && MC.player != null) {
                    if (selectedFaction != Faction.VILLAGERS) {
                        selectedFaction = Faction.VILLAGERS;
                        StartPosServerboundPacket.reservePos(startPos.pos, Faction.VILLAGERS, MC.player.getName().getString());
                    } else {
                        selectedFaction = Faction.NONE;
                        StartPosServerboundPacket.unreservePos(getPos().pos);
                    }
                }
            },
            null,
            List.of(
                    fcs(I18n.get("hud.startbuttons.villagers.reignofnether.first_startpos"), true),
                    fcs(I18n.get("hud.startbuttons.villagers.reignofnether.second_startpos"))
            )
    );

    public static Button monsterReadyButton = new Button(
            "Monsters",
            14,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
            (Keybinding) null,
            () -> selectedFaction == Faction.MONSTERS,
            () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS || !isEnabled(),
            () -> !isSelectedPosReservedByOther() && getPos() != null && !isStarting,
            () -> {
                StartPos startPos = getPos();
                if (startPos != null && MC.player != null) {
                    if (selectedFaction != Faction.MONSTERS) {
                        selectedFaction = Faction.MONSTERS;
                        StartPosServerboundPacket.reservePos(startPos.pos, Faction.MONSTERS, MC.player.getName().getString());
                    } else {
                        selectedFaction = Faction.NONE;
                        StartPosServerboundPacket.unreservePos(getPos().pos);
                    }
                }
            },
            null,
            List.of(
                    fcs(I18n.get("hud.startbuttons.monsters.reignofnether.first_startpos"), true),
                    fcs(I18n.get("hud.startbuttons.monsters.reignofnether.second_startpos"))
            )
    );

    public static Button piglinReadyButton = new Button(
            "Piglins",
            14,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"),
            (Keybinding) null,
            () -> selectedFaction == Faction.PIGLINS,
            () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS || !isEnabled(),
            () -> !isSelectedPosReservedByOther() && getPos() != null && !isStarting,
            () -> {
                StartPos startPos = getPos();
                if (startPos != null && MC.player != null) {
                    if (selectedFaction != Faction.PIGLINS) {
                        selectedFaction = Faction.PIGLINS;
                        StartPosServerboundPacket.reservePos(startPos.pos, Faction.PIGLINS, MC.player.getName().getString());
                    } else {
                        selectedFaction = Faction.NONE;
                        StartPosServerboundPacket.unreservePos(getPos().pos);
                    }
                }
            },
            null,
            List.of(
                    fcs(I18n.get("hud.startbuttons.piglins.reignofnether.first_startpos"), true),
                    fcs(I18n.get("hud.startbuttons.piglins.reignofnether.second_startpos"))
            )
    );
}
