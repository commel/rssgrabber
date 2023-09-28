package de.holarse.rssgrabber.feedhandler.config;

import de.holarse.rssgrabber.configs.FeedType;
import java.util.Map;

public class ConfigFile {

    private String filename;
    private Map<String, String> configData;
    
    public ConfigFile(final String filename, final Map<String, String> configData) {
        this.filename = filename;
        this.configData = configData;
    }
    
    /**
     * Versucht den FeedType zu erraten, wenn auf AUTO. Sonst
     * @return 
     */
    public FeedType getFeedType() {
        if (configData.containsKey("feedtype")) {
            return FeedType.valueOf(configData.get("feedtype").toUpperCase());
        }

        if (configData.containsKey("url")) {
            if (configData.get("url").contains("github.com")) {
                return FeedType.GITHUB;
            }
        }
        
        if (configData.containsKey("steamid")) {
            return FeedType.STEAM;
        }
        
        return FeedType.ATOM;
    }
    
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Map<String, String> getConfigData() {
        return configData;
    }

    public void setConfigData(Map<String, String> configData) {
        this.configData = configData;
    }
    
}
