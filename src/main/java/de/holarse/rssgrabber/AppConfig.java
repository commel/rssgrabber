package de.holarse.rssgrabber;

import static de.holarse.rssgrabber.ConfigFields.*;
import de.holarse.rssgrabber.configs.FilterConfig;
import de.holarse.rssgrabber.util.FeedUtils;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basis-Programm-Konfiguration
 */
public class AppConfig {
    private final static Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private final static String MAIN_CONFIG_FILE = "config.properties";

    private static File configsDir;
    private static File cacheDir;
    private static int maxage; // in Tagen
    private static int threads;
    private static final List<InternetAddress> recipients = new ArrayList<>(3);
    private static final InternetAddress sender = new InternetAddress();
    private static boolean mailActive;
    private static boolean useCache;
    // Timeout in Minuten für Feed-Ausführung insgesamt
    private static int execTimeout;        
    // Timeout in Sekunden für Feed-Ausführung in Sekunden
    private static int feedTimeout; 
    private static FilterConfig globalFilterConfig;

    // Höchstanzahl an Newsmeldungen je Feed
    private static int maxElementsPerFeed;
    
    private static boolean matterbridgeActive;
    private static String matterbridgeAvatar;
    private static String matterbridgeDomain;
    private static String matterbridgeGateway;
    private static String matterbridgeToken;
    
    private static String githubUsername;
    private static String githubToken;
    private static String githubDefaultCategory;

    private static final List<String> steamDefaultEventIds = new ArrayList<>();
    
    private static File objectDirectory;
    
    private static final String WEEK_OF_YEAR = LocalDate.now().format(DateTimeFormatter.ofPattern("w"));
    
    // Version wird direkt aus der pom.xml übernommen
    public final static transient String VERSION = Main.class.getPackage().getImplementationVersion();

    // Unser Erkennungsmerkmal
    private final static String REAL_HTTPAGENT = String.format("Java/%s (compatible; Holarse Newsfeed-Reader/%s; +https://holarse.de/content/rssgrabber)", System.getProperty("java.version"), VERSION);
    
