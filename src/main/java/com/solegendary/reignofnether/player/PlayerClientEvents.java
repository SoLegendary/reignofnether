package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.alliance.AlliancesClient;
import com.solegendary.reignofnether.building.BuildingClientEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientEvents;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientEvents;
import com.solegendary.reignofnether.gamemode.ClientGameModeHelper;
import com.solegendary.reignofnether.gamerules.GameruleClient;
import com.solegendary.reignofnether.hero.HeroClientEvents;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.hud.playerdisplay.PlayerDisplayClientEvents;
import com.solegendary.reignofnether.hud.buttons.HelperButtons;
import com.solegendary.reignofnether.keybinds.Keybindings;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.registrars.SoundRegistrar;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourcesClientEvents;
import com.solegendary.reignofnether.sandbox.SandboxClientEvents;
import com.solegendary.reignofnether.sounds.SoundClientEvents;
import com.solegendary.reignofnether.startpos.StartPosClientEvents;
import com.solegendary.reignofnether.survival.SurvivalClientEvents;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.*;

public class PlayerClientEvents {

    public static long rtsGameTicks = 0;
    private static final Minecraft MC = Minecraft.getInstance();
    public static boolean rtsLocked = false;
    public static boolean canStartRTS = true;
    public static final List<RTSPlayer> rtsPlayers = Collections.synchronizedList(new ArrayList<>());
    public static BlockState lastUsedBlockState = null;

    public static boolean isRTSPlayer() {
        for (RTSPlayer rtsPlayer : rtsPlayers)
            if (MC.player != null && rtsPlayer.name.equals(MC.player.getName().getString()))
                return true;
        return false;
    }

    public static boolean isRTSPlayer(String playerName) {
        for (RTSPlayer rtsPlayer : rtsPlayers)
            if (rtsPlayer.name.equals(playerName))
                return true;
        return false;
    }

    @Nullable
    public static RTSPlayer getRTSPlayer(String playerName) {
        for (RTSPlayer rtsPlayer : rtsPlayers)
            if (rtsPlayer.name.equals(playerName))
                return rtsPlayer;
        return null;
    }

    public static Faction getFaction() {
        for (RTSPlayer rtsPlayer : rtsPlayers)
            if (MC.player != null && rtsPlayer.name.equals(MC.player.getName().getString()))
                return rtsPlayer.faction;
        return Faction.NONE;
    }

    public static RTSPlayer getPlayer(String playerName) {
        for (RTSPlayer rtsPlayer : rtsPlayers)
            if (rtsPlayer.name.equals(playerName))
                return rtsPlayer;
        return null;
    }

    public static Integer getPlayerId(String playerName) {
        var player = getPlayer(playerName);
        if (player == null) {
            return null;
        }

        return player.id;
    }

    public static Integer getPlayerIndex(String playerName) {
        var player = getPlayer(playerName);
        if (player == null) {
            return null;
        }

        return rtsPlayers.indexOf(player);
    }

    public static int getPlayerMapColorId(String playerName) {
        var player = getPlayer(playerName);
        if (player == null) {
            return 0;
        }

        return player.startPosColorId; // corresponds to a MapColor.id
    }

    public static Faction getPlayerFaction(String playerName) {
        var player = getPlayer(playerName);
        if (player == null) {
            return Faction.NONE;
        }

        return player.faction;
    }

