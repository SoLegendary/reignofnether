package com.solegendary.reignofnether.player;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.ProductionBuilding;
import com.solegendary.reignofnether.fogofwar.FogOfWarClientboundPacket;
import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServer;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

import java.util.*;

// this class tracks all available players so that any serverside functions that need to affect the player can be
// performed here by sending a client->server packet containing MC.player.getId()

public class PlayerServerEvents {

    // list of what gamemode these players should be in when outside of RTS cam
    private static final ArrayList<Pair<String, GameType>> playerDefaultGameModes = new ArrayList<>();
    private static final GameType defaultGameMode = GameType.SPECTATOR;
    public static final ArrayList<ServerPlayer> players = new ArrayList<>();
    public static final ArrayList<ServerPlayer> orthoviewPlayers = new ArrayList<>();
    private static final List<RTSPlayer> rtsPlayers = Collections.synchronizedList(new ArrayList<>()); // players that have run /startrts

    public static final int TICKS_TO_REVEAL = 60 * ResourceCost.TICKS_PER_SECOND;

    // warpten - faster building/unit production
    // operationcwal - faster resource gathering
    // modifythephasevariance - ignore building requirements
    // medievalman - get all research (cannot reverse)
    // greedisgood X - gain X of each resource
    // foodforthought - unlimited population
    public static final List<String> singleWordCheats = List.of(
            "warpten", "operationcwal", "modifythephasevariance", "medievalman", "foodforthought"
    );

    private static class RTSPlayer {
        public String name;
        public int id; // for AI, always negative
        public int ticksWithoutCapitol = 0;
        public Faction faction;

        private RTSPlayer(ServerPlayer player, Faction faction) {
            this.name = player.getName().getString();
            this.id = player.getId();
            this.faction = faction;
        }

        // bot
        private RTSPlayer(String name, Faction faction) {
            int minId = Collections.min(rtsPlayers.stream().map(r -> r.id).toList());
            if (minId >= 0)
                this.id = -1;
            else
                this.id = minId - 1;
            this.faction = faction;
            this.name = name;
        }

        public static RTSPlayer getNewPlayer(ServerPlayer player, Faction faction) {
            return new RTSPlayer(player, faction);
        }

        public static RTSPlayer getNewBot(String name, Faction faction) {
            return new RTSPlayer(name, faction);
        }

        public boolean isBot() {
            return id < 0;
        }

        public void tick() {
            int numBuildingsOwned = BuildingServerEvents.getBuildings().stream().filter(
                    b -> b.ownerName.equals(this.name)
            ).toList().size();
            int numCapitolsOwned = BuildingServerEvents.getBuildings().stream().filter(
                    b -> b.ownerName.equals(this.name) && b.isCapitol
            ).toList().size();

            if (numBuildingsOwned > 0 && numCapitolsOwned == 0) {
                if (ticksWithoutCapitol < TICKS_TO_REVEAL) {
                    this.ticksWithoutCapitol += 1;
                    if (ticksWithoutCapitol == TICKS_TO_REVEAL) {
                        sendMessageToAllPlayers(this.name + " has not rebuilt their capitol and is being revealed!");
                        FogOfWarClientboundPacket.revealOrHidePlayer(true, this.name);
                    }
                }
            } else {
                this.ticksWithoutCapitol = 0;
            }
        }

        public boolean isRevealed() {
            return this.ticksWithoutCapitol >= TICKS_TO_REVEAL;
        }
    }

    public static boolean isRTSPlayer(String playerName) {
        synchronized (rtsPlayers) {
            return rtsPlayers.stream().filter(p -> p.name.equals(playerName)).toList().size() > 0;
        }
    }