    // Einige Feeds erlauben nur Browser oder Google
    private final static String FAKE_HTTPAGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:60.0) Gecko/20100101 Firefox/81.0";

    public final static String OBJ_FILE_EXTENSION = ".obj";
    
    private AppConfig() {};

    public static void loadAppConfig() throws AddressException {
        logger.info("Loading main configuration file {}", MAIN_CONFIG_FILE);
        logger.info("Announcing as http agent {}", REAL_HTTPAGENT);
        
        try (final InputStreamReader in = new InputStreamReader(new FileInputStream(MAIN_CONFIG_FILE), StandardCharsets.UTF_8)) {
            final Properties prop = new Properties();
            prop.load(in);

            //
            // config_dir
            //
            configsDir = new File(((String) prop.getOrDefault("config_dir", "feeds")).trim());
            if (!configsDir.exists()) {
                if (configsDir.mkdirs()) {
                    logger.info("Configuration directory created at " + configsDir.getPath());
                }
            }

            if (!configsDir.exists() || !configsDir.isDirectory() || !configsDir.canWrite()) {
                throw new IOException("config_dir " + configsDir.getPath() + " is not writeable or not present");
            }
            logger.debug("configs_dir set to: {}", configsDir.getAbsolutePath());

            //
            // Maximales Alter von Feedeinträgen
            //
            maxage = Integer.parseInt(prop.getProperty(MAX_AGE, "3").trim());            
            
            //
            // Maximale Anzahl paralleler Threads
            //
            threads = Integer.parseInt(prop.getProperty("parallel", "5").trim());

            //
            // Schalter, ob Mails verschickt werden sollen
            //
            mailActive = Boolean.parseBoolean(prop.getProperty("mail_active", "false").trim());            

            //
            // Wenn wahr, Cache verwenden
            //
            useCache = Boolean.parseBoolean(prop.getProperty("use_cache", "true").trim());                        
            
            //
            // cache_dir
            //
            cacheDir = new File(prop.getProperty("cache_dir", "cache").trim());
            if (!cacheDir.exists()) {
                if (cacheDir.mkdirs()) {
                    logger.info("Cache directory created at " + cacheDir.getPath());
                }
            }
            
            //
            // Mail-Recipient
            //
            recipients.addAll(Arrays.asList(InternetAddress.parse(prop.getProperty("recipients"))));

            //
            // Mail-Sender
            //
            sender.setAddress(prop.getProperty("sender", "noreply@holarse-linuxgaming.de"));
            sender.validate();
            
            //
            // Matterbridge API
            //
            matterbridgeActive = Boolean.parseBoolean(prop.getProperty("matterbridgeActive", "false"));
            matterbridgeAvatar = prop.getProperty("matterbridgeAvatar", "");
            matterbridgeDomain = prop.getProperty("matterbridgeDomain", "");
            matterbridgeGateway = prop.getProperty("matterbridgeGateway", "");
            matterbridgeToken = prop.getProperty("matterbridgeToken", "");

            //
            // Timeout der einzelnen Tasks in Millisekunden
            //
            feedTimeout = Integer.parseInt(prop.getProperty("feed_timeout", "250").trim());
            
            //
            // Gesamt-Ausführungszeit in Sekunden
            //
            execTimeout = Integer.parseInt(prop.getProperty("exec_timeout", "60").trim());
            
            //
            // Filter-Konfiguration, durch Kommata getrennt, Negierung durch vorangestelltes Ausrufezeichen
            //
            globalFilterConfig = FeedUtils.parseFilters(prop.getProperty(FILTER));

            //
            // Steam Default-EventIds
            //
            steamDefaultEventIds.addAll(Arrays.asList(prop.getProperty(String.format("steam_%s", STEAM_EVENTIDS), "10,12,13,14,15").split(",")));

            //
            // MaxNewsElementsPerFeed
            //
            maxElementsPerFeed = Integer.parseInt(prop.getProperty(MAX_ELEMENTS, "3"));
            
            // 
            // Github-Username
            //
            githubUsername = prop.getProperty("github_user", "");
            
            // 
            // Github-Standardkategorie
            //
            githubDefaultCategory = prop.getProperty("github_defaultcategory", "OPENSOURCE");            
            
            // 
            // Github-Token
            //            
            githubToken = prop.getProperty("github_token", "");
            
            //
            // Object-Directory zum Zwischenspeichern
            //
            objectDirectory = new File(prop.getProperty("object_dir", "queue"));
            
        } catch (final IOException | AddressException ex) {
            logger.error("Error while reading configuration", ex);
        }
    }

    public static File getConfigsDir() {
        return configsDir;
    }

    public static File getCacheDir() {
        return cacheDir;
    }
    
    public static int getThreads() {
        return threads;
    }
    
    public static List<InternetAddress> getRecipients() {
        return recipients;
    }
    
    public static InternetAddress getSender() {
        return sender;
    }
    
    public static int getMaxAge() {
        return maxage;
    }
    
    public static boolean isMailActive() {
        return mailActive;
    }

    public static boolean isMatterbridgeActive() {
        return matterbridgeActive;
    }
    
    public static String getMatterbridgeAvatar() {
        return matterbridgeAvatar;
    }
    
    public static String getMatterbridgeDomain() {
        return matterbridgeDomain;
    }

    public static String getMatterbridgeGateway() {
        return matterbridgeGateway;
    }

    public static String getMatterbridgeToken() {
        return matterbridgeToken;
    }
    
    public static String getWeekOfYear() {
        return WEEK_OF_YEAR;
    }

    public static boolean useCache() {
        return useCache;
    }

    /**
     * Timeout für Feedausführung in Millisekunden
     * @return int Anzahl der Millisekunden
     */
    public static int getFeedTimeout() {
	    return feedTimeout;
    }
    
    /**
     * Timeout für Gesamtausführung in Minuten
     * @return int Anzahl der Minuten
     */
    public static int getExecTimeout() {
	    return execTimeout;
    }    

    public static FilterConfig getGlobalFilterConfig() {
        return globalFilterConfig;
    }
        
    public static String getUserAgent() {
        return REAL_HTTPAGENT;
    }

    public static String getFakeUserAgent() {
        return FAKE_HTTPAGENT;
    }

    public static List<String> getSteamDefaultEventIds() {
	    return steamDefaultEventIds;
    }

    public static int getMaxElementsPerFeed() {
        return maxElementsPerFeed;
    }

    public static String getGithubUsername() {
        return githubUsername;
    }

    public static String getGithubToken() {
        return githubToken;
    }

    public static File getObjectDirectory() {
        return objectDirectory;
    }
       
}