    public static String getPlayerName() {
        return MC.player != null ? MC.player.getName().getString() : "";
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("rts-camera").executes((command) -> {
            OrthoviewClientEvents.tryToToggleEnable();
            return 1;
        }));
        evt.getDispatcher().register(Commands.literal("rts-surrender").executes((command) -> {
            PlayerServerboundPacket.surrender();
            return 1;
        }));
        evt.getDispatcher().register(Commands.literal("rts-reset").executes((command) -> {
            if (MC.player != null && MC.player.hasPermissions(2)) {
                PlayerServerboundPacket.resetRTS();
                return 1;
            }
            return 0;
        }));
        evt.getDispatcher().register(Commands.literal("rts-hard-reset").executes((command) -> {
            if (MC.player != null && MC.player.hasPermissions(2)) {
                PlayerServerboundPacket.resetRTSHard();
                return 1;
            }
            return 0;
        }));
        evt.getDispatcher()
            .register(Commands.literal("rts-syncing").then(Commands.literal("enable").executes((command) -> {
                if (MC.player != null && MC.player.hasPermissions(4)) {
                    PlayerServerboundPacket.enableRTSSyncing();
                    return 1;
                }
                return 0;
            })));
        evt.getDispatcher()
            .register(Commands.literal("rts-syncing").then(Commands.literal("disable").executes((command) -> {
                if (MC.player != null && MC.player.hasPermissions(4)) {
                    PlayerServerboundPacket.disableRTSSyncing();
                    return 1;
                }
                return 0;
            })));
        evt.getDispatcher().register(Commands.literal("rts-help").executes((command) -> {
            if (MC.player != null) {
                MC.player.sendSystemMessage(Component.literal(""));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.toggle_fow","/rts-fog enable/disable"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.surrender","/rts-surrender"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.reset", "/rts-reset"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.hard_reset", "/rts-hard-reset"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.lock", "/rts-lock enable/disable"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.ally", "/ally"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.disband", "/disband"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.send_resources", "/sendfood | wood | ore <playername> <amount>"));
                MC.player.sendSystemMessage(Component.translatable("commands.reignofnether.camera", "/rts-camera"));
            }
            return 1;
        }));
        evt.getDispatcher().register(Commands.literal("rts-controls").executes((command) -> {
            if (MC.player != null) {
                MC.player.sendSystemMessage(Component.literal(""));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.toggle_cam"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.refresh_chunks"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.toggle_fps_tps"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.deselect"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.command"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.create_group"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.recenter_map"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.select_same"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.destroy"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.rotate_cam"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.zoom"));
                MC.player.sendSystemMessage(Component.translatable("controls.reignofnether.rotate"));
            }
            return 1;
        }));
    }

    public static void defeat(String playerName) {
        if (MC.player == null) {
            return;
        }

        // remove control of this player's buildings for all players' clients
        for (BuildingPlacement building : BuildingClientEvents.getBuildings())
            if (building.ownerName.equals(playerName))
                building.ownerName = "";

        if (isRTSPlayer())
            PlayerDisplayClientEvents.resetDisplay();

        removeRTSPlayer(playerName);
        ResourcesClientEvents.resourcesList.removeIf(r -> r.ownerName.equals(playerName));

        if (!MC.player.getName().getString().equals(playerName))
            return;

        if (!SandboxClientEvents.isSandboxPlayer(playerName)) {
            MC.gui.setTitle(Component.translatable("titles.reignofnether.defeated"));
            MC.player.playSound(SoundRegistrar.DEFEAT.get(), 0.5f, 1.0f);
        }
        ResearchClient.removeAllResearch();
        ResearchClient.removeAllCheats();
        HudClientEvents.controlGroups.clear();
    }

    public static void victory(String playerName) {
        if (MC.player == null || !MC.player.getName().getString().equals(playerName)) {
            return;
        }
        MC.gui.setTitle(Component.translatable("titles.reignofnether.victorious"));
        MC.player.playSound(SoundRegistrar.VICTORY.get(), 0.5f, 1.0f);
    }

    public static void addRTSPlayer(String playerName, Faction faction, Long id, int startPosColorId) {
        if (!isRTSPlayer(playerName)) {
            rtsPlayers.add(RTSPlayer.getNewPlayer(playerName, faction, id.intValue(), startPosColorId));
            if (MC.player != null && MC.player.getName().getString().equals(playerName)) {
                GameruleClient.gamerulesMenuOpen = false;
                if (!SandboxClientEvents.isSandboxPlayer()) {
                    MC.getMusicManager().stopPlaying();
                    ResearchClient.removeAllCheats();
                }
            }
        }
    }

    public static void removeRTSPlayer(String playerName) {
        boolean removed = rtsPlayers.removeIf(p -> p.name.equals(playerName));
        if (removed && MC.player != null && MC.player.getName().getString().equals(playerName)) {
            SoundClientEvents.stopFadeableMusicInstance();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogoutEvent(PlayerEvent.PlayerLoggedOutEvent evt) {
        // LOG OUT FROM SINGLEPLAYER WORLD ONLY
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId()) {
            resetRTS(true);
            UnitClientEvents.getAllUnits().clear();
            BuildingClientEvents.getBuildings().clear();
            FogOfWarClientEvents.movedToCapitol = false;
            FogOfWarClientEvents.frozenChunks.clear();
            FogOfWarClientEvents.semiFrozenChunks.clear();
            OrthoviewClientEvents.unlockCam();
            HeroClientEvents.fallenHeroes.clear();
            PlayerDisplayClientEvents.resetDisplay();
            PlayerColors.reset();
            CustomBuildingClientEvents.customBuildings.clear();
            CustomBuildingClientEvents.setCustomBuildingToEdit(null);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        // LOG IN TO SINGLEPLAYER WORLD ONLY
        if (MC.player != null && evt.getEntity().getId() == MC.player.getId()) {
            FogOfWarClientEvents.updateFogChunks();
        }
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut evt) {
        // LOG OUT FROM SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer() != null && evt.getPlayer().getId() == MC.player.getId()) {
            resetRTS(true);
            UnitClientEvents.getAllUnits().clear();
            BuildingClientEvents.getBuildings().clear();
            FogOfWarClientEvents.movedToCapitol = false;
            FogOfWarClientEvents.frozenChunks.clear();
            FogOfWarClientEvents.semiFrozenChunks.clear();
            HeroClientEvents.fallenHeroes.clear();
            PlayerDisplayClientEvents.resetDisplay();
            PlayerColors.reset();
            CustomBuildingClientEvents.customBuildings.clear();
        }
    }

    @SubscribeEvent
    public static void onClientLogin(ClientPlayerNetworkEvent.LoggingIn evt) {
        // LOG IN TO SERVER WORLD ONLY
        if (MC.player != null && evt.getPlayer().getId() == MC.player.getId()) {
            FogOfWarClientEvents.updateFogChunks();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent evt) {
        if (evt.phase == TickEvent.Phase.END) {
            rtsGameTicks += 1;
        }
    }

    // disallow opening the creative menu while orthoview is enabled
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening evt) {
        if (OrthoviewClientEvents.isEnabled() &&
            (evt.getScreen() instanceof CreativeModeInventoryScreen ||
            evt.getScreen() instanceof InventoryScreen)) {
            evt.setCanceled(true);
        }
    }

    // allow tab player list menu on the orthoview screen
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render evt) {
        if (OrthoviewClientEvents.isEnabled() && Keybindings.tab.isDown() && MC.level != null) {
            if (!MC.isLocalServer()) {
                MC.gui.getTabList().setVisible(true);
                MC.gui.getTabList()
                    .render(evt.getGuiGraphics(), MC.getWindow().getGuiScaledWidth(), MC.level.getScoreboard(), null);
            } else {
                MC.gui.getTabList().setVisible(false);
            }
        }
    }

    public static void syncRtsGameTime(Long gameTicks) {
        rtsGameTicks = gameTicks;
    }

    public static void resetRTS(boolean hardReset) {
        boolean isSandboxOrScenario = SandboxClientEvents.isSandboxPlayer() || GameruleClient.scenarioMode;
        rtsPlayers.clear();
        HelperButtons.updateButtons();
        SoundClientEvents.stopFadeableMusicInstance();

        HudClientEvents.controlGroups.clear();
        UnitClientEvents.getSelectedUnits().clear();
        UnitClientEvents.getPreselectedUnits().clear();
        if (!isSandboxOrScenario)
            UnitClientEvents.getAllUnits().removeIf(u -> (hardReset || (u instanceof Unit unit && !Unit.hasAnchor(unit))));
        if (!isSandboxOrScenario)
            for (LivingEntity entity : UnitClientEvents.getAllUnits())
                if (entity instanceof Unit unit)
                    unit.setOwnerName("");
        UnitClientEvents.idleWorkerIds.clear();
        ResearchClient.removeAllResearch();
        ResearchClient.removeAllCheats();
        BuildingClientEvents.getSelectedBuildings().clear();
        if (!isSandboxOrScenario)
            BuildingClientEvents.getBuildings().removeIf(b -> b.getBuilding().shouldDestroyOnReset || hardReset);
        if (!isSandboxOrScenario)
            for (BuildingPlacement building : BuildingClientEvents.getBuildings())
                building.ownerName = "";
        ResourcesClientEvents.resourcesList.clear();
        ClientGameModeHelper.gameMode = ClientGameModeHelper.DEFAULT_GAMEMODE;
        ClientGameModeHelper.gameModeLocked = false;
        SurvivalClientEvents.reset();
        StartPosClientEvents.resetAll();
        HeroClientEvents.fallenHeroes.clear();
        AlliancesClient.playersWithAlliedControl.clear();
        PlayerColors.reset();
        PlayerDisplayClientEvents.resetDisplay();
        TimeClientEvents.resetBloodMoon();
        CustomBuildingClientEvents.setCustomBuildingToEdit(null);
    }

    public static void publishScenarioMap() {
        rtsPlayers.clear();
        HelperButtons.updateButtons();
        SoundClientEvents.stopFadeableMusicInstance();
        HudClientEvents.controlGroups.clear();
        UnitClientEvents.getSelectedUnits().clear();
        UnitClientEvents.getPreselectedUnits().clear();
        UnitClientEvents.idleWorkerIds.clear();
        ResearchClient.removeAllResearch();
        ResearchClient.removeAllCheats();
        BuildingClientEvents.getSelectedBuildings().clear();
        ResourcesClientEvents.resourcesList.clear();
        ClientGameModeHelper.gameMode = ClientGameModeHelper.DEFAULT_GAMEMODE;
        ClientGameModeHelper.gameModeLocked = false;
        SurvivalClientEvents.reset();
        StartPosClientEvents.resetAll();
        HeroClientEvents.fallenHeroes.clear();
        AlliancesClient.playersWithAlliedControl.clear();
        PlayerColors.reset();
        PlayerDisplayClientEvents.resetDisplay();
        TimeClientEvents.resetBloodMoon();
        CustomBuildingClientEvents.setCustomBuildingToEdit(null);
    }

    public static void setRTSLock(boolean lock) {
        rtsLocked = lock;
    }

    public static void setCanStartRTS(boolean canStart) {
        canStartRTS = canStart;
    }

    public static void syncBeaconOwnerTicks(String playerName, long ticks) {
        for (int i = 0; i < rtsPlayers.size(); i++)
            if (rtsPlayers.get(i).name.equals(playerName))
                rtsPlayers.get(i).beaconOwnerTicks = (int) ticks;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock evt) {
        if (MC.level != null)
            lastUsedBlockState = MC.level.getBlockState(evt.getPos());
    }

    /*
    public static double red = 0.0d;
    public static double green = 0.0d;
    public static double blue = 0.0d;

    @SubscribeEvent
    public static void onButtonPress2(ScreenEvent.KeyPressed.Pre evt) {
        if (Keybindings.shiftMod.isDown()) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                red -= 0.02f;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                red += 0.02f;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                green -= 0.02f;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                green += 0.02f;
            }
        } else {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                blue -= 0.02f;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                blue += 0.02f;
            }
        }
    }
     */

    /*
    @SubscribeEvent
    public static void onRenderOverLay(RenderGuiOverlayEvent.Pre evt) {
        MiscUtil.drawDebugStrings(evt.getGuiGraphics(), MC.font, new String[] {
                "red: " + red,
                "green: " + green,
                "blue: " + blue,
        });
    }


    public static int titleX = -56;
    public static int titleY = 5;
    public static int width = 380;
    public static int height = 127;
    public static int editionY = 92;

    @SubscribeEvent
    public static void onButtonPress2(ScreenEvent.KeyPressed.Pre evt) {
        if (Keybindings.shiftMod.isDown()) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                titleX -= 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                titleX += 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                titleY -= 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                titleY += 1;
            }
        } else if (Keybindings.ctrlMod.isDown()) {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                editionY -= 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                editionY += 1;
            }
        } else {
            if (evt.getKeyCode() == GLFW.GLFW_KEY_LEFT) {
                width -= 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_RIGHT) {
                width += 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_UP) {
                height += 1;
            }
            else if (evt.getKeyCode() == GLFW.GLFW_KEY_DOWN) {
                height -= 1;
            }
        }
    }
     */
}