    public static boolean isRTSPlayer(int id) {
        synchronized (rtsPlayers) {
            return rtsPlayers.stream().filter(p -> p.id == id).toList().size() > 0;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        synchronized (rtsPlayers) {
            if (evt.phase == TickEvent.Phase.END)
                for (RTSPlayer rtsPlayer : rtsPlayers)
                    rtsPlayer.tick();
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        ServerPlayer serverPlayer = (ServerPlayer) evt.getEntity();

        players.add((ServerPlayer) evt.getEntity());
        String playerName = serverPlayer.getName().getString();
        System.out.println("Player logged in: " + playerName + ", id: " + serverPlayer.getId());

        // if a player is looking directly at a frozenchunk on login, they may load in the real blocks before
        // they are frozen so move them away then BuildingClientEvents.placeBuilding moves them to their base later
        // don't do this if they don't own any buildings
        if (isRTSPlayer(playerName)) {
            for (Building building : BuildingServerEvents.getBuildings()) {
                if (building.ownerName.equals(playerName)) {
                    movePlayer(serverPlayer.getId(), 0,85,0);
                    break;
                }
            }
        }
        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity instanceof Unit unit)
                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);

        ResearchServer.syncResearch(playerName);
        ResearchServer.syncCheats(playerName);

        if (orthoviewPlayers.stream().map(Entity::getId).toList().contains(evt.getEntity().getId())) {
            orthoviewPlayers.add((ServerPlayer) evt.getEntity());
        }
        if (!TutorialServerEvents.isEnabled()) {
            if (!isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(Component.literal("Welcome to Reign of Nether").withStyle(Style.EMPTY.withBold(true)));
                serverPlayer.sendSystemMessage(Component.literal("Press F12 to toggle RTS camera and join the game"));
                serverPlayer.sendSystemMessage(Component.literal("Use '/rts-help' to see the list of all commands"));
            } else {
                serverPlayer.sendSystemMessage(Component.literal("Welcome back to Reign of Nether").withStyle(Style.EMPTY.withBold(true)));
            }
            if (serverPlayer.hasPermissions(4)) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.literal("As a server op you may use:"));
                serverPlayer.sendSystemMessage(Component.literal("/rts-fog enable | disable"));
                serverPlayer.sendSystemMessage(Component.literal("/rts-reset"));
            }
        }
        if (isRTSPlayer(playerName))
            PlayerClientboundPacket.enableRTSStatus(playerName);
        else
            PlayerClientboundPacket.disableRTSStatus(playerName);
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        System.out.println("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        players.removeIf(player -> player.getId() == id);
    }

    public static void startRTS(int playerId, Vec3 pos, Faction faction) {
        synchronized (rtsPlayers) {
            ServerPlayer serverPlayer = null;
            for (ServerPlayer player : players)
                if (player.getId() == playerId)
                    serverPlayer = player;

            if (serverPlayer == null)
                return;
            if (isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                serverPlayer.sendSystemMessage(Component.literal("You already started your RTS match!"));
                serverPlayer.sendSystemMessage(Component.literal(""));
                return;
            }

            EntityType<? extends Unit> entityType = switch(faction) {
                case VILLAGERS -> EntityRegistrar.VILLAGER_UNIT.get();
                case MONSTERS -> EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
                case PIGLINS -> EntityRegistrar.GRUNT_UNIT.get();
                case NONE -> null;
            };
            rtsPlayers.add(RTSPlayer.getNewPlayer(serverPlayer, faction));
            PlayerClientboundPacket.enableRTSStatus(serverPlayer.getName().getString());

            ServerLevel level = serverPlayer.getLevel();
            for (int i = -1; i <= 1; i++) {
                Entity entity = entityType != null ? entityType.create(level) : null;
                if (entity != null) {
                    BlockPos bp = MiscUtil.getHighestNonAirBlock(level, new BlockPos(pos.x + i, 0, pos.z)).above().above();
                    ((Unit) entity).setOwnerName(serverPlayer.getName().getString());
                    entity.moveTo(bp, 0,0);
                    level.addFreshEntity(entity);
                }
            }
            if (faction == Faction.MONSTERS) {
                level.setDayTime(13000);
            }
            ResourcesServerEvents.resetResources(serverPlayer.getName().getString());

            if (!TutorialServerEvents.isEnabled()) {
                serverPlayer.sendSystemMessage(Component.literal(""));
                sendMessageToAllPlayers(serverPlayer.getName().getString() + " has started their game!", true);
                sendMessageToAllPlayers("There are now " + rtsPlayers.size() + " total RTS player(s)");
            }
        }
    }

    public static void startRTSBot(String name, Vec3 pos, Faction faction) {
        synchronized (rtsPlayers) {
            ServerLevel level;
            if (players.isEmpty())
                return;
            else
                level = players.get(0).getLevel();

            EntityType<? extends Unit> entityType = switch(faction) {
                case VILLAGERS -> EntityRegistrar.VILLAGER_UNIT.get();
                case MONSTERS -> EntityRegistrar.ZOMBIE_VILLAGER_UNIT.get();
                case PIGLINS -> EntityRegistrar.GRUNT_UNIT.get();
                case NONE -> null;
            };
            RTSPlayer bot = RTSPlayer.getNewBot(name, faction);
            rtsPlayers.add(bot);

            for (int i = -1; i <= 1; i++) {
                Entity entity = entityType != null ? entityType.create(level) : null;
                if (entity != null) {
                    BlockPos bp = MiscUtil.getHighestNonAirBlock(level, new BlockPos(pos.x + i, 0, pos.z)).above().above();
                    ((Unit) entity).setOwnerName(bot.name);
                    entity.moveTo(bp, 0,0);
                    level.addFreshEntity(entity);
                }
            }
            if (faction == Faction.MONSTERS) {
                level.setDayTime(13000);
            }
            ResourcesServerEvents.resetResources(bot.name);

            if (!TutorialServerEvents.isEnabled()) {
                sendMessageToAllPlayers(bot.name + " (bot) has been added to the game!", true);
                sendMessageToAllPlayers("There are now " + rtsPlayers.size() + " total RTS player(s)");
            }
        }
    }

    // commands for ops to give resources
    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent.Submitted evt) {
        /*
        if (evt.getMessage().getString().equals("test spiders")) {
            UnitServerEvents.convertAllToUnit(
                    evt.getPlayer().getName().getString(),
                    evt.getPlayer().getLevel(),
                    (LivingEntity entity) ->
                            entity instanceof SpiderUnit sUnit &&
                                    sUnit.getOwnerName().equals(evt.getPlayer().getName().getString()),
                    EntityRegistrar.POISON_SPIDER_UNIT.get()
            );
        }*/
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

            String playerName = serverPlayer.getName().getString();
            playerDefaultGameModes.removeIf(p -> p.getFirst().equals(playerName));
            playerDefaultGameModes.add(new Pair<>(playerName, serverPlayer.gameMode.getGameModeForPlayer()));

            serverPlayer.setGameMode(GameType.CREATIVE); // could use spectator, but makes rendering less reliable
        }
        else {
            System.out.println("serverPlayer is null, cannot open topdown gui");
        }
    }

    public static void closeTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);

        for (Pair<String, GameType> defaultGameMode : playerDefaultGameModes) {
            if (serverPlayer.getName().getString().equals(defaultGameMode.getFirst())) {
                serverPlayer.setGameMode(defaultGameMode.getSecond());
                return;
            }
        }
        serverPlayer.setGameMode(defaultGameMode);
    }

    public static void movePlayer(int playerId, double x, double y, double z) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        serverPlayer.moveTo(x, y, z);
    }

    public static void sendMessageToAllPlayers(String msg) {
        sendMessageToAllPlayers(msg, false);
    }

    public static void sendMessageToAllPlayers(String msg, boolean bold) {
        for (ServerPlayer player : players) {
            player.sendSystemMessage(Component.literal(""));
            if (bold)
                player.sendSystemMessage(Component.literal(msg).withStyle(Style.EMPTY.withBold(true)));
            else
                player.sendSystemMessage(Component.literal(msg));
            player.sendSystemMessage(Component.literal(""));
        }
    }

    // defeat a player, giving them a defeat screen, removing all their unit/building control and removing them from rtsPlayers
    public static void defeat(int playerId, String reason) {
        for (ServerPlayer player : players) {
            if (player.getId() == playerId) {
                defeat(player.getName().getString(), reason);
                return;
            }
        }
    }

    public static void defeat(String playerName, String reason) {
        synchronized (rtsPlayers) {
            rtsPlayers.removeIf(rtsPlayer -> {
                if (rtsPlayer.name.equals(playerName)) {
                    sendMessageToAllPlayers(playerName + " has " + reason + " and is defeated!", true);
                    sendMessageToAllPlayers("There are " + (rtsPlayers.size() - 1) + " RTS player(s) remaining");

                    PlayerClientboundPacket.defeat(playerName);

                    for (LivingEntity entity : UnitServerEvents.getAllUnits())
                        if (entity instanceof Unit unit && unit.getOwnerName().equals(playerName))
                            unit.setOwnerName("");

                    for (Building building : BuildingServerEvents.getBuildings())
                        if (building.ownerName.equals(playerName))
                            building.ownerName = "";

                    return true;
                }
                return false;
            });
            // if there is only one player left, they are automatically victorious
            if (rtsPlayers.size() == 1) {
                for (RTSPlayer rtsPlayer : rtsPlayers) {
                    sendMessageToAllPlayers(rtsPlayer.name + " is victorious!", true);
                    PlayerClientboundPacket.victory(rtsPlayer.name);
                }
            }
        }
    }

    public static void resetRTS() {
        synchronized (rtsPlayers) {
            rtsPlayers.clear();

            for (LivingEntity entity : UnitServerEvents.getAllUnits())
                entity.kill();
            UnitServerEvents.getAllUnits().clear();

            for (Building building : BuildingServerEvents.getBuildings()) {
                if (building instanceof ProductionBuilding productionBuilding) {
                    productionBuilding.productionQueue.clear();
                    building.destroy((ServerLevel) building.getLevel());
                }
            }
            BuildingServerEvents.getBuildings().clear();
            ResearchServer.removeAllResearch();
            ResearchServer.removeAllCheats();

            PlayerClientboundPacket.resetRTS();

            sendMessageToAllPlayers("Match has been reset!", true);
        }
    }
}
