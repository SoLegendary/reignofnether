package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.building.buildings.placements.PortalPlacement;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.sounds.SoundAction;
import com.solegendary.reignofnether.sounds.SoundClientboundPacket;
import com.solegendary.reignofnether.time.TimeUtils;
import com.solegendary.reignofnether.tutorial.TutorialServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.solegendary.reignofnether.time.TimeUtils.getWaveSurvivalTimeModifier;

public class SurvivalServerEvents {

    private static boolean isEnabled = false;
    @Nullable public static Wave currentWave = null;
    public static Wave nextWave = Wave.getWave(1);
    private static WaveDifficulty difficulty = WaveDifficulty.BEGINNER;
    private static final ArrayList<WaveEnemy> enemies = new ArrayList<>();
    public static final String ENEMY_OWNER_NAME = "Enemy";
    private static final Random random = new Random();
    public static Faction lastFaction = Faction.NONE;

    public static final ArrayList<WavePortal> portals = new ArrayList<>();

    public static final long TICK_INTERVAL = 10;
    private static long lastTime = -1;
    private static long lastEnemyCount = 0;
    private static long ticks = 0;

    private static ArrayList<BuildingPlacement> lastPortals = new ArrayList<>();

    private static ServerLevel serverLevel = null;

    public static WaveDifficulty getDifficulty() {
        return difficulty;
    }

    public static void saveData(ServerLevel level) {
        SurvivalSaveData survivalData = SurvivalSaveData.getInstance(level);
        survivalData.isEnabled = isEnabled;
        survivalData.waveNumber = nextWave.number;
        survivalData.difficulty = difficulty;
        survivalData.randomSeed = Wave.randomSeed;
        survivalData.save();
        level.getDataStorage().save();
        //ReignOfNether.LOGGER.info("saved survival data in serverevents");
    }

