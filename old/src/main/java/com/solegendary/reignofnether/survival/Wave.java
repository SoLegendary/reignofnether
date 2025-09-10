package com.solegendary.reignofnether.survival;

import com.solegendary.reignofnether.util.Faction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.solegendary.reignofnether.survival.spawners.IllagerWaveSpawner.spawnIllagerWave;
import static com.solegendary.reignofnether.survival.spawners.MonsterWaveSpawner.spawnMonsterWave;
import static com.solegendary.reignofnether.survival.spawners.PiglinWaveSpawner.spawnPiglinWave;

public class Wave {

    public int number;
    public int population; // multiplied by number of players
    public int highestUnitTier;
    public Faction faction;

    public static long randomSeed = System.currentTimeMillis();

    public Wave(int number, int population, int highestUnitTier) {
        this.number = number;
        this.population = population;
        this.highestUnitTier = highestUnitTier;
        this.faction = factions.get(Mth.clamp(number - 1, 0, factions.size()));
    }

    public int getNumPortals() {
        return Math.max(1, 1 + number / 3);
    }

    public void start(ServerLevel level) {
        switch (faction) {
            case VILLAGERS -> spawnIllagerWave(level, this);
            case MONSTERS -> spawnMonsterWave(level, this);
            case PIGLINS -> spawnPiglinWave(level, this);
            case NONE -> {
                switch (new Random().nextInt(3)) {
                    case 0 -> spawnIllagerWave(level, this);
                    case 1 -> spawnMonsterWave(level, this);
                    case 2 -> spawnPiglinWave(level, this);
                }
            }
        }
    }

    public static Wave getWave(int number) {
        if (number <= 0)
            return waves.get(0);
        if (number > waves.size())
            return waves.get(waves.size() - 1);

        return waves.get(number - 1);
    }

    private static final ArrayList<Wave> waves = new ArrayList<>();

    private static final ArrayList<Faction> factions = new ArrayList<>();

    public static void reseedWaves() {
        Random random = new Random(randomSeed);
        factions.clear();

        Faction lastFaction = Faction.NONE;
        for (int i = 0; i < 30; i++) {
            Faction newFaction = Faction.NONE;
            switch (lastFaction) {
                case VILLAGERS -> {
                    if (random.nextBoolean())
                        newFaction = Faction.MONSTERS;
                    else
                        newFaction = Faction.PIGLINS;
                }
                case MONSTERS -> {
                    if (random.nextBoolean())
                        newFaction = Faction.VILLAGERS;
                    else
                        newFaction = Faction.PIGLINS;
                }
                case PIGLINS -> {
                    if (random.nextBoolean())
                        newFaction = Faction.MONSTERS;
                    else
                        newFaction = Faction.VILLAGERS;
                }
                case NONE -> {
                    switch (random.nextInt(3)) {
                        case 0 -> newFaction = Faction.MONSTERS;
                        case 1 -> newFaction = Faction.VILLAGERS;
                        case 2 -> newFaction = Faction.PIGLINS;
                    }
                }
            }
            factions.add(newFaction);
            lastFaction = newFaction;
        }

        waves.clear();
        waves.addAll(List.of(
                new Wave(1, 5, 1), // every tier increase, raise rate of population increase by +1
                new Wave(2, 10, 1),
                new Wave(3, 15, 1),
                new Wave(4, 21, 2),
                new Wave(5, 27, 2),
                new Wave(6, 33, 2),
                new Wave(7, 40, 3),
                new Wave(8, 47, 3),
                new Wave(9, 54, 3),
                new Wave(10, 62, 4),
                new Wave(11, 70, 4),
                new Wave(12, 78, 4),
                new Wave(13, 87, 5),
                new Wave(14, 96, 5),
                new Wave(15, 105, 5),
                new Wave(16, 115, 6),
                new Wave(17, 125, 6),
                new Wave(18, 135, 6), // after this wave, start raising population geometrically
                new Wave(19, 150, 6),
                new Wave(20, 170, 6),
                new Wave(21, 195, 6),
                new Wave(22, 225, 6),
                new Wave(23, 260, 6),
                new Wave(24, 295, 6),
                new Wave(25, 335, 6),
                new Wave(26, 380, 6),
                new Wave(27, 430, 6),
                new Wave(28, 485, 6),
                new Wave(29, 545, 6),
                new Wave(30, 610, 6)
        ));
    }

    static {
        reseedWaves();
    }
}
