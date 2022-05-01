package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.apache.logging.log4j.core.jmx.Server;

import java.util.ArrayList;

// this class tracks all available players so that any serverside functions that need to affect the player can be
// performed here by sending a client->server packet containing MC.player.getId()

public class PlayerServerEvents {

    private static final ArrayList<ServerPlayer> serverPlayers = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        System.out.println("Player logged in: " + evt.getPlayer().getName().getString() + ", id: " + evt.getPlayer().getId());
        serverPlayers.add((ServerPlayer) evt.getPlayer());
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getPlayer().getId();
        System.out.println("Player logged out: " + evt.getPlayer().getName().getString() + ", id: " + id);
        serverPlayers.removeIf(player -> player.getId() == id);
    }

    private static ServerPlayer getPlayerById(int playerId) {
        return serverPlayers.stream()
            .filter(player -> playerId == player.getId())
            .findAny()
            .orElse(null);
    }

    public static void openTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);

        // containers have to be opened server side so that the server can track its data
        if (serverPlayer != null) {
            MenuConstructor provider = TopdownGuiContainer.getServerContainerProvider();
            MenuProvider namedProvider = new SimpleMenuProvider(provider, TopdownGuiContainer.TITLE);
            NetworkHooks.openGui(serverPlayer, namedProvider);
            serverPlayer.setGameMode(GameType.SPECTATOR);
        }
        else {
            System.out.println("serverPlayer is null, cannot open topdown gui");
        }
    }

    public static void closeTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);

        GameType previousGameMode = serverPlayer.gameMode.getPreviousGameModeForPlayer();
        if (previousGameMode != null)
            serverPlayer.setGameMode(previousGameMode);
    }

    public static void movePlayer(int playerId, double x, double y, double z) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        serverPlayer.moveTo(x, y, z);
    }
}
