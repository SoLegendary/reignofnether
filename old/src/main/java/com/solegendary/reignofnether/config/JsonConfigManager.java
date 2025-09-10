package com.solegendary.reignofnether.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JsonConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "reignofnether";
    private static final String OLD_CONFIG_FILENAME = "reignofnether-config.json";
    private static final String UNITS_FILENAME = "units.json";
    private static final String BUILDINGS_FILENAME = "buildings.json";
    private static final String RESEARCH_FILENAME = "research.json";
    private static final String ENCHANTMENTS_FILENAME = "enchantments.json";
    private static JsonConfig config;

    public static JsonConfig loadOrCreateConfig() {
        Path configDirPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR);
        Path oldConfigPath = FMLPaths.CONFIGDIR.get().resolve(OLD_CONFIG_FILENAME);
        
        // Check if old config exists and migrate it
        if (Files.exists(oldConfigPath)) {
            try {
                config = migrateOldConfig();
                saveConfig();
                // Rename old config file
                Files.move(oldConfigPath, FMLPaths.CONFIGDIR.get().resolve("reignofnether-config-old.json"));
                ReignOfNether.LOGGER.info("Migrated old JSON configuration from {}", oldConfigPath);
                return config;
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("Failed to migrate old JSON config file: {}", e.getMessage());
            }
        }
        
        // Check if new config exists
        if (Files.exists(configDirPath) && hasAllConfigFiles(configDirPath)) {
            config = new JsonConfig();
            try {
                loadUnitsConfig(configDirPath);
                loadBuildingsConfig(configDirPath);
                loadResearchConfig(configDirPath);
                loadEnchantmentsConfig(configDirPath);
                ReignOfNether.LOGGER.info("Loaded JSON configuration from {}", configDirPath);
                return config;
            } catch (IOException e) {
                ReignOfNether.LOGGER.error("Failed to read JSON config files: {}", e.getMessage());
            }
        }

        config = createDefaultConfig();
        saveConfig();
        return config;
    }

    public static JsonConfig createDefaultConfig() {
        JsonConfig defaultConfig = new JsonConfig();
        
        // Units
        defaultConfig.units.put("creeper", new UnitEntry(50, 0, 100, 35, 2));
        defaultConfig.units.put("zombie", new UnitEntry(75, 0, 0, 18, 1));
        defaultConfig.units.put("zombie_villager", new UnitEntry(50, 0, 0, 15, 1));
        defaultConfig.units.put("skeleton", new UnitEntry(50, 45, 0, 18, 1));
        defaultConfig.units.put("stray", new UnitEntry(50, 45, 0, 18, 1));
        defaultConfig.units.put("husk", new UnitEntry(75, 0, 0, 18, 1));
        defaultConfig.units.put("drowned", new UnitEntry(75, 0, 0, 18, 1));
        defaultConfig.units.put("spider", new UnitEntry(90, 25, 25, 25, 2));
        defaultConfig.units.put("poison_spider", new UnitEntry(90, 25, 25, 25, 2));
        defaultConfig.units.put("slime", new UnitEntry(40, 40, 40, 25, 2));
        defaultConfig.units.put("warden", new UnitEntry(275, 0, 125, 50, 5));
        defaultConfig.units.put("zombie_piglin", new UnitEntry(0, 0, 0, 10, 1));
        defaultConfig.units.put("zoglin", new UnitEntry(0, 0, 0, 10, 2));
        defaultConfig.units.put("necromancer", new UnitEntry(0, 0, 0, 30, 5));
        
        // Villagers
        defaultConfig.units.put("villager", new UnitEntry(50, 0, 0, 15, 1));
        defaultConfig.units.put("militia", new UnitEntry(50, 0, 0, 15, 1));
        defaultConfig.units.put("iron_golem", new UnitEntry(0, 50, 250, 45, 4));
        defaultConfig.units.put("pillager", new UnitEntry(120, 80, 0, 32, 3));
        defaultConfig.units.put("vindicator", new UnitEntry(170, 0, 0, 32, 3));
        defaultConfig.units.put("witch", new UnitEntry(90, 90, 90, 35, 3));
        defaultConfig.units.put("evoker", new UnitEntry(150, 0, 120, 35, 3));
        defaultConfig.units.put("ravager", new UnitEntry(400, 50, 150, 60, 7));
        defaultConfig.units.put("royal_guard", new UnitEntry(0, 0, 0, 30, 5));
        
        // Piglins
        defaultConfig.units.put("grunt", new UnitEntry(50, 0, 0, 15, 1));
        defaultConfig.units.put("brute", new UnitEntry(120, 0, 0, 25, 2));
        defaultConfig.units.put("headhunter", new UnitEntry(90, 60, 0, 25, 2));
        defaultConfig.units.put("hoglin", new UnitEntry(150, 0, 75, 35, 3));
        defaultConfig.units.put("blaze", new UnitEntry(40, 40, 100, 30, 2));
        defaultConfig.units.put("wither_skeleton", new UnitEntry(200, 0, 125, 40, 4));
        defaultConfig.units.put("ghast", new UnitEntry(100, 100, 250, 60, 6));
        defaultConfig.units.put("magma_cube", new UnitEntry(40, 40, 40, 25, 2));
        defaultConfig.units.put("piglin_merchant", new UnitEntry(0, 0, 0, 30, 5));
        
        // Neutral
        defaultConfig.units.put("enderman", new UnitEntry(75, 75, 75, 35, 3));
        defaultConfig.units.put("polar_bear", new UnitEntry(250, 0, 0, 40, 4));
        defaultConfig.units.put("grizzly_bear", new UnitEntry(250, 0, 0, 40, 4));
        defaultConfig.units.put("panda", new UnitEntry(250, 0, 0, 40, 4));
        defaultConfig.units.put("wolf", new UnitEntry(120, 0, 0, 25, 2));
        defaultConfig.units.put("llama", new UnitEntry(180, 0, 0, 25, 2));
        
        defaultConfig.units.put("hero_base_revive_cost", new UnitEntry(100, 0, 0, 30, 5));
        defaultConfig.units.put("hero_extra_revive_cost_per_level", new UnitEntry(50, 0, 0, 5, 0));
        
        // Buildings
        defaultConfig.buildings.put("beacon", new BuildingEntry(0, 1000, 1000, 0, 0));
        defaultConfig.buildings.put("stockpile", new BuildingEntry(0, 75, 0, 0, 0));
        defaultConfig.buildings.put("oak_bridge", new BuildingEntry(0, 100, 0, 0, 0));
        defaultConfig.buildings.put("spruce_bridge", new BuildingEntry(0, 100, 0, 0, 0));
        defaultConfig.buildings.put("blackstone_bridge", new BuildingEntry(0, 0, 100, 0, 0));
        
        // Monster Buildings
        defaultConfig.buildings.put("mausoleum", new BuildingEntry(0, 350, 250, 0, 10));
        defaultConfig.buildings.put("haunted_house", new BuildingEntry(0, 100, 0, 0, 10));
        defaultConfig.buildings.put("pumpkin_farm", new BuildingEntry(0, 200, 0, 0, 0));
        defaultConfig.buildings.put("sculk_catalyst", new BuildingEntry(0, 125, 0, 0, 0));
        defaultConfig.buildings.put("graveyard", new BuildingEntry(0, 150, 0, 0, 0));
        defaultConfig.buildings.put("spider_lair", new BuildingEntry(0, 150, 75, 0, 0));
        defaultConfig.buildings.put("dungeon", new BuildingEntry(0, 150, 75, 0, 0));
        defaultConfig.buildings.put("laboratory", new BuildingEntry(0, 250, 150, 0, 0));
        defaultConfig.buildings.put("dark_watchtower", new BuildingEntry(0, 100, 75, 0, 0));
        defaultConfig.buildings.put("slime_pit", new BuildingEntry(0, 175, 125, 0, 0));
        defaultConfig.buildings.put("stronghold", new BuildingEntry(0, 400, 300, 0, 0));
        defaultConfig.buildings.put("altar_of_darkness", new BuildingEntry(0, 125, 50, 0, 0));
        
        // Villager Buildings
        defaultConfig.buildings.put("town_centre", new BuildingEntry(0, 350, 250, 0, 10));
        defaultConfig.buildings.put("villager_house", new BuildingEntry(0, 100, 0, 0, 10));
        defaultConfig.buildings.put("wheat_farm", new BuildingEntry(0, 150, 0, 0, 0));
        defaultConfig.buildings.put("barracks", new BuildingEntry(0, 150, 0, 0, 0));
        defaultConfig.buildings.put("blacksmith", new BuildingEntry(0, 100, 300, 0, 0));
        defaultConfig.buildings.put("arcane_tower", new BuildingEntry(0, 200, 100, 0, 0));
        defaultConfig.buildings.put("library", new BuildingEntry(0, 300, 100, 0, 0));
        defaultConfig.buildings.put("watchtower", new BuildingEntry(0, 100, 75, 0, 0));
        defaultConfig.buildings.put("castle", new BuildingEntry(0, 400, 300, 0, 0));
        defaultConfig.buildings.put("iron_golem_building", new BuildingEntry(0, 50, 250, 0, 0));
        defaultConfig.buildings.put("shrine_of_prosperity", new BuildingEntry(0, 125, 50, 0, 0));
        
        // Piglin Buildings
        defaultConfig.buildings.put("central_portal", new BuildingEntry(0, 350, 250, 0, 10));
        defaultConfig.buildings.put("basic_portal", new BuildingEntry(0, 75, 0, 0, 0));
        defaultConfig.buildings.put("civilian_portal", new BuildingEntry(0, 75, 0, 0, 15));
        defaultConfig.buildings.put("netherwart_farm", new BuildingEntry(0, 150, 0, 0, 0));
        defaultConfig.buildings.put("bastion", new BuildingEntry(0, 150, 100, 0, 0));
        defaultConfig.buildings.put("hoglin_stables", new BuildingEntry(0, 250, 0, 0, 0));
        defaultConfig.buildings.put("flame_sanctuary", new BuildingEntry(0, 300, 150, 0, 0));
        defaultConfig.buildings.put("wither_shrine", new BuildingEntry(0, 350, 200, 0, 0));
        defaultConfig.buildings.put("basalt_springs", new BuildingEntry(0, 200, 200, 0, 0));
        defaultConfig.buildings.put("fortress", new BuildingEntry(0, 400, 300, 0, 0));
        defaultConfig.buildings.put("infernal_portal", new BuildingEntry(0, 125, 50, 0, 0));
        
        // Research (sample - add all research items)
        defaultConfig.research.put("golem_smithing", new ResearchEntry(0, 150, 200, 90, 0));
        defaultConfig.research.put("militia_bows", new ResearchEntry(250, 500, 0, 160, 0));
        defaultConfig.research.put("lab_lightning_rod", new ResearchEntry(0, 0, 400, 120, 0));
        // Add more research items...
        
        // Enchantments
        defaultConfig.enchantments.put("maiming", new EnchantmentEntry(0, 20, 30, 0, 0));
        defaultConfig.enchantments.put("quick_charge", new EnchantmentEntry(0, 40, 20, 0, 0));
        defaultConfig.enchantments.put("sharpness", new EnchantmentEntry(0, 40, 60, 0, 0));
        defaultConfig.enchantments.put("multishot", new EnchantmentEntry(0, 70, 35, 0, 0));
        defaultConfig.enchantments.put("vigor", new EnchantmentEntry(0, 60, 60, 0, 0));
        
        return defaultConfig;
    }
    
    private static JsonConfig migrateOldConfig() {
        // Create a temporary config class for migration
        class OldJsonConfig {
            public Map<String, JsonResourceCostEntry> units = new HashMap<>();
            public Map<String, JsonResourceCostEntry> buildings = new HashMap<>();
            public Map<String, JsonResourceCostEntry> research = new HashMap<>();
            public Map<String, JsonResourceCostEntry> enchantments = new HashMap<>();
        }
        
        JsonConfig newConfig = new JsonConfig();
        
        // We need to read the old file directly with the old structure
        Path oldConfigPath = FMLPaths.CONFIGDIR.get().resolve(OLD_CONFIG_FILENAME);
        try (FileReader reader = new FileReader(oldConfigPath.toFile())) {
            OldJsonConfig oldConfigData = GSON.fromJson(reader, OldJsonConfig.class);
            
            // Migrate units
            for (Map.Entry<String, JsonResourceCostEntry> entry : oldConfigData.units.entrySet()) {
                JsonResourceCostEntry old = entry.getValue();
                newConfig.units.put(entry.getKey(), new UnitEntry(old.food, old.wood, old.ore, old.seconds, old.population));
            }
            
            // Migrate buildings  
            for (Map.Entry<String, JsonResourceCostEntry> entry : oldConfigData.buildings.entrySet()) {
                JsonResourceCostEntry old = entry.getValue();
                newConfig.buildings.put(entry.getKey(), new BuildingEntry(old.food, old.wood, old.ore, old.seconds, old.population));
            }
            
            // Migrate research
            for (Map.Entry<String, JsonResourceCostEntry> entry : oldConfigData.research.entrySet()) {
                JsonResourceCostEntry old = entry.getValue();
                newConfig.research.put(entry.getKey(), new ResearchEntry(old.food, old.wood, old.ore, old.seconds, old.population));
            }
            
            // Migrate enchantments
            for (Map.Entry<String, JsonResourceCostEntry> entry : oldConfigData.enchantments.entrySet()) {
                JsonResourceCostEntry old = entry.getValue();
                newConfig.enchantments.put(entry.getKey(), new EnchantmentEntry(old.food, old.wood, old.ore, old.seconds, old.population));
            }
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("Failed to read old config for migration: {}", e.getMessage());
            return createDefaultConfig();
        }
        
        return newConfig;
    }
    
    private static boolean hasAllConfigFiles(Path configDirPath) {
        return Files.exists(configDirPath.resolve(UNITS_FILENAME)) &&
               Files.exists(configDirPath.resolve(BUILDINGS_FILENAME)) &&
               Files.exists(configDirPath.resolve(RESEARCH_FILENAME)) &&
               Files.exists(configDirPath.resolve(ENCHANTMENTS_FILENAME));
    }
    
    private static void loadUnitsConfig(Path configDirPath) throws IOException {
        Path unitsPath = configDirPath.resolve(UNITS_FILENAME);
        Type type = new TypeToken<Map<String, UnitEntry>>(){}.getType();
        try (FileReader reader = new FileReader(unitsPath.toFile())) {
            config.units = GSON.fromJson(reader, type);
        }
    }
    
    private static void loadBuildingsConfig(Path configDirPath) throws IOException {
        Path buildingsPath = configDirPath.resolve(BUILDINGS_FILENAME);
        Type type = new TypeToken<Map<String, BuildingEntry>>(){}.getType();
        try (FileReader reader = new FileReader(buildingsPath.toFile())) {
            config.buildings = GSON.fromJson(reader, type);
        }
    }
    
    private static void loadResearchConfig(Path configDirPath) throws IOException {
        Path researchPath = configDirPath.resolve(RESEARCH_FILENAME);
        Type type = new TypeToken<Map<String, ResearchEntry>>(){}.getType();
        try (FileReader reader = new FileReader(researchPath.toFile())) {
            config.research = GSON.fromJson(reader, type);
        }
    }
    
    private static void loadEnchantmentsConfig(Path configDirPath) throws IOException {
        Path enchantmentsPath = configDirPath.resolve(ENCHANTMENTS_FILENAME);
        Type type = new TypeToken<Map<String, EnchantmentEntry>>(){}.getType();
        try (FileReader reader = new FileReader(enchantmentsPath.toFile())) {
            config.enchantments = GSON.fromJson(reader, type);
        }
    }


    public static void saveConfig() {
        if (config == null) return;
        
        Path configDirPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_DIR);
        
        try {
            // Create directory if it doesn't exist
            Files.createDirectories(configDirPath);
            
            // Save each category to its own file
            saveUnitsConfig(configDirPath);
            saveBuildingsConfig(configDirPath);
            saveResearchConfig(configDirPath);
            saveEnchantmentsConfig(configDirPath);
            
            ReignOfNether.LOGGER.info("Saved JSON configuration to {}", configDirPath);
        } catch (IOException e) {
            ReignOfNether.LOGGER.error("Failed to save JSON config files: {}", e.getMessage());
        }
    }
    
    private static void saveUnitsConfig(Path configDirPath) throws IOException {
        Path unitsPath = configDirPath.resolve(UNITS_FILENAME);
        try (FileWriter writer = new FileWriter(unitsPath.toFile())) {
            GSON.toJson(config.units, writer);
        }
    }
    
    private static void saveBuildingsConfig(Path configDirPath) throws IOException {
        Path buildingsPath = configDirPath.resolve(BUILDINGS_FILENAME);
        try (FileWriter writer = new FileWriter(buildingsPath.toFile())) {
            GSON.toJson(config.buildings, writer);
        }
    }
    
    private static void saveResearchConfig(Path configDirPath) throws IOException {
        Path researchPath = configDirPath.resolve(RESEARCH_FILENAME);
        try (FileWriter writer = new FileWriter(researchPath.toFile())) {
            GSON.toJson(config.research, writer);
        }
    }
    
    private static void saveEnchantmentsConfig(Path configDirPath) throws IOException {
        Path enchantmentsPath = configDirPath.resolve(ENCHANTMENTS_FILENAME);
        try (FileWriter writer = new FileWriter(enchantmentsPath.toFile())) {
            GSON.toJson(config.enchantments, writer);
        }
    }

    public static JsonConfig getConfig() {
        return config;
    }
}