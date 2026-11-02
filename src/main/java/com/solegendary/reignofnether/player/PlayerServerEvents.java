package com.solegendary.reignofnether.player;

import com.solegendary.reignofnether.building.buildings.placements.ItemShopPlacement;
import com.solegendary.reignofnether.taskscheduler.TaskSchedulerServerEvents;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.replay.ReplayServerEvents;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.ability.TradeAction;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.matchstart.MatchResumeServerEvents;
import com.solegendary.reignofnether.alliance.AllyCommand;
import com.solegendary.reignofnether.api.ReignOfNetherRegistries;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.buildings.neutral.Beacon;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.building.buildings.placements.ProductionPlacement;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.fogofwar.FogChunkSnapshot;
import com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents;
import com.solegendary.reignofnether.gamemode.GameMode;
import com.solegendary.reignofnether.gamemode.GameModeClientboundPacket;
import com.solegendary.reignofnether.gamerules.GameruleClientboundPacket;
import com.solegendary.reignofnether.guiscreen.TopdownGuiContainer;
import com.solegendary.reignofnether.hero.HeroClientboundPacket;
import com.solegendary.reignofnether.hero.HeroServerEvents;
import com.solegendary.reignofnether.items.ItemServerEvents;
import com.solegendary.reignofnether.items.RandomItemDropRule;
import com.solegendary.reignofnether.registrars.EntityRegistrar;
import com.solegendary.reignofnether.registrars.GameRuleRegistrar;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.sandbox.SandboxServer;
import com.solegendary.reignofnether.scenario.ScenarioRole;
import com.solegendary.reignofnether.scenario.ScenarioServerEvents;
import com.solegendary.reignofnether.scenario.ScenarioUtils;
import com.solegendary.reignofnether.soldier.SoldierServerEvents;
import com.solegendary.reignofnether.ping.PingServerEvents;
import com.solegendary.reignofnether.taskscheduler.TaskSchedulerServerEvents;
import com.solegendary.reignofnether.startpos.CoCommanderCommand;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.survival.SurvivalServerEvents;
import com.solegendary.reignofnether.time.TimeServerEvents;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import com.solegendary.reignofnether.util.MiscUtil;
import com.solegendary.reignofnether.worldborder.WorldBorderServerEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.solegendary.reignofnether.building.BuildingServerEvents.random;
import static com.solegendary.reignofnether.building.BuildingServerEvents.saveBuildings;
import static com.solegendary.reignofnether.time.TimeUtils.getWaveSurvivalTimeModifier;
import static net.minecraft.world.level.GameRules.RULE_DISABLE_ELYTRA_MOVEMENT_CHECK;

// this class tracks all available players so that any serverside functions that need to affect the player can be
// performed here by sending a client->server packet containing MC.player.getId()

public class PlayerServerEvents {

    // list of what gamemode these players should be in when outside of RTS cam
    private static final Map<String, GameType> playerDefaultGameModes = new HashMap<>();
    private static final Map<String, Boolean> playerGuiOpenStatus = new HashMap<>();

    public static final ArrayList<ServerPlayer> players = new ArrayList<>();
    public static final ArrayList<ServerPlayer> orthoviewPlayers = new ArrayList<>();
    // players that are currently playing a match
    public static final List<RTSPlayer> rtsPlayers = Collections.synchronizedList(new ArrayList<>());
    // list of players after a match ends
    public static final List<RTSPlayer> postGameRtsPlayers = Collections.synchronizedList(new ArrayList<>());
    public static boolean rtsLocked = false; // can players join as RTS players or not?
    public static boolean rtsSyncingEnabled = true; // will logging in players sync units and buildings?

    private static final int MONSTER_START_TIME_OF_DAY = 500; // 500 = dawn, 6500 = noon, 12500 = dusk

    public static final int TICKS_TO_REVEAL = 60 * ResourceCost.TICKS_PER_SECOND;

    public static long rtsGameTicks = 0; // ticks up as long as there is at least 1 rtsPlayer

    public static ServerLevel serverLevel = null;

    // warpten - faster building/unit production
    // operationcwal - faster resource gathering
    // modifythephasevariance - ignore building requirements
    // medievalman - get all research (cannot reverse)
    // greedisgood X - gain X of each resource
    // foodforthought - ignore soft population caps
    // thereisnospoon - allow changing survival wave by clicking the wave indicator and using debug commands
    // slipslopslap - monster units are unaffected by sunlight
    // wouldyoukindly - allow control of non-unit mobs in RTS mode
    // thebeastofcaerbannog - spawns the Killer Rabbit
    // elitetaurenchieftain - levels all owned heroes to 10
    public static final List<String> singleWordCheats = List.of(
        "warpten",
        "operationcwal",
        "modifythephasevariance",
        "medievalman",
        "foodforthought",
        "thereisnospoon",
        "slipslopslap",
        "wouldyoukindly"
    );

    public static void saveRTSPlayers() {
        if (serverLevel == null) {
            return;
        }
        RTSPlayerSaveData data = RTSPlayerSaveData.getInstance(serverLevel);
        data.rtsPlayers.clear();
        data.rtsPlayers.addAll(rtsPlayers);
        PlayerRoles.saveTo(data.soldierOf, data.coCommanderOf);
        data.alliances.clear();
        AlliancesServerEvents.getAlliances().forEach((owner, allies) -> data.alliances.put(owner, new HashSet<>(allies)));
        data.alliedControl.clear();
        data.alliedControl.addAll(AlliancesServerEvents.playersWithAlliedControl);
        data.rtsLocked = rtsLocked;
        data.rtsGameTicks = rtsGameTicks;
        data.fogEnabled = FogOfWarServerEvents.isEnabled();
        data.save();
        serverLevel.getDataStorage().save();
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);

