package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.unit.units.monsters.SkeletonUnit;
import com.solegendary.reignofnether.unit.units.monsters.ZombieUnit;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import org.checkerframework.checker.units.qual.C;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// this class tracks all available players so that any serverside functions that need to affect the player can be
// performed here by sending a client->server packet containing MC.player.getId()

public class PlayerServerEvents {

    public static final ArrayList<ServerPlayer> players = new ArrayList<>();
    public static final ArrayList<ServerPlayer> orthoviewPlayers = new ArrayList<>();
    public static final Set<Integer> rtsPlayerIds = new HashSet<>(); // players that have run /startrts

    // warpten - faster building/unit production
    // operationcwal - faster resource gathering
    // iseedeadpeople - ignore fog of war
    // modifythephasevariance - ignore building requirements
    // medievalman - get all research (cannot disable)
    // greedisgood X - gain X of each resource
    // foodforthought - unlimited population
    public static final List<String> singleWordCheats = List.of(
            "warpten", "operationcwal", "iseedeadpeople", "modifythephasevariance", "medievalman", "foodforthought"
    );

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        ServerPlayer serverPlayer = (ServerPlayer) evt.getEntity();
        players.add((ServerPlayer) evt.getEntity());

        String playerName = serverPlayer.getName().getString();
        System.out.println("Player logged in: " + playerName + ", id: " + serverPlayer.getId());

        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity instanceof Unit unit)
                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);

        ResearchServer.syncResearch(playerName);
        ResearchServer.syncCheats(playerName);

        if (orthoviewPlayers.stream().map(Entity::getId).toList().contains(evt.getEntity().getId())) {
            orthoviewPlayers.add((ServerPlayer) evt.getEntity());
        }

        if (!rtsPlayerIds.contains(serverPlayer.getId())) {
            serverPlayer.sendSystemMessage(Component.literal("Welcome to Reign of Nether").withStyle(Style.EMPTY.withBold(true)));
            serverPlayer.sendSystemMessage(Component.literal("Use /startrts <faction_name> to get started"));
            serverPlayer.sendSystemMessage(Component.literal("Make sure to be in a good base location first!"));
        } else {
            serverPlayer.sendSystemMessage(Component.literal("Welcome back to Reign of Nether").withStyle(Style.EMPTY.withBold(true)));
        }
        if (serverPlayer.hasPermissions(4)) {
            serverPlayer.sendSystemMessage(Component.literal(""));
            serverPlayer.sendSystemMessage(Component.literal("As a server op you may use:"));
            serverPlayer.sendSystemMessage(Component.literal("/fog enable | disable"));
            serverPlayer.sendSystemMessage(Component.literal("/startrts (unlimited use)"));
        }

    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        System.out.println("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        players.removeIf(player -> player.getId() == id);
    }

    public static void startRTS(int playerId, Vec3 pos, Faction faction) {
        ServerPlayer serverPlayer = null;
        for (ServerPlayer player : players)
            if (player.getId() == playerId)
                serverPlayer = player;

        if (serverPlayer == null)
            return;
        if (rtsPlayerIds.contains(playerId) && !serverPlayer.hasPermissions(4))
            return;

        EntityType<? extends Unit> entityType = switch(faction) {
            case VILLAGERS -> EntityRegistrar.VILLAGER_UNIT.get();
            case MONSTERS -> EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
            case NETHERLINGS -> null;
        };
        ServerLevel level = serverPlayer.getLevel();
        for (int i = -1; i <= 1; i++) {
            Entity entity = entityType.create(level);
            if (entity != null) {
                BlockPos bp = MiscUtil.getHighestSolidBlock(level, new BlockPos(pos.x + i, 0, pos.z)).above().above();
                ((Unit) entity).setOwnerName(serverPlayer.getName().getString());
                entity.moveTo(bp, 0,0);
                level.addFreshEntity(entity);
                rtsPlayerIds.add(serverPlayer.getId());
            }
        }
    }

    // commands for ops to give resources
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent.Submitted evt) {

        if (evt.getMessage().getString().equals("test strays")) {
            UnitServerEvents.convertAllToUnit(
                    evt.getPlayer().getName().getString(),
                    evt.getPlayer().getLevel(),
                    (LivingEntity entity) ->
                            entity instanceof SkeletonUnit zUnit &&
                                    zUnit.getOwnerName().equals(evt.getPlayer().getName().getString()),
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
                    evt.setCanceled(true);
                    sendMessageToAllPlayers(playerName + " used cheat: " + words[0] + " " + amount);
                }
                catch(NumberFormatException err) {
                    System.out.println(err);
                }
            }

            for (String cheatName : singleWordCheats) {
                if (words.length == 1 && words[0].equalsIgnoreCase(cheatName)) {
                    if (ResearchServer.playerHasCheat(playerName, cheatName) && !cheatName.equals("medievalman")) {
                        ResearchServer.removeCheat(playerName, cheatName);
                        ResearchClientboundPacket.removeCheat(playerName, cheatName);
                        evt.setCanceled(true);
                        sendMessageToAllPlayers(playerName + " disabled cheat: " + cheatName);
                    }
                    else {
                        ResearchServer.addCheat(playerName, cheatName);
                        ResearchClientboundPacket.addCheat(playerName, cheatName);
                        evt.setCanceled(true);
                        sendMessageToAllPlayers(playerName + " enabled cheat: " + cheatName);
                    }
                }
            }

            // apply all cheats - NOTE can cause concurrentModificationException clientside
            if (words.length == 1 && words[0].equalsIgnoreCase("allcheats")) {
                ResourcesServerEvents.addSubtractResources(new Resources(playerName, 99999, 99999, 99999));
                for (String cheatName : singleWordCheats) {
                    ResearchServer.addCheat(playerName, cheatName);
                    ResearchClientboundPacket.addCheat(playerName, cheatName);
                    evt.setCanceled(true);
                }
                sendMessageToAllPlayers(playerName + " enabled all cheats");
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

    public static void sendMessageToAllPlayers(String msg) {
        for (ServerPlayer player : players)
            player.sendSystemMessage(Component.literal(msg));
    }
}
