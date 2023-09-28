package de.holarse.rssgrabber;

/**
 * Definition von Feldnamen, die in der globalen Konfiguration stehen
 * und die von den spezifischen Feed-Konfigurationen überschrieben werden können
 * @author comrad
 */
public class ConfigFields {

    // Allgemein
    public static final String MAX_AGE = "max_age";
    public static final String FILTER = "filter";
    public static final String MAX_ELEMENTS = "max_elements";
    
    // Steam
    public static final String STEAM_EVENTIDS = "eventids";
    
    // Github
    public static final String CATEGORY = "category";
    
    private ConfigFields() {}
    
}
