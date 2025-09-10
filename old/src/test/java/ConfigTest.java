import com.solegendary.reignofnether.config.JsonConfigManager;
import com.solegendary.reignofnether.config.JsonConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {

    @Test
    public void testConfigStructure() {
        JsonConfig config = JsonConfigManager.createDefaultConfig();
        
        assertNotNull(config.units);
        assertNotNull(config.buildings);
        assertNotNull(config.research);
        assertNotNull(config.enchantments);
        
        // Test that we have some data
        assertTrue(config.units.size() > 0);
        assertTrue(config.buildings.size() > 0);
        assertTrue(config.research.size() > 0);  
        assertTrue(config.enchantments.size() > 0);
        
        // Test that types are correct
        assertNotNull(config.units.get("creeper"));
        assertNotNull(config.buildings.get("beacon"));
        assertNotNull(config.research.get("golem_smithing"));
        assertNotNull(config.enchantments.get("maiming"));
        
        System.out.println("Config structure test passed!");
        System.out.println("Units: " + config.units.size());
        System.out.println("Buildings: " + config.buildings.size());
        System.out.println("Research: " + config.research.size());
        System.out.println("Enchantments: " + config.enchantments.size());
    }
}