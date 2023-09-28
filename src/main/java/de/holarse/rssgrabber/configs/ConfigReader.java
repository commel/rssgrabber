package de.holarse.rssgrabber.configs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.holarse.rssgrabber.AppConfig;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Der ConfigReader liest eine einzelne Feed-Konfiguration ein.
 */
public class ConfigReader {

    private final static Logger logger = LoggerFactory.getLogger(ConfigReader.class);

    /**
     * Liest alle Konfigurationseinträge ein
     * @return 
     */
    public static List<ConfigFile> readAll() {
        final List<ConfigFile> configs = new ArrayList<>(50);
        logger.debug("Reading configurations from directory {}", AppConfig.getConfigsDir().getPath());
        final File[] configList = AppConfig.getConfigsDir().listFiles((pathname) -> pathname.getPath().endsWith(".properties"));
        Arrays.sort(configList);
        
        for(final File configfile : configList) {
            try {
                configs.add(read(configfile));
            } catch (Exception e) {
                logger.error("Error reading configuation " + configfile.getPath(), e);
            }
        }

        return configs;
    }

    /**
     * Liest eine spezifische Feed-Konfiguration ein
     * @param configfile
     * @return 
     * @throws java.lang.Exception
     * @throws java.io.IOException
     */
    public static ConfigFile read(final File configfile) throws Exception, IOException {
        logger.debug("Loading feed configuration file {}", configfile.getPath());
        try (final InputStreamReader in = new InputStreamReader(new FileInputStream(configfile), StandardCharsets.UTF_8)) {
            final Properties prop = new Properties();
            prop.load(in);
            
            final HashMap<String, String> map = new HashMap<>(5);
            for(final Map.Entry<Object, Object> entry: prop.entrySet()) {
                map.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }

            return new ConfigFile(configfile.getName(), map);
        } catch (final IOException ex) {
            logger.error("Error reading config file", ex);
            throw ex;
        }
    }

    private ConfigReader() {};

}