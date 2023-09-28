package de.holarse.rssgrabber.feedhandler.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import de.holarse.rssgrabber.AppConfig;
import static de.holarse.rssgrabber.ConfigFields.STEAM_EVENTIDS;
import java.util.Collections;

public class SteamFilterConfig extends AbstractConfig {

    private final static List<String> DEFAULT_EVENTIDS = AppConfig.getSteamDefaultEventIds();
    
    private final String steamid;
    private final List<String> eventIds = new ArrayList<>();
    private final boolean useFeedCategory;
    
    public SteamFilterConfig(final ConfigFile configFile) {
        super(configFile, "");
        
        if (getConfigData().containsKey("eventids")) {
            eventIds.addAll(Stream.of(getConfigData().get(STEAM_EVENTIDS).split(",")).map(String::trim).collect(Collectors.toList()));
        } else {
            eventIds.addAll(DEFAULT_EVENTIDS);
        }
        
        steamid = getConfigData().get("steamid");
        
        // Ist bereits manuell eine Kategorie definiert worden? Sonst soll die Kategorie aus dem Steam-Feed ermittelt werden? Standardmäßig ja
        useFeedCategory = isCategoryWasDefined() ? false : Boolean.parseBoolean(getConfigData().getOrDefault("use_feedcategory", "true"));
    }

    public List<String> getEventIds() {
        return Collections.unmodifiableList(eventIds);
    }
    
    public String getSteamid() {
        return steamid;
    }

    /**
     * Bei True: Die Feed-Kategory aus der Steam EventId wird verwendet, sonst die Feed-Konfiguration
     * 
     * @return 
     */
    public boolean isUseFeedCategory() {
        return useFeedCategory;
    }
    
}
