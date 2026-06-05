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
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
    public static Faction selectedFaction = Faction.NONE;
    public static boolean isStarting = false; // game is counting down to start

    public static boolean isEnabled() {
        return ClientGameModeHelper.gameMode == GameMode.CLASSIC && !startPoses.isEmpty();
    }

    public static void setPlayerReady(String playerName, boolean ready) {
        for (StartPos startPos : startPoses) {
            if (startPos.playerName.equals(playerName)) {
                if (startPos.ready != ready) {
                    startPos.ready = ready;
                    if (MC.player != null) {
                        if (startPos.ready) {
                            MC.player.sendSystemMessage(Component.translatable("startpos.reignofnether.player_ready",
                                    playerName, getNumPlayersReady(), getNumEnabledPoses()));
                        } else {
                            MC.player.sendSystemMessage(Component.translatable("startpos.reignofnether.player_not_ready", playerName));
                        }
                    }
                }
            }
        }
    }

    public static void setPosEnabled(BlockPos pos, boolean enable) {
        for (StartPos startPos : startPoses) {
            if (startPos.pos.equals(pos)) {
                startPos.enabled = enable;
                if (!startPos.enabled) {
                    startPos.playerName = "";
                    startPos.ready = false;
                    startPos.faction = Faction.NONE;
                }
            }
        }
    }

    public static boolean hasReservedPos() {
        return getPos() != null;
    }

    private static boolean isReady() {
        for (StartPos startPos : startPoses) {
            if (MC.player != null && MC.player.getName().getString().equals(startPos.playerName) && startPos.ready) {
                return true;
            }
        }
        return false;
    }

    public static Button getReadyButton() {
        return new Button("Ready",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/tick.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !isEnabled() || isStarting || isReady(),
                () -> hasReservedPos() && selectedFaction != Faction.NONE,
                () -> {
                    if (MC.player != null)
                        StartPosServerboundPacket.readyPlayer(MC.player.getName().getString());
                },
                null,
                getReadyButtonTooltip()
        );
    }

    private static int getNumPlayersReady() {
        int readyPlayers = 0;
        for (StartPos startPose : startPoses)
            if (startPose.faction != Faction.NONE && startPose.ready && !startPose.playerName.isBlank())
                readyPlayers++;
        return readyPlayers;
    }
    private static int getNumEnabledPoses() {
        int enabledPoses = 0;
        for (StartPos startPose : startPoses)
            if (startPose.enabled)
                enabledPoses++;
        return enabledPoses;
    }

    private static List<FormattedCharSequence> getReadyButtonTooltip() {
        ArrayList<FormattedCharSequence> fcsList = new ArrayList<>();
        fcsList.add(fcs(I18n.get("startpos.reignofnether.ready_button.ready"), true));
        if (!hasReservedPos())
            fcsList.add(fcs(I18n.get("startpos.reignofnether.start_button.no_reserved_pos")));
        if (selectedFaction == Faction.NONE)
            fcsList.add(fcs(I18n.get("startpos.reignofnether.start_button.no_faction")));
        return fcsList;
    }


    public static Button getUnreadyButton() {
        return new Button("Unready",
                14,
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/cross.png"),
                ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame.png"),
                null,
                () -> false,
                () -> !isEnabled() || isStarting || !isReady(),
                () -> true,
                () -> {
                    if (MC.player != null)
                        StartPosServerboundPacket.unreadyPlayer(MC.player.getName().getString());
                },
                null,
                List.of(
                        fcs(I18n.get("startpos.reignofnether.ready_button.unready"), true)
                )
        );
    }

    public static StartPos getPos() {
        for (StartPos startPos : startPoses)
            if (MC.player != null && MC.player.getName().getString().equals(startPos.playerName))
                return startPos;
        return null;
    }

    private static ResourceLocation getIcon() {
        if (getPos() == null)
            return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/block/rts_start_block_white.png");
        else
            return getPos().getIcon();
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
            () -> !isStarting,
            () -> {
                if (MC.player != null) {
                    if (selectedFaction != Faction.VILLAGERS) {
                        selectedFaction = Faction.VILLAGERS;
                        if (getPos() != null)
                            StartPosServerboundPacket.reservePos(getPos().pos, Faction.VILLAGERS, MC.player.getName().getString());
                    } else {
                        selectedFaction = Faction.NONE;
                        if (getPos() != null)
                            StartPosServerboundPacket.reservePos(getPos().pos, Faction.NONE, MC.player.getName().getString());
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
            () -> !isStarting,
            () -> {
                if (MC.player != null) {
                    if (selectedFaction != Faction.MONSTERS) {
                        selectedFaction = Faction.MONSTERS;
                        if (getPos() != null)
                            StartPosServerboundPacket.reservePos(getPos().pos, Faction.MONSTERS, MC.player.getName().getString());
                    } else {
                        selectedFaction = Faction.NONE;
                        if (getPos() != null)
                            StartPosServerboundPacket.reservePos(getPos().pos, Faction.NONE, MC.player.getName().getString());
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
            () -> !isStarting,
            () -> {
                if (MC.player != null) {
                    if (selectedFaction != Faction.PIGLINS) {
                        selectedFaction = Faction.PIGLINS;
                        if (getPos() != null)
                            StartPosServerboundPacket.reservePos(getPos().pos, Faction.PIGLINS, MC.player.getName().getString());
                    } else {
                        selectedFaction = Faction.NONE;
                        if (getPos() != null)
                            StartPosServerboundPacket.reservePos(getPos().pos, Faction.NONE, MC.player.getName().getString());
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
