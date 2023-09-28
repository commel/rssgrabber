package de.holarse.rssgrabber.feedhandler.config;

import de.holarse.rssgrabber.configs.DuplicateFilterType;
import de.holarse.rssgrabber.configs.FeedType;
import de.holarse.rssgrabber.configs.FilterConfig;
import java.util.Optional;

/**
 * Basis für die Konfiguration
 * @author comrad
 */
public interface Config {
    
    /**
     * Der Dateiname der Konfiguration
     * @return 
     */
    String getFilename();
    /**
     * Der Name des Feeds, passend zur Wikiseite
     * @return 
     */
    String getName();
    /**
     * Die Quelle des Feeds
     * @return 
     */
    String getSource();
    /**
     * Die Filterkonfiguration
     * @return 
     */
    FilterConfig getFilterConfig();
    /**
     * Die Art des Feeds
     * @return 
     */
    FeedType getFeedType();
    /**
     * Maximales Alter der Einträge für diesen Feed
     * @return 
     */
    int getMaxAge();
    /**
     * Duplikatsfindung
     * @return 
     */
    DuplicateFilterType getDuplicateFilter();

    /**
     * Angabe, ob wir uns wirklich zu erkennen geben sollen,
     * oder lieber einen gefälschten HTTP-Agent (Firefox-Browser) ausgeben sollen
     * @return true falls als Browser via AppConfig.REAL_HTTPAGENT, sonst Fake aus AppConfig.FAKE_HTTPAGENT
     */
    boolean useFakeUserAgent();

    /**
     * Die maximale Anzahl an Elementen, die durchgelassen werden. Soll Spamming verhindern, insbesondere
     * wenn bei Github wieder alle Tags aktualisiert werden.
     * @return
     */
    int getMaxElementsPerFeed();
    
    /**
     * Die Kategorie für diesen Feed
     * @return 
     */
    String getCategory();
    
    /**
     * Optionale externes Changelog
     * @return 
     */
    Optional<String> getChangelog();
    
}
