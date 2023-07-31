package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;

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
            if (entity instanceof Unit unit)
                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        System.out.println("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        players.removeIf(player -> player.getId() == id);
    }

    // commands for ops to give resources
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent.Submitted evt) {

        if (evt.getMessage().getString().equals("test strays")) {
            UnitServerEvents.convertAllToUnit(
                evt.getPlayer().getName().getString(),
                evt.getPlayer().getLevel(),
                (LivingEntity entity) ->
                    entity instanceof SkeletonUnit sUnit &&
                    sUnit.getOwnerName().equals(evt.getPlayer().getName().getString()),
                EntityRegistrar.STRAY_UNIT.get()
            );
        }

        if (evt.getPlayer().hasPermissions(4)) {
            String msg = evt.getMessage().getString();
            String[] words = msg.split(" ");
            String playerName = evt.getPlayer().getName().getString();

            if (words.length == 2 && words[0].equalsIgnoreCase("greedisgood")) {
                try {
                    int amount = Integer.parseInt(words[1]);
                    ResourcesServerEvents.addSubtractResources(new Resources(playerName, amount, amount, amount));
                }
                catch(NumberFormatException err) {
                    System.out.println(err);
                }
            }
            List<String> singleWordCheats = List.of("warpten", "operationcwal", "iseedeadpeople", "modifythephasevariance", "medievalman");

            for (String cheatName : singleWordCheats) {
                if (words.length == 1 && words[0].equalsIgnoreCase(cheatName)) {
                    if (ResearchServer.playerHasCheat(playerName, cheatName)) {
                        ResearchServer.removeCheat(playerName, cheatName);
                        ResearchClientboundPacket.removeCheat(playerName, cheatName);
                    }
                    else {
                        ResearchServer.addCheat(playerName, cheatName);
                        ResearchClientboundPacket.addCheat(playerName, cheatName);
                    }
                }
            }

            // apply all cheats
            if (words.length == 1 && words[0].equalsIgnoreCase("allcheats")) {
                ResourcesServerEvents.addSubtractResources(new Resources(playerName, 99999, 99999, 99999));
                for (String cheatName : singleWordCheats) {
                    ResearchServer.addCheat(playerName, cheatName);
                    ResearchClientboundPacket.addCheat(playerName, cheatName);
                }
            }
        }
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
