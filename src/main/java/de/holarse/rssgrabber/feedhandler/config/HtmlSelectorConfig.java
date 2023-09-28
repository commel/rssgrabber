package de.holarse.rssgrabber.feedhandler.config;

public class HtmlSelectorConfig extends AbstractConfig {

    private final String select;
    private final String linkAttribute;
    
    public HtmlSelectorConfig(final ConfigFile configFile) {
        super(configFile, "");
        select = getConfigData().getOrDefault("select", "");
        linkAttribute = getConfigData().getOrDefault("linkAttr", "");
    }

    public String getSelect() {
        return select;
    }

    public String getLinkAttribute() {
        return linkAttribute;
    }
    
}
