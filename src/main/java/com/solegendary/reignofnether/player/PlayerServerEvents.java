package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import com.solegendary.reignofnether.unit.UnitClientboundPacket;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;

// this class tracks all available players so that any serverside functions that need to affect the player can be
// performed here by sending a client->server packet containing MC.player.getId()

public class PlayerServerEvents {

    public static final ArrayList<ServerPlayer> players = new ArrayList<>();
    public static final ArrayList<ServerPlayer> orthoviewPlayers = new ArrayList<>();

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        System.out.println("Player logged in: " + evt.getEntity().getName().getString() + ", id: " + evt.getEntity().getId());
        ServerPlayer serverPlayer = (ServerPlayer) evt.getEntity();
        players.add((ServerPlayer) evt.getEntity());

        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            UnitClientboundPacket.sendSyncResourcesPacket(entity);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        System.out.println("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        players.removeIf(player -> player.getId() == id);
    }

    public static void enableOrthoview(int id) {
        ServerPlayer player = getPlayerById(id);
        player.removeAllEffects();

        orthoviewPlayers.removeIf(p -> p.getId() == id);
        orthoviewPlayers.add(player);
    }
    public static void disableOrthoview(int id) {
        orthoviewPlayers.removeIf(p -> p.getId() == id);
    }

    private static ServerPlayer getPlayerById(int playerId) {
        return players.stream()
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
            NetworkHooks.openScreen(serverPlayer, namedProvider);
            serverPlayer.setGameMode(GameType.CREATIVE); // could use spectator, but makes rendering less reliable
        }
        else {
            System.out.println("serverPlayer is null, cannot open topdown gui");
        }
    }

    public static void closeTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        serverPlayer.setGameMode(GameType.CREATIVE);
    }

    public static void movePlayer(int playerId, double x, double y, double z) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        serverPlayer.moveTo(x, y, z);
    }
}
