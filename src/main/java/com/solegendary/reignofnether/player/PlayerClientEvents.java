package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
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
        evt.getDispatcher().register(Commands.literal("rts-surrender")
                .executes((command) -> {
                    PlayerServerboundPacket.surrender();
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("rts-reset")
                .executes((command) -> {
                    PlayerServerboundPacket.resetRTS();
                    return 1;
                }));
        evt.getDispatcher().register(Commands.literal("rts-help")
                .executes((command) -> {
                    if (MC.player != null) {
                        MC.player.sendSystemMessage(Component.literal(" "));
                        MC.player.sendSystemMessage(Component.literal("F12 - Toggle RTS camera"));
                        MC.player.sendSystemMessage(Component.literal("F8 - Refresh chunks"));
                        MC.player.sendSystemMessage(Component.literal("F7 - Toggle FPS/TPS counters"));
                        MC.player.sendSystemMessage(Component.literal("F6 - Toggle hide leaves (experimental)"));
                        MC.player.sendSystemMessage(Component.literal("F1 - Deselect all units and buildings"));
                        MC.player.sendSystemMessage(Component.literal("/rts-fog enable/disable - Toggle fog of war for all players"));
                        MC.player.sendSystemMessage(Component.literal("/rts-surrender - Concede the match"));
                        MC.player.sendSystemMessage(Component.literal("/rts-reset - Delete all units/buildings, set all to spectator"));
                        MC.player.sendSystemMessage(Component.literal("Right click - move, attack or set rallypoint"));
                        MC.player.sendSystemMessage(Component.literal("Ctrl + number key - Create control group"));
                        MC.player.sendSystemMessage(Component.literal("Shift + left-click on map - recenter map"));
                        MC.player.sendSystemMessage(Component.literal("Double left-click on unit - select all same units"));
                        MC.player.sendSystemMessage(Component.literal("Delete - destroy selected units/buildings"));
                        MC.player.sendSystemMessage(Component.literal("Alt + right-click - rotate camera"));
                        MC.player.sendSystemMessage(Component.literal("Alt + scroll - zoom in/out"));
                    }
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
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId()) {
            resetRTS();
            FogOfWarClientEvents.frozenChunks.clear();
            FogOfWarClientEvents.semiFrozenChunks.clear();
        }
    }

    @SubscribeEvent
    public static void onPlayerLoginEvent(PlayerEvent.PlayerLoggedInEvent evt) {
        // LOG IN TO SINGLEPLAYER WORLD ONLY
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId())
            FogOfWarClientEvents.updateFogChunks();
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        // LOG OUT FROM SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer() != null && evt.getPlayer().getId() == MC.player.getId()) {
            resetRTS();
            FogOfWarClientEvents.frozenChunks.clear();
            FogOfWarClientEvents.semiFrozenChunks.clear();
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn evt) {
        // LOG IN TO SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer().getId() == MC.player.getId())
            FogOfWarClientEvents.updateFogChunks();
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
