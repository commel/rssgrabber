package de.holarse.rssgrabber.feedhandler.config;

/**
 *
 * @author comrad
 */
public class GithubConfig extends AbstractConfig {
 
    private boolean allow_prereleases = false;
    
    public GithubConfig(final ConfigFile configFile) {
        super(configFile, "OPENSOURCE");
        
        if (getConfigData().containsKey("allow_prereleases")) {
            allow_prereleases = Boolean.parseBoolean(getConfigData().getOrDefault("allow_prereleases", "false"));
        }
    }    

    public boolean isPrereleasesAllowed() {
        return allow_prereleases;
    }
    
}
