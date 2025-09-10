import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.solegendary.reignofnether.config.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;

public class ConfigSaveLoadTest {
    
    @TempDir
    Path tempDir;
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @Test
    public void testSeparateFileStructure() throws IOException {
        JsonConfig config = JsonConfigManager.createDefaultConfig();
        
        // Simulate the new file structure
        Path reignOfNetherDir = tempDir.resolve("reignofnether");
        Files.createDirectories(reignOfNetherDir);
        
        // Save each category to its own file  
        saveUnitsConfig(reignOfNetherDir, config);
        saveBuildingsConfig(reignOfNetherDir, config);
        saveResearchConfig(reignOfNetherDir, config);
        saveEnchantmentsConfig(reignOfNetherDir, config);
        
        // Verify files were created
        assertTrue(Files.exists(reignOfNetherDir.resolve("units.json")));
        assertTrue(Files.exists(reignOfNetherDir.resolve("buildings.json")));
        assertTrue(Files.exists(reignOfNetherDir.resolve("research.json")));
        assertTrue(Files.exists(reignOfNetherDir.resolve("enchantments.json")));
        
        // Test loading files back
        JsonConfig loadedConfig = loadConfiguration(reignOfNetherDir);
        
        // Verify data integrity
        assertEquals(config.units.size(), loadedConfig.units.size());
        assertEquals(config.buildings.size(), loadedConfig.buildings.size());
        assertEquals(config.research.size(), loadedConfig.research.size());
        assertEquals(config.enchantments.size(), loadedConfig.enchantments.size());
        
        // Test specific entries
        assertNotNull(loadedConfig.units.get("creeper"));
        assertNotNull(loadedConfig.buildings.get("beacon"));
        assertNotNull(loadedConfig.research.get("golem_smithing"));
        assertNotNull(loadedConfig.enchantments.get("maiming"));
        
        // Test value equality
        UnitEntry originalCreeper = config.units.get("creeper");
        UnitEntry loadedCreeper = loadedConfig.units.get("creeper");
        assertEquals(originalCreeper.food, loadedCreeper.food);
        assertEquals(originalCreeper.wood, loadedCreeper.wood);
        assertEquals(originalCreeper.ore, loadedCreeper.ore);
        assertEquals(originalCreeper.seconds, loadedCreeper.seconds);
        assertEquals(originalCreeper.population, loadedCreeper.population);
        
        System.out.println("Separate file structure test passed!");
        System.out.println("Files created: units.json, buildings.json, research.json, enchantments.json");
    }
    
    private void saveUnitsConfig(Path configDirPath, JsonConfig config) throws IOException {
        Path unitsPath = configDirPath.resolve("units.json");
        try (FileWriter writer = new FileWriter(unitsPath.toFile())) {
            GSON.toJson(config.units, writer);
        }
    }
    
    private void saveBuildingsConfig(Path configDirPath, JsonConfig config) throws IOException {
        Path buildingsPath = configDirPath.resolve("buildings.json");
        try (FileWriter writer = new FileWriter(buildingsPath.toFile())) {
            GSON.toJson(config.buildings, writer);
        }
    }
    
    private void saveResearchConfig(Path configDirPath, JsonConfig config) throws IOException {
        Path researchPath = configDirPath.resolve("research.json");
        try (FileWriter writer = new FileWriter(researchPath.toFile())) {
            GSON.toJson(config.research, writer);
        }
    }
    
    private void saveEnchantmentsConfig(Path configDirPath, JsonConfig config) throws IOException {
        Path enchantmentsPath = configDirPath.resolve("enchantments.json");
        try (FileWriter writer = new FileWriter(enchantmentsPath.toFile())) {
            GSON.toJson(config.enchantments, writer);
        }
    }
    
    private JsonConfig loadConfiguration(Path configDirPath) throws IOException {
        JsonConfig config = new JsonConfig();
        
        // Load units
        Path unitsPath = configDirPath.resolve("units.json");
        try (FileReader reader = new FileReader(unitsPath.toFile())) {
            com.google.gson.reflect.TypeToken<java.util.Map<String, UnitEntry>> typeToken = 
                new com.google.gson.reflect.TypeToken<java.util.Map<String, UnitEntry>>(){};
            config.units = GSON.fromJson(reader, typeToken.getType());
        }
        
        // Load buildings
        Path buildingsPath = configDirPath.resolve("buildings.json");
        try (FileReader reader = new FileReader(buildingsPath.toFile())) {
            com.google.gson.reflect.TypeToken<java.util.Map<String, BuildingEntry>> typeToken = 
                new com.google.gson.reflect.TypeToken<java.util.Map<String, BuildingEntry>>(){};
            config.buildings = GSON.fromJson(reader, typeToken.getType());
        }
        
        // Load research
        Path researchPath = configDirPath.resolve("research.json");
        try (FileReader reader = new FileReader(researchPath.toFile())) {
            com.google.gson.reflect.TypeToken<java.util.Map<String, ResearchEntry>> typeToken = 
                new com.google.gson.reflect.TypeToken<java.util.Map<String, ResearchEntry>>(){};
            config.research = GSON.fromJson(reader, typeToken.getType());
        }
        
        // Load enchantments
        Path enchantmentsPath = configDirPath.resolve("enchantments.json");
        try (FileReader reader = new FileReader(enchantmentsPath.toFile())) {
            com.google.gson.reflect.TypeToken<java.util.Map<String, EnchantmentEntry>> typeToken = 
                new com.google.gson.reflect.TypeToken<java.util.Map<String, EnchantmentEntry>>(){};
            config.enchantments = GSON.fromJson(reader, typeToken.getType());
        }
        
        return config;
    }
}