        if (level != null) {
            RTSPlayerSaveData data = RTSPlayerSaveData.getInstance(level);

            rtsPlayers.clear();
            rtsPlayers.addAll(data.rtsPlayers);
            PlayerRoles.loadFrom(data.soldierOf, data.coCommanderOf);
            FogOfWarServerEvents.invalidateRtsCache();
            MatchResumeServerEvents.resumePending = false; // static: don't carry it over from the last world

            // a match in progress: bring back what isn't in the world itself, and wait for the host to resume
            if (!rtsPlayers.isEmpty()) {
                data.alliances.forEach((owner, allies) -> allies.forEach(ally -> AlliancesServerEvents.addAlliance(owner, ally)));
                AlliancesServerEvents.playersWithAlliedControl.addAll(data.alliedControl);
                rtsLocked = data.rtsLocked;
                rtsGameTicks = data.rtsGameTicks;
                FogOfWarServerEvents.restoreOnLoad(data.fogEnabled);
                // saved games are singleplayer/LAN worlds; a dedicated server just carries on
                MatchResumeServerEvents.resumePending = !evt.getServer().isDedicatedServer();
            }

            for (RTSPlayer rtsPlayer : rtsPlayers) {
                if (rtsPlayer.faction == Factions.NONE) {
                    GameModeClientboundPacket.setAndLockAllClientGameModes(GameMode.SANDBOX);
                    enableAllCheats(rtsPlayer.name);
                    break;
                }
            }
            UnitServerEvents.maxPopulation = level.getGameRules().getInt(GameRuleRegistrar.MAX_POPULATION);

            level.getGameRules().getRule(RULE_DISABLE_ELYTRA_MOVEMENT_CHECK).set(true, evt.getServer());
        }
    }

    private static final int SAVE_TICKS_MAX = 1200;
    private static int saveTicks = 0;
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent evt) {
        saveRTSPlayers();
    }

    // statics outlive the world: clear them so the next world opened in this game starts clean
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent evt) {
        rtsPlayers.clear();
        postGameRtsPlayers.clear();
        players.clear();
        orthoviewPlayers.clear();
        playerDefaultGameModes.clear();
        playerGuiOpenStatus.clear();
        rtsLocked = false;
        rtsSyncingEnabled = true;
        rtsGameTicks = 0;
    }

    // ops, or the singleplayer/LAN host (who has no permission level when cheats are off)
    public static boolean isOpOrHost(ServerPlayer player) {
        return player.hasPermissions(2) || player.server.isSingleplayerOwner(player.getGameProfile());
    }

    // commanders, including co-commanders (PlayerRoles)
    public static boolean isRTSPlayer(String playerName) {
        return PlayerRoles.roleOf(playerName) == PlayerRole.COMMANDER;
    }

    // whether this name has its own entry in rtsPlayers; most code wants isRTSPlayer or PlayerRoles.roleOf
    public static boolean inRtsList(String playerName) {
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers) {
                if (p.name.equals(playerName)) return true;
            }
            return false;
        }
    }

    // co-commanders get their lead's entry
    @Nullable
    // every player's army (non-worker units) right now, for the end screen's chart and peak army
    private static void sampleArmies() {
        Map<String, Integer> army = new HashMap<>();
        for (LivingEntity entity : UnitServerEvents.getAllUnits())
            if (entity instanceof Unit unit && !(entity instanceof WorkerUnit))
                army.merge(unit.getOwnerName(), 1, Integer::sum);
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers) {
                int size = army.getOrDefault(p.name, 0);
                p.armyTimeline.add(size);
                RTSPlayerScoresEnum peak = RTSPlayerScoresEnum.PEAK_ARMY;
                p.scores.scoreList.put(peak, Math.max(p.scores.scoreList.get(peak), size));
            }
        }
    }

    public static RTSPlayer getRTSPlayer(String playerName) {
        String owner = PlayerRoles.ownerOf(playerName);
        synchronized (rtsPlayers) {
            for (RTSPlayer rtsPlayer : rtsPlayers)
                if (rtsPlayer.name.equals(owner))
                    return rtsPlayer;
        }
        return null;
    }

    public static boolean isRTSPlayer(int id) {
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers) {
                if (p.id == id) return true;
            }
        }
        for (ServerPlayer player : players)
            if (player.getId() == id)
                return isRTSPlayer(player.getName().getString());
        return false;
    }

    public static boolean isBot(String playerName) {
        synchronized (rtsPlayers) {
            for (RTSPlayer rtsPlayer : rtsPlayers)
                if (rtsPlayer.name.equalsIgnoreCase(playerName)) {
                    return rtsPlayer.isBot();
                }
        }
        return false;
    }

    public static boolean isBot(int id) {
        synchronized (rtsPlayers) {
            for (RTSPlayer rtsPlayer : rtsPlayers)
                if (rtsPlayer.id == id) {
                    return rtsPlayer.isBot();
                }
        }
        return false;
    }

    public static boolean isGameActive() {
        return !rtsPlayers.isEmpty();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent evt) {
        serverLevel = evt.getServer().getLevel(Level.OVERWORLD);

        // also recapture when a match in progress has lost its snapshot (dark chunks would render as void)
        // ponytail: retries every tick if the snapshot dir can't be created; add a give-up flag if that ever happens
        if (FogChunkSnapshot.shouldRecapture
                || (!FogChunkSnapshot.hasAny() && FogOfWarServerEvents.isEnabled() && isGameActive())) {
            FogChunkSnapshot.captureFogChunks(serverLevel);
        }

        synchronized (rtsPlayers) {
            if (evt.phase == TickEvent.Phase.END) {
                for (RTSPlayer rtsPlayer : rtsPlayers)
                    rtsPlayer.serverTick();

                for (RTSPlayer rtsPlayer : rtsPlayers) {
                    if (rtsPlayer.beaconOwnerTicks == Beacon.getTicksToWin(serverLevel)) {
                        PlayerServerEvents.beaconVictory(rtsPlayer.name);
                        break;
                    }
                }
                if (rtsPlayers.isEmpty()) {
                    rtsGameTicks = 0;
                } else {
                    rtsGameTicks += 1;
                    if (rtsGameTicks % MatchStatsClientboundPacket.ARMY_SAMPLE_TICKS == 0)
                        sampleArmies();
                    if (rtsGameTicks % 200 == 0) {
                        PlayerClientboundPacket.syncRtsGameTime(rtsGameTicks);
                    }
                    if (rtsGameTicks % 20 == 0) {
                        for (RTSPlayer rtsPlayer : rtsPlayers) {
                            PlayerClientboundPacket.syncBeaconOwnerTicks(rtsPlayer.name, rtsPlayer.beaconOwnerTicks);
                        }
                    }
                }
            }
        }
        if (evt.phase == TickEvent.Phase.END) {
            saveTicks += 1;
            if (saveTicks >= SAVE_TICKS_MAX) {
                ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
                if (level != null) {
                    saveRTSPlayers();
                    saveTicks = 0;
                }
            }
        }
    }

    public static void syncUnits() {
        for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
            if (entity instanceof Unit unit) {
                UnitSyncClientboundPacket.sendSyncResourcesPacket(unit);
                UnitSyncClientboundPacket.sendSyncOwnerNamePacket(unit);
                UnitSyncClientboundPacket.sendSyncScenarioRoleIndexPacket(unit);
                if (unit.getAnchor() != null)
                    UnitSyncClientboundPacket.sendSyncAnchorPosPacket(entity, unit.getAnchor());
            }
            if (entity instanceof HeroUnit hero) {
                HeroClientboundPacket.setExperience(entity.getId(), hero.getExperience());
                HeroClientboundPacket.setSkillPoints(entity.getId(), hero.getSkillPoints());
                HeroClientboundPacket.setCharges(entity.getId(), hero.getChargesForSaveData());
                List<HeroAbility> abls = hero.getHeroAbilities();
                if (abls.size() > 0)
                    HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(0).getRank(hero), 0);
                if (abls.size() > 1)
                    HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(1).getRank(hero), 1);
                if (abls.size() > 2)
                    HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(2).getRank(hero), 2);
                if (abls.size() > 3)
                    HeroClientboundPacket.setAbilityRank(entity.getId(), abls.get(3).getRank(hero), 3);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        ServerPlayer serverPlayer = (ServerPlayer) evt.getEntity();

        players.add((ServerPlayer) evt.getEntity());
        String playerName = serverPlayer.getName().getString();
        // entity ids change every session: point a returning RTS player at their new one
        RTSPlayer returning = getRTSPlayer(playerName);
        if (returning != null && returning.id >= 0)
            returning.id = serverPlayer.getId();
        ReignOfNether.LOGGER.info("Player logged in: " + playerName + ", id: " + serverPlayer.getId());

        PlayerRoles.syncTo(serverPlayer); // first: the client filters research by the owner it acts as
        if (rtsSyncingEnabled) {
            MinecraftServer server = evt.getEntity().level().getServer();
            if (server != null && !server.isDedicatedServer()) {
                CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS)
                        .execute(() -> server.execute(PlayerServerEvents::syncUnits));
            } else {
                syncUnits();
            }
            ResearchServerEvents.syncResearch(PlayerRoles.ownerOf(playerName));
            ResearchServerEvents.syncCheats(PlayerRoles.ownerOf(playerName));
        }

        boolean inOrthoviewList = false;
        for (ServerPlayer orthoviewPlayer : orthoviewPlayers) {
            if (orthoviewPlayer.getId() == evt.getEntity().getId())  {
                inOrthoviewList = true;
                break;
            }
        }
        if (!inOrthoviewList)
            orthoviewPlayers.add((ServerPlayer) evt.getEntity());

        if (!TutorialServerEvents.isEnabled()) {
            if (!isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(SystemMessage.important(Component.translatable("tutorial.reignofnether.welcome")));
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.join")));
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.help")));
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.controls")));
                if (rtsLocked) {
                    serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.locked")));
                }
            } else {
                serverPlayer.sendSystemMessage(SystemMessage.important(Component.translatable("tutorial.reignofnether.welcome_back")));
            }
            if (serverPlayer.hasPermissions(4)) {
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.op_commands")));
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.fog")));
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.lock")));;
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.reset")));
            }
            if (!rtsSyncingEnabled) {
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.sync_disabled1")));
                serverPlayer.sendSystemMessage(SystemMessage.info(Component.translatable("tutorial.reignofnether.sync_disabled2")));
            }
        }
        if (getRTSPlayer(playerName) == null) {
            PlayerClientboundPacket.removeRTSPlayer(playerName);
        }
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            PlayerClientboundPacket.addRTSPlayer(
                    rtsPlayer.name, rtsPlayer.faction, (long) rtsPlayer.id,
                    rtsPlayer.startPosColorId, rtsPlayer.isDogPerson, true
            );
        }

        if (rtsLocked) {
            PlayerClientboundPacket.lockRTS(playerName);
        } else {
            PlayerClientboundPacket.unlockRTS(playerName);
        }

        if (rtsSyncingEnabled) {
            PlayerClientboundPacket.enableStartRTS(playerName);
        } else {
            PlayerClientboundPacket.disableStartRTS(playerName);
        }

        updateMarketRates(playerName);

        // a little later, so the screen opens after the world has finished loading on the client
        if (MatchResumeServerEvents.resumePending) {
            MinecraftServer server = serverPlayer.server;
            CompletableFuture.delayedExecutor(1000, TimeUnit.MILLISECONDS).execute(() -> server.execute(() -> {
                if (!serverPlayer.hasDisconnected())
                    MatchResumeServerEvents.sendScreenToAll(server); // everyone sees who is back
            }));
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent evt) {
        int id = evt.getEntity().getId();
        ReignOfNether.LOGGER.info("Player logged out: " + evt.getEntity().getName().getString() + ", id: " + id);
        players.removeIf(player -> player.getId() == id);
        orthoviewPlayers.removeIf(player -> player.getId() == id);
        PingServerEvents.forget(evt.getEntity().getName().getString());
        // while a saved game waits, the others see them go offline (once they've left the player list)
        if (MatchResumeServerEvents.resumePending && evt.getEntity() instanceof ServerPlayer sp)
            sp.server.execute(() -> MatchResumeServerEvents.sendScreenToAll(sp.server));
    }

    public static void startRTS(int playerId, Vec3 pos, Faction faction) {
        startRTS(playerId, pos, faction, 0);
    }

    // readied start is a simultaneous start from players using RTS start pos blocks, difference being:
    // - places the capitol foundations automatically
    // - spawns workers outside the foundations
    // - no start messages are sent other than the one from the countdown
    public static void startRTS(int playerId, Vec3 pos, Faction faction, int startPosColorId) {
        if (faction == Factions.RANDOM) 
            faction = MiscUtil.getRandomItem(List.of(Factions.VILLAGERS, Factions.MONSTERS, Factions.PIGLINS));
        ReignOfNether.LOGGER.info("[Player] startRTS: playerId={}, pos=[{},{},{}], faction={}, startPosColorId={}", playerId, pos.x, pos.y, pos.z, faction, startPosColorId);
        synchronized (rtsPlayers) {
            boolean readiedStart = startPosColorId != 0;

            ServerPlayer serverPlayer = null;
            for (ServerPlayer player : players)
                if (player.getId() == playerId)
                    serverPlayer = player;

            if (serverPlayer == null) {
                return;
            }
            if (rtsLocked) {
                serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("server.reignofnether.locked")));
                return;
            }
            if (isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("server.reignofnether.already_started")));
                return;
            }
            if (serverPlayer.level().getWorldBorder().getDistanceToBorder(pos.x, pos.z) < 1 && faction != Factions.NONE) {
                serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("server.reignofnether.outside_border")));
                return;
            }
            boolean isDogPerson = random.nextBoolean();
            captureFreshGameFog((ServerLevel) serverPlayer.level());
            RandomItemDropRule randomItemDropRule = RandomItemDropRule.fromValue(
                    serverPlayer.level().getGameRules().getRule(GameRuleRegistrar.RANDOM_ITEM_DROPS).get()
            );
            Long itemSeed = switch (randomItemDropRule) {
                case DISABLED -> -1L;
                case ENABLED_RANDOM, ENABLED_SAME_RARITY -> random.nextLong(1, Long.MAX_VALUE);
                case ENABLED_SAME_ITEMS -> ItemServerEvents.ITEM_DROPS_SEED;
            };
            rtsPlayers.add(RTSPlayer.getNewPlayer(
                    serverPlayer.getName().getString(),
                    faction,
                    serverPlayer.getId(),
                    startPosColorId,
                    isDogPerson,
                    itemSeed,
                    randomItemDropRule == RandomItemDropRule.ENABLED_SAME_RARITY ||
                    randomItemDropRule == RandomItemDropRule.ENABLED_SAME_ITEMS ?
                        ItemServerEvents.ITEM_RARITY_SEED : -1L
            ));
            FogOfWarServerEvents.invalidateRtsCache();
            String playerName = serverPlayer.getName().getString();
            ResourcesServerEvents.assignResources(playerName);
            PlayerClientboundPacket.addRTSPlayer(playerName, faction, (long) serverPlayer.getId(), startPosColorId, isDogPerson, false);
            HeroServerEvents.fallenHeroes.removeIf(hero -> hero.ownerName.equals(playerName));

            ServerLevel level = (ServerLevel) serverPlayer.level();
            List<Entity> startingUnits = createStartingUnits(level, playerName, faction, isDogPerson, pos);
            if (!readiedStart)
                startingUnits.forEach(level::addFreshEntity);

            if (faction != Factions.NONE) {
                if (SurvivalServerEvents.isEnabled()) {
                    level.setDayTime(TimeUtils.DAWN + getWaveSurvivalTimeModifier(SurvivalServerEvents.getDifficulty()));
                    for (RTSPlayer rtsPlayer : rtsPlayers)
                        if (!rtsPlayer.name.equals(playerName))
                            AlliancesServerEvents.addAlliance(rtsPlayer.name, playerName);
                } else {
                    level.setDayTime(MONSTER_START_TIME_OF_DAY);
                }
                ResearchServerEvents.removeAllCheatsFor(playerName);
            } else {
                enableAllCheats(playerName);
            }
            ResourcesServerEvents.resetResources(playerName, readiedStart);

            if (readiedStart) {
                if (placeStartingCapitol(level, playerName, faction, pos, startingUnits))
                    PlayerClientboundPacket.teleport(playerName, BlockPos.containing(pos));
                allyStartColour(playerName, startPosColorId);
            }

            boolean coopMode = serverLevel.getGameRules().getRule(GameRuleRegistrar.COOP_MODE).get();

            if (!TutorialServerEvents.isEnabled() && !readiedStart) {
                if (faction.equals(Factions.NONE))
                    sendMessageToAllPlayers("server.reignofnether.started_sandbox", true, playerName);
                else if (coopMode)
                    sendMessageToAllPlayers("server.reignofnether.started_ally", true, playerName);
                else
                    sendMessageToAllPlayers("server.reignofnether.started", true, playerName);
                sendMessageToAllPlayers("server.reignofnether.total_players", false, rtsPlayers.size());
            }
            if (coopMode)
                AlliancesServerEvents.applyCoopAlliances();


            PlayerClientboundPacket.syncRtsGameTime(rtsGameTicks);
            saveRTSPlayers();

            if (rtsPlayers.size() == 1)
                for (BuildingPlacement bpl : BuildingServerEvents.getBuildings())
                    if (bpl instanceof ItemShopPlacement isp)
                        isp.resetStock();
        }
    }

    // first RTS join into a fresh game: snapshot the playable area for late joiners
    private static void captureFreshGameFog(ServerLevel level) {
        if (!rtsPlayers.isEmpty())
            return;
        if (!FogChunkSnapshot.hasAny() && WorldBorderServerEvents.isRtsOptimisedMap(level))
            FogChunkSnapshot.captureFogChunks(level);
        FogOfWarServerEvents.captureNeutralFogUnits();
    }

    // the starting workers (first five) and, with fog on, the scout around pos; owned but not yet in the level
    private static List<Entity> createStartingUnits(ServerLevel level, String owner, Faction faction, boolean isDogPerson, Vec3 pos) {
        List<Entity> units = new ArrayList<>();
        EntityType<?> workerType = ForgeRegistries.ENTITY_TYPES.getValue(faction.workerEntityType);
        int x = (int) pos.x, z = (int) pos.z;
        for (BlockPos bp0 : List.of(new BlockPos(x, 0, z), new BlockPos(x + 1, 0, z), new BlockPos(x, 0, z + 1),
                new BlockPos(x - 1, 0, z), new BlockPos(x, 0, z - 1))) {
            Entity entity = workerType != null ? workerType.create(level) : null;
            if (entity instanceof Unit unit) {
                unit.setOwnerName(owner);
                entity.moveTo(MiscUtil.getHighestNonAirBlock(level, bp0).above().above(), 0, 0);
                units.add(entity);
            }
        }
        if (FogOfWarServerEvents.isEnabled()) {
            EntityType<?> scoutType = faction.equals(Factions.VILLAGERS) && !isDogPerson
                    ? EntityRegistrar.SCOUT_CAT_UNIT.get() : ForgeRegistries.ENTITY_TYPES.getValue(faction.scoutEntityType);
            Entity scout = scoutType != null ? scoutType.create(level) : null;
            if (scout instanceof Unit unit) {
                unit.setOwnerName(owner);
                BlockPos bp = MiscUtil.getHighestNonAirBlock(level, new BlockPos(x - 1, 0, z - 1)).above().above();
                scout.moveTo(isBat(scout) ? bp.offset(0, 10, 0) : bp, 0, 0);
                units.add(scout);
            }
        }
        return units;
    }

    private static boolean isBat(Entity entity) {
        return entity.getType() == EntityRegistrar.BAT_UNIT.get();
    }

    // readied start: the capitol's foundations at pos, built by the starting workers with the scout beside it.
    // False if the faction has no capitol
    private static boolean placeStartingCapitol(ServerLevel level, String owner, Faction faction, Vec3 pos, List<Entity> units) {
        Building building = ReignOfNetherRegistries.BUILDING.get(faction.capitolBuilding);
        if (building == null)
            return false;
        BlockPos bp = getBuildingOriginPos(new BlockPos((int) pos.x, (int) pos.y, (int) pos.z), building.getRelativeBlockData(level));
        EntityType<?> workerType = ForgeRegistries.ENTITY_TYPES.getValue(faction.workerEntityType);
        List<Integer> workerIds = new ArrayList<>();
        for (Entity unit : units) {
            boolean worker = unit.getType() == workerType;
            unit.moveTo(worker ? bp.offset(-1, 1, workerIds.size()) : bp.offset(0, isBat(unit) ? 10 : 1, 2), 0, 0);
            level.addFreshEntity(unit);
            if (worker)
                workerIds.add(unit.getId());
        }
        int[] ids = workerIds.stream().mapToInt(Integer::intValue).toArray();
        BuildingServerEvents.placeBuilding(building, bp, Rotation.NONE, owner, ids, false, false, true, true);
        return true;
    }

    // a readied start shares the start colour's team
    private static void allyStartColour(String name, int startPosColorId) {
        for (RTSPlayer rtsPlayer : rtsPlayers)
            if (!rtsPlayer.name.equals(name) && rtsPlayer.startPosColorId == startPosColorId)
                AlliancesServerEvents.addAlliance(rtsPlayer.name, name);
    }

    public static BlockPos getBuildingOriginPos(BlockPos bp, ArrayList<BuildingBlock> blocks) {
        Vec3i buildingDimensions = BuildingUtils.getBuildingSize(blocks);
        int xRadius = buildingDimensions.getX() / 2;
        int zRadius = buildingDimensions.getZ() / 2;
        return bp.offset(-xRadius, 0 , -zRadius);
    }

    public static void startRTSBot(String name, Vec3 pos, Faction faction) {
        synchronized (rtsPlayers) {
            ServerLevel level;
            if (players.isEmpty()) {
                return;
            } else {
                level = (ServerLevel) players.get(0).level();
            }

            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(faction.workerEntityType);
            RTSPlayer bot = RTSPlayer.getNewBot(name, faction);
            rtsPlayers.add(bot);
            FogOfWarServerEvents.invalidateRtsCache();
            ResourcesServerEvents.assignResources(bot.name);

            for (int i = -1; i <= 1; i++) {
                Entity entity = entityType != null ? entityType.create(level) : null;
                if (entity instanceof Unit unit) {
                    BlockPos bp = MiscUtil.getHighestNonAirBlock(level, new BlockPos((int) (pos.x + i), 0, (int) pos.z))
                        .above()
                        .above();
                    unit.setOwnerName(bot.name);
                    entity.moveTo(bp, 0, 0);
                    level.addFreshEntity(entity);
                }
            }
            if (faction.equals(Factions.MONSTERS)) {
                level.setDayTime(MONSTER_START_TIME_OF_DAY);
            }
            ResourcesServerEvents.resetResources(bot.name, false);

            if (!TutorialServerEvents.isEnabled()) {
                sendMessageToAllPlayers("server.reignofnether.bot_added", true, bot.name);
                sendMessageToAllPlayers("server.reignofnether.total_players", false, rtsPlayers.size());
            }
            saveRTSPlayers();
        }
    }

    // readied start for a bot from a start position: capitol foundations, workers, scout, client sync and
    // same-colour alliances, like startRTS does for a human. The faction must be one the bots can play (BotPlans)
    public static void startRTSBot(String name, Vec3 pos, Faction faction, int startPosColorId) {
        synchronized (rtsPlayers) {
            if (serverLevel == null || isRTSPlayer(name))
                return;
            ServerLevel level = serverLevel;
            captureFreshGameFog(level);

            RTSPlayer bot = RTSPlayer.getNewBot(name, faction);
            bot.startPosColorId = startPosColorId;
            rtsPlayers.add(bot);
            FogOfWarServerEvents.invalidateRtsCache();
            ResourcesServerEvents.assignResources(name);
            PlayerClientboundPacket.addRTSPlayer(name, faction, (long) bot.id, startPosColorId, bot.isDogPerson, false);
            HeroServerEvents.fallenHeroes.removeIf(hero -> hero.ownerName.equals(name));
            ResearchServerEvents.removeAllCheatsFor(name);
            ResourcesServerEvents.resetResources(name, true);
            // a bot-only match has no human start to set the clock
            if (SurvivalServerEvents.isEnabled())
                level.setDayTime(TimeUtils.DAWN + getWaveSurvivalTimeModifier(SurvivalServerEvents.getDifficulty()));
            else
                level.setDayTime(MONSTER_START_TIME_OF_DAY);

            if (!placeStartingCapitol(level, name, faction, pos, createStartingUnits(level, name, faction, bot.isDogPerson, pos)))
                return;
            allyStartColour(name, startPosColorId);

            PlayerClientboundPacket.syncRtsGameTime(rtsGameTicks);
            saveRTSPlayers();
        }
    }

    public static void startRTSScenario(int playerId, int roleIndex) {
        synchronized (rtsPlayers) {
            ServerPlayer serverPlayer = null;
            for (ServerPlayer player : players)
                if (player.getId() == playerId)
                    serverPlayer = player;

            ScenarioRole role = ScenarioUtils.getScenarioRole(false, roleIndex);

            if (serverPlayer == null || role == null) {
                return;
            }
            if (role.isNpc) {
                serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("sandbox.reignofnether.scenario.npc_role_error")));
            }
            if (rtsLocked) {
                serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("server.reignofnether.locked")));
                return;
            }
            if (isRTSPlayer(serverPlayer.getId())) {
                serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("server.reignofnether.already_started_scenario")));
                return;
            }
            for (RTSPlayer rtsPlayer : rtsPlayers) {
                if (rtsPlayer.scenarioRoleIndex == roleIndex) {
                    serverPlayer.sendSystemMessage(SystemMessage.error(Component.translatable("server.reignofnether.scenario_role_taken")));
                    return;
                }
            }
            RTSPlayer rtsPlayer = RTSPlayer.getNewScenarioPlayer(
                    serverPlayer.getName().getString(),
                    role.faction,
                    serverPlayer.getId(),
                    roleIndex
            );
            rtsPlayers.add(rtsPlayer);
            FogOfWarServerEvents.invalidateRtsCache();
            String playerName = serverPlayer.getName().getString();
            ResourcesServerEvents.assignScenarioResources(rtsPlayer);
            PlayerClientboundPacket.addRTSPlayer(playerName, role.faction, (long) serverPlayer.getId(), 0, true, false);

            for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                if (building.scenarioRoleIndex == roleIndex) {
                    building.ownerName = playerName;
                    BuildingClientboundPacket.syncBuilding(building.originPos, building.getBlocksPlaced(),
                            building.partialBlocksDestroyed, playerName, building.scenarioRoleIndex);
                }
            }
            for (LivingEntity le : UnitServerEvents.getAllUnits()) {
                if (le instanceof Unit unit && unit.getScenarioRoleIndex() == roleIndex) {
                    unit.setOwnerName(playerName);
                    UnitSyncClientboundPacket.sendSyncOwnerNamePacket(unit);
                }
            }

            sendMessageToAllPlayers("server.reignofnether.started_scenario", true, playerName, role.name, role.faction.getName());
            PlayerClientboundPacket.syncRtsGameTime(rtsGameTicks);

            // add NPC rtsPlayers
            int id = -1;
            for (ScenarioRole scenarioRole : ScenarioServerEvents.scenarioRoles) {
                int numUnits = ScenarioServerEvents.getNumScenarioUnits(scenarioRole);
                int numBuilds = ScenarioServerEvents.getNumScenarioBuildings(scenarioRole);
                if (!isRTSPlayer(scenarioRole.name) && (numUnits > 0 || numBuilds > 0) && scenarioRole.isNpc) {
                    RTSPlayer npcRtsPlayer = RTSPlayer.getNewScenarioPlayer(
                            scenarioRole.name,
                            scenarioRole.faction,
                            id,
                            scenarioRole.index
                    );
                    rtsPlayers.add(npcRtsPlayer);
                    FogOfWarServerEvents.invalidateRtsCache();
                    ResourcesServerEvents.assignScenarioResources(npcRtsPlayer);
                    PlayerClientboundPacket.addScenarioNPCRTSPlayer(scenarioRole.name, scenarioRole.faction, (long) id, scenarioRole.index);
                    id -= 1;
                }
            }
            if (serverLevel.getGameRules().getRule(GameRuleRegistrar.SCENARIO_MODE).get())
                AlliancesServerEvents.applyScenarioAlliances();
            if (serverLevel.getGameRules().getRule(GameRuleRegistrar.COOP_MODE).get())
                AlliancesServerEvents.applyCoopAlliances();
            saveRTSPlayers();
        }
    }

    @SubscribeEvent
    public static void onPlayerChat(ServerChatEvent evt) {
        if (evt.getPlayer().hasPermissions(4)) {
            String msg = evt.getMessage().getString();
            String[] words = msg.split(" ");
            String playerName = evt.getPlayer().getName().getString();

            if (words.length == 1 && words[0].equalsIgnoreCase("thebeastofcaerbannog")) {
                UnitServerEvents.spawnMob(EntityRegistrar.getEntityType("Killer Rabbit"), serverLevel, evt.getPlayer().getOnPos(), playerName);
                sendMessageToAllPlayers("server.reignofnether.used_cheat",false, playerName, words[0]);
            }

            if (words.length == 1 && words[0].equalsIgnoreCase("elitetaurenchieftain")) {
                for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                    if (entity instanceof HeroUnit heroUnit && ((Unit) heroUnit).getOwnerName().equals(playerName)) {
                        heroUnit.addExperience(10000);
                        heroUnit.setSkillPoints(10);
                        heroUnit.setMana(heroUnit.getMaxMana());
                        HeroClientboundPacket.setExperience(entity.getId(), heroUnit.getExperience());
                        HeroClientboundPacket.setSkillPoints(entity.getId(), 10);
                    }
                }
                sendMessageToAllPlayers("server.reignofnether.used_cheat",false, playerName, words[0]);
            }

            if (words.length == 2) {
                try {
                    if (words[0].equalsIgnoreCase("greedisgood")) {
                        int amount = Integer.parseInt(words[1]);
                        if (amount > 0) {
                            ResourcesServerEvents.addSubtractResources(new Resources(playerName,
                                    amount,
                                    amount,
                                    amount,
                                    amount
                            ));
                            evt.setCanceled(true);
                            sendMessageToAllPlayers("server.reignofnether.used_cheat_amount",
                                    false,
                                    playerName,
                                    words[0],
                                    Integer.toString(amount)
                            );
                        }
                    }
                } catch (NumberFormatException err) {
                    ReignOfNether.LOGGER.error(err);
                }
            }

            if (words.length == 3) {
                try {
                    if (words[0].equalsIgnoreCase("greedisgood")) {
                        int amount = Integer.parseInt(words[2]);
                        if (amount > 0 && List.of("food", "wood", "ore", "emerald").contains(words[1].toLowerCase())) {
                            switch (words[1].toLowerCase()) {
                                case "food" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, amount, 0, 0,0));
                                case "wood" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, amount, 0,0));
                                case "ore" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, 0, amount,0));
                                case "emerald" -> ResourcesServerEvents.addSubtractResources(new Resources(playerName, 0, 0,0, amount));
                            }
                            evt.setCanceled(true);
                            sendMessageToAllPlayers("server.reignofnether.used_cheat_amount",
                                    false,
                                    playerName,
                                    words[0] + " " + words[1],
                                    Integer.toString(amount)
                            );
                        }
                    }
                } catch (NumberFormatException err) {
                    ReignOfNether.LOGGER.error(err);
                }
            }

            for (String cheatName : singleWordCheats) {
                if (words.length == 1 && words[0].equalsIgnoreCase(cheatName)) {
                    if (ResearchServerEvents.playerHasCheat(playerName, cheatName)) {
                        ResearchServerEvents.removeCheat(playerName, cheatName);
                        ResearchClientboundPacket.removeCheat(playerName, cheatName);
                        evt.setCanceled(true);
                        sendMessageToAllPlayers("server.reignofnether.disabled_cheat", false, playerName, cheatName);
                    } else {
                        ResearchServerEvents.addCheat(playerName, cheatName);
                        ResearchClientboundPacket.addCheat(playerName, cheatName);
                        evt.setCanceled(true);
                        sendMessageToAllPlayers("server.reignofnether.enabled_cheat", false, playerName, cheatName);
                    }
                }
            }

            // apply all cheats - NOTE can cause concurrentModificationException clientside
            if (words.length == 1 && words[0].equalsIgnoreCase("allcheats") && (
                playerName.equalsIgnoreCase("solegendary") ||
                playerName.equalsIgnoreCase("altsolegendary"))
            ) {
                ResourcesServerEvents.addSubtractResources(new Resources(playerName, 99999, 99999, 99999));
                UnitServerEvents.maxPopulation = 99999;
                enableAllCheats(playerName);
                evt.setCanceled(true);
                sendMessageToAllPlayers("server.reignofnether.all_cheats", false, playerName);
            }
        }
    }

    public static void enableAllCheats(String playerName) {
        for (String cheatName : singleWordCheats) {
            ResearchServerEvents.addCheat(playerName, cheatName);
            ResearchClientboundPacket.addCheat(playerName, cheatName);
        }
    }

    public static void enableOrthoview(int id) {
        ServerPlayer player = getPlayerById(id);
        if (player != null && PlayerRoles.roleOf(player.getName().getString()) == PlayerRole.SOLDIER)
            return; // soldiers fight in first person, alive or dead
        if (player != null)
            player.removeAllEffects();

        orthoviewPlayers.removeIf(p -> p.getId() == id);
        orthoviewPlayers.add(player);
    }

    public static void disableOrthoview(int id) {
        ServerPlayer leaving = getPlayerById(id);
        orthoviewPlayers.removeIf(p -> p.getId() == id);
        if (leaving != null) {
            // drop fog bookkeeping so normal view-distance behavior resumes
            com.solegendary.reignofnether.fogofwar.FogOfWarServerEvents.onPlayerExitOrthoview(leaving);
        }
    }

    private static ServerPlayer getPlayerById(int playerId) {
        for (ServerPlayer player : players) {
            if (playerId == player.getId()) {
                return player;
            }
        }
        return null;
    }

    public static void openTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);

        // mirrors OrthoviewClientEvents.tryToToggleEnable, survival/adventure players can't use the RTS view
        if (serverPlayer != null && (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL ||
                serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE)) {
            ReignOfNether.LOGGER.warn("Refused to open topdown GUI for survival/adventure player {}", serverPlayer.getName().getString());
            return;
        }
        if (serverPlayer != null && PlayerRoles.roleOf(serverPlayer.getName().getString()) == PlayerRole.SOLDIER) {
            return;
        } else if (serverPlayer != null) {
            // Open GUI server-side
            MenuConstructor provider = TopdownGuiContainer.getServerContainerProvider();
            MenuProvider namedProvider = new SimpleMenuProvider(provider, TopdownGuiContainer.TITLE);
            NetworkHooks.openScreen(serverPlayer, namedProvider);

            // Save original game mode only if it's not already saved for this session
            String playerName = serverPlayer.getName().getString();
            playerDefaultGameModes.putIfAbsent(playerName, serverPlayer.gameMode.getGameModeForPlayer());

            // Mark that this player has the GUI open
            playerGuiOpenStatus.put(playerName, true);

            // Set game mode to SPECTATOR for GUI interaction
            serverPlayer.setGameMode(GameType.SPECTATOR);
        } else {
            ReignOfNether.LOGGER.warn("serverPlayer is null, cannot open topdown GUI");
        }
    }

    public static void closeTopdownGui(int playerId) {
        ServerPlayer serverPlayer = getPlayerById(playerId);

        if (serverPlayer != null) {
            String playerName = serverPlayer.getName().getString();

            // Ensure player had GUI open before attempting to close
            if (Boolean.TRUE.equals(playerGuiOpenStatus.get(playerName))) {
                // Restore the player’s original game mode if saved
                GameType originalGameType = playerDefaultGameModes.remove(playerName);

                if (originalGameType != null) {
                    serverPlayer.setGameMode(originalGameType);
                } else {
                    ReignOfNether.LOGGER.warn("No original game mode found for player {}", playerName);
                }

                if (SandboxServer.isSandboxPlayer(playerName)) {
                    serverPlayer.setGameMode(GameType.CREATIVE);
                }

                // Mark that the GUI is now closed
                playerGuiOpenStatus.remove(playerName);
            } else {
                ReignOfNether.LOGGER.warn("Attempted to close GUI for player {} who didn't have it open", playerName);
            }
        } else {
            ReignOfNether.LOGGER.warn("serverPlayer is null, cannot close topdown GUI");
        }
    }

    public static void movePlayer(int playerId, double x, double y, double z) {
        ServerPlayer serverPlayer = getPlayerById(playerId);
        if (serverPlayer != null && (serverPlayer.isCreative() || serverPlayer.isSpectator())) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
                return;
            boolean isOrtho = Boolean.TRUE.equals(playerGuiOpenStatus.get(serverPlayer.getName().getString()));
            for (ServerPlayer op : orthoviewPlayers)
                if (op.getId() == playerId) { isOrtho = true; break; }
            if (!isOrtho)
                return;
            net.minecraft.world.level.border.WorldBorder border = serverPlayer.level().getWorldBorder();
            double margin = 1.0D;
            double minX = border.getMinX() + margin;
            double maxX = border.getMaxX() - margin;
            double minZ = border.getMinZ() + margin;
            double maxZ = border.getMaxZ() - margin;
            if (maxX > minX && maxZ > minZ) {
                x = net.minecraft.util.Mth.clamp(x, minX, maxX);
                z = net.minecraft.util.Mth.clamp(z, minZ, maxZ);
            } else {
                x = border.getCenterX();
                z = border.getCenterZ();
            }
            // headroom above build height since the orthoview camera sits up to 100 blocks over the terrain
            y = net.minecraft.util.Mth.clamp(y, serverPlayer.level().getMinBuildHeight(), serverPlayer.level().getMaxBuildHeight() + 100);
            serverPlayer.teleportTo(x, y, z);
        }
    }

    public static void sendMessageToAllPlayers(String msg) {
        sendMessageToAllPlayers(msg, false);
    }

    // important lines get the ▲ marker (see SystemMessage); colour tints the text, 0xFFFFFF leaves the default
    public static void sendMessageToAllPlayers(String msg, int color, boolean important, Object... formatArgs) {
        MutableComponent text = Component.translatable(msg, formatArgs);
        if (color != 0xFFFFFF)
            text.withStyle(s -> s.withColor(color));
        Component line = important ? SystemMessage.important(text) : SystemMessage.info(text);
        for (ServerPlayer player : players)
            player.sendSystemMessage(line);
    }

    public static void sendMessageToAllPlayers(String msg, boolean important, Object... formatArgs) {
        sendMessageToAllPlayers(msg, 0xFFFFFF, important, formatArgs);
    }

    public static void sendMessageToPlayer(String playerName, String msg) {
        sendMessageToPlayer(playerName, msg, false);
    }

    public static void sendMessageToPlayer(String playerName, String msg, boolean important, Object... formatArgs) {
        Component text = Component.translatable(msg, formatArgs);
        for (ServerPlayer player : players) {
            if (player.getName().getString().equals(playerName)) {
                player.sendSystemMessage(important ? SystemMessage.important(text) : SystemMessage.info(text));
                return;
            }
        }
    }

    // defeat a player, giving them a defeat screen, removing all their unit/building control and removing them from
    // rtsPlayers
    public static void defeat(int playerId, String reason) {
        for (ServerPlayer player : players) {
            if (player.getId() == playerId) {
                defeat(player.getName().getString(), reason);
                return;
            }
        }
    }

    public static void defeat(String playerName, String reason) {
        // a co-commander surrendering only leaves; their lead's faction plays on
        if (!PlayerRoles.ownerOf(playerName).equals(playerName)) {
            PlayerRoles.remove(playerName);
            ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayerByName(playerName);
            if (sp != null)
                SoldierServerEvents.toSpectator(sp);
            PlayerClientboundPacket.defeat(playerName);
            return;
        }
        if (SandboxServer.isSandboxPlayer(playerName))
            return;

        boolean playerExists = false;
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            if (rtsPlayer.name.equals(playerName)) {
                playerExists = true;
                break;
            }
        }
        if (!playerExists)
            return;

        ReignOfNether.LOGGER.info("[Player] defeat: playerName={}, reason={}", playerName, reason);

        synchronized (rtsPlayers) {
            // Remove the defeated player from the list
            FogOfWarServerEvents.invalidateRtsCache();

            TaskSchedulerServerEvents.schedule(60, () -> {
                ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayerByName(playerName);
                if (sp != null)
                    FogOfWarServerEvents.resendAllTrackedChunks(sp);
            });

            rtsPlayers.removeIf(rtsPlayer -> {
                if (rtsPlayer.name.equals(playerName)) {
                    sendMessageToAllPlayers("server.reignofnether.is_defeated", true, playerName, reason);
                    sendMessageToAllPlayers("server.reignofnether.players_remaining", false, (rtsPlayers.size() - 1));

                    postGameRtsPlayers.add(rtsPlayer);

                    PlayerClientboundPacket.defeat(playerName);

                    // Remove ownership from all units and buildings of the defeated player
                    for (LivingEntity entity : UnitServerEvents.getAllUnits()) {
                        if (entity instanceof Unit unit && unit.getOwnerName().equals(playerName)) {
                            unit.resetBehaviours();
                            Unit.resetBehaviours(unit);
                            if (unit instanceof AttackerUnit aUnit)
                                AttackerUnit.resetBehaviours(aUnit);
                            if (unit instanceof WorkerUnit wUnit)
                                WorkerUnit.resetBehaviours(wUnit);
                            unit.setOwnerName("");
                        }
                    }
                    for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                        if (building.ownerName.equals(playerName)) {
                            if (building instanceof ProductionPlacement productionBuilding)
                                productionBuilding.productionQueue.clear();
                            building.ownerName = "";
                        }
                    }
                    return true;
                }
                return false;
            });

            // co-commanders and soldiers go down with their faction
            for (String dropped : PlayerRoles.dropFaction(playerName)) {
                ServerPlayer sp = serverLevel.getServer().getPlayerList().getPlayerByName(dropped);
                if (sp != null)
                    SoldierServerEvents.toSpectator(sp);
                PlayerClientboundPacket.defeat(dropped);
            }

            // Remove research data and resources associated with the defeated player
            saveRTSPlayers();
            ResearchServerEvents.removeAllResearchFor(playerName);
            ResearchServerEvents.syncResearch(playerName);
            ResearchServerEvents.saveResearch();
            ResearchServerEvents.removeAllCheatsFor(playerName);
            ResourcesServerEvents.resourcesList.removeIf(rl -> rl.ownerName.equals(playerName));

            // Check if only allied players are left or if a single player remains
            if (!serverLevel.getServer().getGameRules().getRule(GameRuleRegistrar.COOP_MODE).get()) {
                if (rtsPlayers.size() > 1) {
                    // Get the set of remaining player names
                    Set<String> remainingPlayers = new HashSet<>();
                    for (RTSPlayer player : rtsPlayers) {
                        String name = player.name;
                        remainingPlayers.add(name);
                    }

                    // Use the first remaining player as a reference to find all connected allies
                    String referencePlayer = remainingPlayers.iterator().next();
                    Set<String> factionGroup = AlliancesServerEvents.getAllConnectedAllies(referencePlayer);

                    // Check if all remaining players are part of the same alliance group
                    if (remainingPlayers.equals(factionGroup)) {
                        // Declare victory for all players in the faction group
                        for (String winner : remainingPlayers) {
                            postGameRtsPlayers.add(getRTSPlayer(winner));
                            sendMessageToAllPlayers("server.reignofnether.victory_alliance", true, winner);
                            PlayerClientboundPacket.victory(winner);
                        }
                        broadcastMatchStats(remainingPlayers);
                    }
                } else if (rtsPlayers.size() == 1) {
                    // Single remaining player - declare victory
                    RTSPlayer winner = rtsPlayers.get(0);
                    postGameRtsPlayers.add(winner);
                    sendMessageToAllPlayers("server.reignofnether.victorious", true, winner.name);
                    PlayerClientboundPacket.victory(winner.name);
                    broadcastMatchStats(Set.of(winner.name));
                }
            }
        }
    }

    public static void beaconVictory(String playerName) {
        ReignOfNether.LOGGER.info("[Player] beaconVictory: playerName={}", playerName);
        if (SurvivalServerEvents.isEnabled()) {
            try {
                if (AlliancesServerEvents.getAllAllies(playerName).isEmpty())
                    sendMessageToAllPlayers("server.reignofnether.victorious", true, playerName);
                else
                    sendMessageToAllPlayers("server.reignofnether.victory_alliance", true, playerName);
                PlayerClientboundPacket.victory(playerName);
                for (String allyName : AlliancesServerEvents.getAllAllies(playerName))
                    PlayerClientboundPacket.victory(allyName);
                SurvivalServerEvents.endCurrentWave();
                Set<String> winners = new HashSet<>(AlliancesServerEvents.getAllAllies(playerName));
                winners.add(playerName);
                broadcastMatchStats(winners);
            } catch (ConcurrentModificationException e) {
                ReignOfNether.LOGGER.error("ConcurrentModificationException during beaconVictory", e);
            }
        } else {
            for (RTSPlayer p : rtsPlayers) {
                String n = p.name;
                if (AlliancesServerEvents.isAllied(playerName, n) || n.equals(playerName)) continue;
                defeat(n, Component.translatable("server.reignofnether.beacon_defeat").getString());
            }
        }
    }

    // Sends the final per-player scoreboard to all clients so they can show the
    // end-of-match stats popup (MatchEndScreen). Winners are those in winnerNames;
    // everyone else (already moved to postGameRtsPlayers on defeat) is a loser.
    public static void broadcastMatchStats(Set<String> winnerNames) {
        LinkedHashMap<String, RTSPlayer> byName = new LinkedHashMap<>();
        synchronized (rtsPlayers) {
            for (RTSPlayer p : rtsPlayers)
                if (p != null) byName.putIfAbsent(p.name, p);
        }
        synchronized (postGameRtsPlayers) {
            for (RTSPlayer p : postGameRtsPlayers)
                if (p != null) byName.putIfAbsent(p.name, p);
        }
        List<MatchStatsClientboundPacket.MatchStatRow> rows = new ArrayList<>();
        for (RTSPlayer p : byName.values())
            rows.add(new MatchStatsClientboundPacket.MatchStatRow(
                    p.name, p.faction, winnerNames.contains(p.name), p.startPosColorId,
                    p.scores.getScoreListAsArray(), p.armyTimeline.stream().mapToInt(Integer::intValue).toArray()));
        MatchStatsClientboundPacket.broadcast(rtsGameTicks, rows);
        ReplayServerEvents.onMatchEnd(winnerNames);
    }

    public static String getBeaconWinTime(String playerName) {
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            if (rtsPlayer.name.equals(playerName)) {
                return TimeUtils.getTimeStrFromTicks(Beacon.getTicksToWin(serverLevel) - rtsPlayer.beaconOwnerTicks);
            }
        }
        return TimeUtils.getTimeStrFromTicks(Beacon.getTicksToWin(serverLevel));
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        AllyCommand.register(evt.getDispatcher());
        CoCommanderCommand.register(evt.getDispatcher());
        RTSPlayerScoresCommand.register(evt.getDispatcher());

        evt.getDispatcher().register(Commands.literal("rts-lock").then(Commands.literal("enable").executes((command) -> {
            if ((command.getSource() != null &&
                command.getSource().getPlayer() != null &&
                command.getSource().getPlayer().hasPermissions(4)) ||
                (command.getSource() != null &&
                !command.getSource().isPlayer())) {
                setRTSLock(true);
                return 1;
            }
            return 0;
        })));

        evt.getDispatcher().register(Commands.literal("rts-lock").then(Commands.literal("disable").executes((command) -> {
            if ((command.getSource() != null &&
                command.getSource().getPlayer() != null &&
                command.getSource().getPlayer().hasPermissions(4)) ||
                (command.getSource() != null &&
                !command.getSource().isPlayer())) {
                setRTSLock(false);
                return 1;
            }
            return 0;
        })));
    }

    public static int resetRTS(boolean hardReset) {
        ReignOfNether.LOGGER.info("[Player] resetRTS: hardReset={}", hardReset);
        StartPosServerEvents.cancelStartGameCountdown(true);
        FogChunkSnapshot.clear();

        boolean isSandboxOrScenario = SandboxServer.isAnyoneASandboxPlayer() || serverLevel.getGameRules().getRule(GameRuleRegistrar.SCENARIO_MODE).get();

        synchronized (rtsPlayers) {
            rtsPlayers.clear();
            PlayerRoles.clear();
            SoldierServerEvents.reset();

            for (LivingEntity entity : new ArrayList<>(UnitServerEvents.getAllUnits()))
                if (hardReset || (entity instanceof Unit unit && !Unit.hasAnchor(unit) && !isSandboxOrScenario))
                    entity.kill();

            if (!isSandboxOrScenario)
                UnitServerEvents.getAllUnits().removeIf(u -> (hardReset || (u instanceof Unit unit && !Unit.hasAnchor(unit))));

            if (!isSandboxOrScenario)
                for (LivingEntity entity : UnitServerEvents.getAllUnits())
                    if (entity instanceof Unit unit)
                        unit.setOwnerName("");

            for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                if (building instanceof ProductionPlacement productionBuilding)
                    productionBuilding.productionQueue.clear();
                if ((building.getBuilding().shouldDestroyOnReset || hardReset) && !isSandboxOrScenario)
                    building.destroy((ServerLevel) building.getLevel());
            }
            if (!isSandboxOrScenario)
                BuildingServerEvents.getBuildings().removeIf(b -> b.getBuilding().shouldDestroyOnReset || hardReset);

            if (!isSandboxOrScenario)
                for (BuildingPlacement building : BuildingServerEvents.getBuildings())
                    building.ownerName = "";

            ResearchServerEvents.removeAllResearch();
            ResearchServerEvents.removeAllCheats();
            PlayerClientboundPacket.resetRTS(hardReset);
            if (!TutorialServerEvents.isEnabled()) {
                if (hardReset)
                    sendMessageToAllPlayers("server.reignofnether.match_reset_hard", true);
                else
                    sendMessageToAllPlayers("server.reignofnether.match_reset", true);
            }
            ResourcesServerEvents.resourcesList.clear();
            saveAll();

            if (rtsLocked)
                setRTSLock(false);
            AlliancesServerEvents.resetAllAlliances();
            SurvivalServerEvents.reset();
        }
        HeroServerEvents.fallenHeroes.clear();
        UnitServerEvents.saveFallenHeroUnits(serverLevel);

        for (ServerPlayer player : serverLevel.players())
            player.setGameMode(GameType.SPECTATOR);

        playerDefaultGameModes.replaceAll((key, oldValue) -> GameType.SPECTATOR);
        AlliancesServerEvents.playersWithAlliedControl.clear();
        TimeServerEvents.resetBloodMoon();

        for (BuildingPlacement bpl : BuildingServerEvents.getBuildings())
            if (bpl instanceof CustomBuildingPlacement cbpl)
                cbpl.resetAllCommands();

        return 1;
    }

    public static void publishScenarioMap() {
        if (ScenarioServerEvents.getNumScenarioUnits() == 0 && ScenarioServerEvents.getNumScenarioBuildings() == 0) {
            sendMessageToAllPlayers("server.reignofnether.scenario_published_error1");
            return;
        }

        for (LivingEntity le : UnitServerEvents.getAllUnits()) {
            if (le instanceof Unit unit) {
                ScenarioRole role = ScenarioUtils.getScenarioRole(false, unit.getScenarioRoleIndex());
                unit.setOwnerName(role != null ? role.name : "");
                UnitSyncClientboundPacket.sendSyncScenarioRoleIndexPacket(unit);
            }
        }
        for (BuildingPlacement bpl : BuildingServerEvents.getBuildings()) {
            ScenarioRole role = ScenarioUtils.getScenarioRole(false, bpl.scenarioRoleIndex);
            bpl.ownerName = role != null ? role.name : "";
            BuildingClientboundPacket.syncBuilding(bpl.originPos, bpl.getBlocksPlaced(), bpl.partialBlocksDestroyed, bpl.ownerName, bpl.scenarioRoleIndex);
        }
        serverLevel.getGameRules().getRule(GameRuleRegistrar.SCENARIO_MODE).set(true, serverLevel.getServer());

        synchronized (rtsPlayers) {
            rtsPlayers.clear();
            PlayerRoles.clear();
            SoldierServerEvents.reset();

            for (BuildingPlacement building : BuildingServerEvents.getBuildings()) {
                if (building instanceof ProductionPlacement productionBuilding)
                    productionBuilding.productionQueue.clear();
            }
            ResearchServerEvents.removeAllResearch();
            ResearchServerEvents.removeAllCheats();
            PlayerClientboundPacket.publishScenarioMap();
            sendMessageToAllPlayers("server.reignofnether.scenario_published", true);
            sendMessageToAllPlayers("server.reignofnether.scenario_published_tooltip1");
            sendMessageToAllPlayers("server.reignofnether.scenario_published_tooltip2");
            ResourcesServerEvents.resourcesList.clear();
            saveAll();

            if (rtsLocked)
                setRTSLock(false);
            if (serverLevel.getGameRules().getRule(GameRuleRegistrar.SCENARIO_MODE).get())
                AlliancesServerEvents.applyScenarioAlliances();
            if (serverLevel.getGameRules().getRule(GameRuleRegistrar.COOP_MODE).get())
                AlliancesServerEvents.applyCoopAlliances();
            SurvivalServerEvents.reset();
        }
        HeroServerEvents.fallenHeroes.clear();
        UnitServerEvents.saveFallenHeroUnits(serverLevel);

        for (ServerPlayer player : serverLevel.players())
            player.setGameMode(GameType.SPECTATOR);

        playerDefaultGameModes.replaceAll((key, oldValue) -> GameType.SPECTATOR);
        AlliancesServerEvents.playersWithAlliedControl.clear();
        TimeServerEvents.resetBloodMoon();

        GameruleClientboundPacket.setScenarioMode(true);
        GameModeClientboundPacket.setAndLockAllClientGameModes(GameMode.SCENARIO);
    }

    private static void saveAll() {
        saveRTSPlayers();
        postGameRtsPlayers.clear();
        saveBuildings(serverLevel);
        BuildingServerEvents.saveNetherZones(serverLevel);
        UnitServerEvents.saveGatherTargets(serverLevel);
        ResourcesServerEvents.saveResources(serverLevel);
        ResearchServerEvents.saveResearch();
    }

    public static void setRTSLock(boolean lock) {
        setRTSLock(lock, false);
    }

    public static void setRTSLock(boolean lock, boolean noMsg) {
        rtsLocked = lock;
        serverLevel.players().forEach(p -> {
            if (rtsLocked) {
                PlayerClientboundPacket.lockRTS(p.getName().getString());
            } else {
                PlayerClientboundPacket.unlockRTS(p.getName().getString());
            }
        });
        if (!noMsg) {
            if (rtsLocked) {
                sendMessageToAllPlayers("server.reignofnether.match_locked");
            } else {
                sendMessageToAllPlayers("server.reignofnether.match_unlocked");
            }
        }
    }

    public static void setRTSSyncingEnabled(boolean enable) {
        rtsSyncingEnabled = enable;
        if (rtsSyncingEnabled) {
            sendMessageToAllPlayers("server.reignofnether.sync_enabled");
        } else {
            sendMessageToAllPlayers("server.reignofnether.sync_disabled");
        }
    }

    public static void updateMarketRates(String playerName) {
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            if (rtsPlayer.name.equals(playerName)) {
                for (TradeAction tradeAction : rtsPlayer.tradeRates.keySet()) {
                    PlayerClientboundPacket.setMarketRate(tradeAction, rtsPlayer.name, rtsPlayer.tradeRates.get(tradeAction));
                }
                return;
            }
        }
    }

    public static void updateMarketRates(int playerId) {
        for (RTSPlayer rtsPlayer : rtsPlayers) {
            if (rtsPlayer.id == playerId) {
                for (TradeAction tradeAction : rtsPlayer.tradeRates.keySet()) {
                    PlayerClientboundPacket.setMarketRate(tradeAction, rtsPlayer.name, rtsPlayer.tradeRates.get(tradeAction));
                }
                return;
            }
        }
    }
}
