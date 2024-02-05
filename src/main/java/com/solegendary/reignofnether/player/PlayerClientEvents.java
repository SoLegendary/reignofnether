package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.TitleCommand;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerClientEvents {

    public static boolean isRTSPlayer = false;

    private static final Minecraft MC = Minecraft.getInstance();

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("surrender")
                .executes((command) -> {
                    PlayerServerboundPacket.surrender();
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("startrts").then(Commands.literal("villagers")
                .executes((command) -> {
                    if (!OrthoviewClientEvents.isEnabled())
                        OrthoviewClientEvents.toggleEnable();
                    PlayerServerboundPacket.startRTS(Faction.VILLAGERS);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("startrts").then(Commands.literal("monsters")
                .executes((command) -> {
                    if (!OrthoviewClientEvents.isEnabled())
                        OrthoviewClientEvents.toggleEnable();
                    PlayerServerboundPacket.startRTS(Faction.MONSTERS);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("startrts").then(Commands.literal("piglins")
                .executes((command) -> {
                    if (!OrthoviewClientEvents.isEnabled())
                        OrthoviewClientEvents.toggleEnable();
                    PlayerServerboundPacket.startRTS(Faction.PIGLINS);
                    return 1;
                })));
        evt.getDispatcher().register(Commands.literal("resetrts")
                .executes((command) -> {
                    PlayerServerboundPacket.resetRTS();
                    return 1;
                }));
    }

    public static void defeat(String playerName) {
        if (MC.player == null)
            return;

        // remove control of this player's buildings for all players' clients
        for (Building building : BuildingClientEvents.getBuildings())
            if (building.ownerName.equals(playerName))
                building.ownerName = "";

        if (!MC.player.getName().getString().equals(playerName))
            return;

        disableRTS(playerName);
        MC.gui.setTitle(Component.literal("You have been defeated"));
        MC.player.playSound(SoundRegistrar.DEFEAT_SOUND.get(), 0.5f, 1.0f);
    }

    public static void victory(String playerName) {
        if (MC.player == null || !MC.player.getName().getString().equals(playerName))
            return;

        MC.gui.setTitle(Component.literal("You are victorious!"));
        MC.player.playSound(SoundRegistrar.VICTORY_SOUND.get(), 0.5f, 1.0f);
    }

    public static void enableRTS(String playerName) {
        if (MC.player != null && MC.player.getName().getString().equals(playerName))
            isRTSPlayer = true;
    }

    public static void disableRTS(String playerName) {
        if (MC.player != null && MC.player.getName().getString().equals(playerName))
            isRTSPlayer = false;
    }

    @SubscribeEvent
    public static void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent evt) {
        // LOG OUT FROM SINGLEPLAYER WORLD ONLY
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId())
            resetRTS();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        // LOG OUT FROM SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer() != null && evt.getPlayer().getId() == MC.player.getId())
            resetRTS();
    }

    public static void resetRTS() {
        isRTSPlayer = false;

        HudClientEvents.controlGroups.clear();
        UnitClientEvents.getSelectedUnits().clear();
        UnitClientEvents.getPreselectedUnits().clear();
        UnitClientEvents.getAllUnits().clear();
        UnitClientEvents.idleWorkerIds.clear();
        ResearchClient.removeAllResearch();
        ResearchClient.removeAllCheats();
        BuildingClientEvents.getSelectedBuildings().clear();
        BuildingClientEvents.getBuildings().clear();
    }
}