    @SubscribeEvent
    public static void loadWaveData(ServerStartedEvent evt) {
        ServerLevel level = evt.getServer().getLevel(Level.OVERWORLD);
        if (level != null) {
            SurvivalSaveData survivalData = SurvivalSaveData.getInstance(level);
            isEnabled = survivalData.isEnabled;
            nextWave = Wave.getWave(survivalData.waveNumber);
            Wave.randomSeed = survivalData.randomSeed;
            Wave.reseedWaves();
            difficulty = survivalData.difficulty;

            if (isEnabled()) {
                SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
                SurvivalClientboundPacket.setWaveNumber(nextWave.number);
            }
            ReignOfNether.LOGGER.info("loaded survival data: isEnabled: " + isEnabled());
            ReignOfNether.LOGGER.info("loaded survival data: nextWave: " + nextWave.number);
            ReignOfNether.LOGGER.info("loaded survival data: difficulty: " + difficulty);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END || evt.level.isClientSide() || evt.level.dimension() != Level.OVERWORLD)
            return;

        serverLevel = (ServerLevel) evt.level;

        if (!isEnabled())
            return;

        ticks += 1;
        if (ticks % TICK_INTERVAL != 0)
            return;

        long time = evt.level.getDayTime();
        long normTime = TimeUtils.normaliseTime(evt.level.getDayTime());

        if (!isStarted()) {
            setToGameStartTime();
            return;
        }

        if (lastTime >= 0) {
            if (lastTime <= TimeUtils.DUSK - 600 && normTime > TimeUtils.DUSK - 600) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dusksoon", true);
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.RANDOM_CAVE_AMBIENCE);
            }
            if (lastTime <= TimeUtils.DUSK && normTime > TimeUtils.DUSK) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dusk", true);
                SoundClientboundPacket.playSoundForAllPlayers(SoundAction.RANDOM_CAVE_AMBIENCE);
                setToStartingNightTime();
            }
            if (lastTime <= TimeUtils.DUSK + getWaveSurvivalTimeModifier(difficulty) + 50 &&
                    normTime > TimeUtils.DUSK + getWaveSurvivalTimeModifier(difficulty) + 50) {
                startNextWave((ServerLevel) evt.level);
            }
            if (lastTime <= TimeUtils.DAWN && normTime > TimeUtils.DAWN) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.dawn", true);
                setToStartingDayTime();
            }
        }

        int enemyCount = getCurrentEnemies().size() + portals.size();
        if (enemyCount < lastEnemyCount && enemyCount <= 3) {
            if (enemyCount == 0)
                waveCleared((ServerLevel) evt.level);
            else if (enemyCount == 1) {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.remaining_enemies_one");
            } else {
                PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.remaining_enemies", false, enemyCount);
            }
        }
        for (WaveEnemy enemy : enemies)
            enemy.tick(TICK_INTERVAL);

        // detect new portals and update portals list accordingly
        List<BuildingPlacement> currentPortals = new ArrayList<>();
        for (BuildingPlacement b : BuildingServerEvents.getBuildings()) {
            if (ENEMY_OWNER_NAME.equals(b.ownerName) && b instanceof PortalPlacement) {
                currentPortals.add(b);
                if (!lastPortals.contains(b))
                    SurvivalServerEvents.portals.add(new WavePortal((PortalPlacement) b, currentWave != null ? currentWave : nextWave));
            }
        }

        SurvivalServerEvents.portals.removeIf(p -> !currentPortals.contains(p.getPortal()));

        lastPortals.clear();
        lastPortals.addAll(currentPortals);

        for (WavePortal portal : portals)
            portal.tick(TICK_INTERVAL);

        lastTime = normTime;
        lastEnemyCount = enemyCount;
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(Commands.literal("debug-end-wave")
                .executes((command) -> {
                    if (command.getSource().getPlayer() != null) {
                        String name = command.getSource().getPlayer().getName().getString();
                        if (!ResearchServerEvents.playerHasCheat(name, "thereisnospoon"))
                            return 0;
                    }
                    return endCurrentWave();
                }));
        evt.getDispatcher().register(Commands.literal("debug-next-night")
                .executes((command) -> {
                    if (command.getSource().getPlayer() != null) {
                        String name = command.getSource().getPlayer().getName().getString();
                        if (!ResearchServerEvents.playerHasCheat(name, "thereisnospoon"))
                            return 0;
                    }
                    if (!isEnabled())
                        return 0;
                    PlayerServerEvents.sendMessageToAllPlayers("Advancing to next night and wave");
                    serverLevel.setDayTime(12450);
                    return 1;
                }));
    }

    public static int endCurrentWave() {
        if (!isEnabled())
            return 0;
        PlayerServerEvents.sendMessageToAllPlayers("Ending current wave");
        ArrayList<WaveEnemy> enemiesCopy = new ArrayList<>(enemies);
        for (WaveEnemy enemy : enemiesCopy)
            enemy.getEntity().kill();
        ArrayList<WavePortal> portalsCopy = new ArrayList<>(portals);
        for (WavePortal portal : portalsCopy)
            portal.portal.destroy(serverLevel);
        return 1;
    }

    public static void enable(WaveDifficulty diff) {
        if (TutorialServerEvents.isEnabled())
            return;
        if (!isEnabled()) {
            reset();
            lastEnemyCount = 0;
            difficulty = diff;
            isEnabled = true;
            SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
            if (serverLevel != null)
                saveData(serverLevel);
        }
    }

    public static void reset() {
        for (WaveEnemy enemy : enemies)
            enemy.getEntity().kill();
        ArrayList<WavePortal> portalsCopy = new ArrayList<>(portals);
        for (WavePortal portal : portalsCopy)
            portal.portal.destroy(serverLevel);
        difficulty = WaveDifficulty.EASY;
        isEnabled = false;
        portals.clear();
        enemies.clear();
        Wave.randomSeed = System.currentTimeMillis();
        Wave.reseedWaves();
        nextWave = Wave.getWave(1);
        SurvivalClientboundPacket.setWaveRandomSeed(Wave.randomSeed);
        lastTime = -1;
        if (serverLevel != null)
            saveData(serverLevel);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (isEnabled()) {
            SurvivalClientboundPacket.enableAndSetDifficulty(difficulty);
            SurvivalClientboundPacket.setWaveNumber(nextWave.number);
            SurvivalClientboundPacket.setWaveRandomSeed(Wave.randomSeed);
            for (RTSPlayer rtsPlayer : PlayerServerEvents.rtsPlayers)
                AlliancesServerEvents.addAlliance(rtsPlayer.name, evt.getEntity().getName().getString());
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent evt) {
        if (evt.getEntity() instanceof Unit unit &&
                evt.getEntity() instanceof LivingEntity entity &&
                !evt.getLevel().isClientSide &&
                isEnabled() &&
                ENEMY_OWNER_NAME.equals(unit.getOwnerName())) {

            enemies.add(new WaveEnemy(unit));
        }
    }

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent evt) {
        if (evt.getEntity() instanceof Unit unit &&
                evt.getEntity() instanceof LivingEntity entity &&
                !evt.getLevel().isClientSide &&
                ENEMY_OWNER_NAME.equals(unit.getOwnerName())) {

            enemies.removeIf(e -> e.getEntity().getId() == entity.getId());
        }
    }



    public static long getDayLength() {
        return 12000 - getWaveSurvivalTimeModifier(difficulty);
    }

    public static void setToStartingDayTime() {
        serverLevel.setDayTime(TimeUtils.DAWN + getWaveSurvivalTimeModifier(difficulty));
    }

    public static void setToStartingNightTime() {
        serverLevel.setDayTime(TimeUtils.DUSK + getWaveSurvivalTimeModifier(difficulty));
    }

    public static void setToGameStartTime() {
        serverLevel.setDayTime(TimeUtils.DUSK + getWaveSurvivalTimeModifier(difficulty) + 60);
    }

    public static boolean isEnabled() { return isEnabled; }

    public static boolean isStarted() {
        for (RTSPlayer player : PlayerServerEvents.rtsPlayers)
            if (BuildingUtils.getTotalCompletedBuildingsOwned(false, player.name) > 0)
                return true;
        return false;
    }

    public static List<WaveEnemy> getCurrentEnemies() {
        return enemies;
    }

    public static int getTotalEnemyPopulation() {
        int pop = 0;
        for (WaveEnemy waveEnemy : getCurrentEnemies())
            pop += waveEnemy.unit.getCost().population;
        return pop;
    }

    public static boolean isWaveInProgress() {
        return !getCurrentEnemies().isEmpty();
    }

    // triggered at nightfall
    public static void startNextWave(ServerLevel level) {
        saveData(level);
        currentWave = nextWave;
        System.out.println("starting wave: " + nextWave.faction.name());
        nextWave.start(level);
        nextWave = Wave.getWave(nextWave.number + 1);
        System.out.println("next wave: " + nextWave.faction.name());
        SurvivalClientboundPacket.setWaveNumber(nextWave.number);
    }

    // triggered when last enemy is killed
    public static void waveCleared(ServerLevel level) {
        PlayerServerEvents.sendMessageToAllPlayers("survival.reignofnether.wave_cleared", true);
        SoundClientboundPacket.playSoundForAllPlayers(SoundAction.ALLY);
        currentWave = null;
    }

    public static void setWaveNumber(int waveNumber) {
        nextWave = Wave.getWave(waveNumber);
        SurvivalClientboundPacket.setWaveNumber(nextWave.number);
    }
}
