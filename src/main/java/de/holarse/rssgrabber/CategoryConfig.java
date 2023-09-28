package de.holarse.rssgrabber;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryConfig {

    private final static Logger logger = LoggerFactory.getLogger(CategoryConfig.class);

    private final static String CATEGORY_CONFIG_FILE = "categories.properties";    
    
    private static String defaultCategory = "";
    
    private static final Map<String, String> categories = new HashMap<>(30);
    
    public static void loadConfig() {
        logger.info("Loading category configuration file {}", CATEGORY_CONFIG_FILE);
        
        try (final InputStreamReader in = new InputStreamReader(new FileInputStream(CATEGORY_CONFIG_FILE), StandardCharsets.UTF_8)) {
            final Properties prop = new Properties();
            prop.load(in);
            
            //
            // Standard-Kategorie
            //
            defaultCategory = prop.getProperty("default_category", "Panorama");
            
            for(final String key : prop.keySet().stream().map(k -> (String) k).toList()) {
                categories.put(key, prop.getProperty(key));
            }

        } catch (final IOException ex) {
            logger.error("Error while reading category configuration", ex);
        }
    }
    
    private CategoryConfig() {};
    
    /**
     * Kategorie-Key in Kategorie-Text auflösen
     * @param key
     * @return 
     */
    public static String resolveCategory(final String key) {
        if (key == null || key.isEmpty() || key.isBlank()) {
            return defaultCategory;
        }
        
        return categories.getOrDefault(key, defaultCategory);
    }
    
    /**
     * Steam-EventId auflösen in Kategorie-Key, darüber dann wieder zu einem Kategorie-Text
     * @param event
     * @return 
     */
    public static String resolveSteamEventToCategory(final String event) {
        return resolveCategory(categories.getOrDefault(String.format("STEAM_%s", event), ""));
    }
    
}
