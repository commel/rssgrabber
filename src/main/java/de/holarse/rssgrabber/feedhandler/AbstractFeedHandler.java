package de.holarse.rssgrabber.feedhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.holarse.rssgrabber.AppConfig;
import de.holarse.rssgrabber.cache.FeedCache;
import de.holarse.rssgrabber.cache.FeedCacheEntry;
import de.holarse.rssgrabber.feedhandler.config.Config;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;

import java.io.File;

import de.holarse.rssgrabber.output.FeedResult;
import de.holarse.rssgrabber.util.FeedUtils;
import de.holarse.rssgrabber.util.GrabberException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFeedHandler<T extends Config> implements Callable<FeedResult> {
    
    private final static Logger logger = LoggerFactory.getLogger(AbstractFeedHandler.class);    
    
    private final T config;
    
    // Lädt die FeedEntries für diesen Feed
    protected abstract List<FeedCacheEntry> parseWorkEntries(final String name, final InputStream is) throws Exception;

    public AbstractFeedHandler(final Class<T> configType, final ConfigFile configFile) throws Exception {
       this.config = configType.getConstructor(ConfigFile.class).newInstance(configFile);
    }
    
    protected T getConfig() {
        return config;
    }
  
    /**
     * Ermittelt den Dateinamen für die Cache-Datei
     * @return
     */
    protected File getCacheFile() {
        // Dateiendung durch cache ersetzen
        String basename;
        final int dot_index = getConfig().getFilename().lastIndexOf(".");
        if (dot_index == -1) {
            basename = getConfig().getFilename();
        } else {
            basename = getConfig().getFilename().substring(0, dot_index);
        }
        
        return new File(AppConfig.getCacheDir(), String.format("%s.cache", basename));
    }
    
    /**
     * Lädt die gecachten Einträge aus der Cache-Datei
     * @param file
     * @return
     * @throws IOException
     */
    List<FeedCacheEntry> loadCache(final File file, final LocalDate maxAge) throws IOException {
        final List<FeedCacheEntry> entries = new ArrayList<>(25);

        // Keine Cache-Datei erstmal kein Problem
        if (!file.exists()) {
            return entries;
        }
        
        final ObjectMapper mapper = new XmlMapper().registerModule(new JavaTimeModule())
                                                   .enable(SerializationFeature.INDENT_OUTPUT)
                                                   .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                                   .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        
        final FeedCache fc = mapper.readValue(new FileInputStream(file), FeedCache.class);
        
        logger.info("Loaded {} entries from cached file {}", fc.getEntries().size(), file);
        
        // Nur die Cache-Einträge verwenden, die vor dem Maximal-Alter liegen
        entries.addAll(fc.getEntries().stream().filter(e -> e.getTimestamp() == null || e.getTimestamp().isAfter(maxAge))
                                               .collect(Collectors.toList()));
        logger.debug("Added {} entries to cache (filtered by maxAge {})", entries.size(), maxAge);

        return entries;
    }

    /**
     * Erzeugt einen Index über die UUID-Einträge im Cache
     * @param entries
     * @return
     */
    Set<String> buildCacheIndex(final List<FeedCacheEntry> entries) {
        return entries.stream().map(c -> c.getUuid()).collect(Collectors.toUnmodifiableSet());
    }
    
    /**
     * Speichert den aktualisierten Cache in die Cache-Datei
     * @param file
     * @param feedUrl
     * @param cacheEntries
     * @throws FileNotFoundException
     * @throws IOException 
     */
    void saveCache(final File file, final String feedUrl, final List<FeedCacheEntry> cacheEntries) throws FileNotFoundException, IOException {
        final FeedCache output = new FeedCache(getConfig().getName(), feedUrl, LocalDateTime.now());
        output.setEntries(cacheEntries);
        
        final ObjectMapper mapper = new XmlMapper().registerModule(new JavaTimeModule())
                                                   .enable(SerializationFeature.INDENT_OUTPUT)
                                                   .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                                                   .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID);
        
        mapper.writeValue(new FileOutputStream(file), output);
    }

    /**
     * Öffnet eine Http-Verbindung zum Feed
     * @param feedUrl
     * @return
     * @throws IOException
     * @throws java.net.URISyntaxException
     * @throws java.lang.InterruptedException
     */
    protected InputStream getLiveFeedStream(final String feedUrl) throws IOException, IllegalArgumentException, URISyntaxException, InterruptedException {
        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                                                         .version(Version.HTTP_2)
                                                         .followRedirects(HttpClient.Redirect.NORMAL)
                                                         .build();

        final HttpRequest request = HttpRequest.newBuilder(URI.create(feedUrl))
                                               .GET()
                                               .setHeader("User-Agent", getHttpUserAgent())
                                               .setHeader("Accept", "application/atom+xml, application/xhtml+xml, application/xml, application/json")
                                               .build();
        
        final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        logger.debug("Loading feed {} as: {} with return code {}", config.getName(), request.uri(), response.statusCode());

        if (response.statusCode() >= 400) {
            logger.error("Error Status with feed {}: {}", feedUrl, response.statusCode());
            throw new IOException("Illegal feed result code 200 != " + response.statusCode());
        }
        
        return response.body();
    }

    protected String getHttpUserAgent() {
        return getConfig().useFakeUserAgent() ? AppConfig.getFakeUserAgent() : AppConfig.getUserAgent();
    }

    /**
     * Stellt die individuelle FeedURL für getFeedConnection() bereit.
     * @return
     */
    protected abstract String getFeedConnectionUrl();

    /**
     * Prüfung, ob bereits ein Eintrag im Cache vorliegt?
     * @param fce
     * @param cachedItems
     * @return true, falls Cacheeintrag vorhanden, false wenn nein
     */
    protected boolean hasCachedEntry(final FeedCacheEntry fce, final Set<String> cachedItems) {
        return cachedItems != null && cachedItems.contains(fce.getUuid());
    }

    /**
     * Prüft, ob ein Eintrag zu alt ist und nicht berücksichtigt wird
     * @param fce
     * @param maxAge
     * @return
     */
    protected boolean tooOld(final FeedCacheEntry fce, final LocalDate maxAge) {
        // Einträge ohne Datum, vor dem ältesten Datum oder gleich dem ältestem Datum fliegen raus
        return (fce.getTimestamp() == null || fce.getTimestamp().isBefore(maxAge) || fce.getTimestamp().isEqual(maxAge));
    }

    protected String getUniqueReference(final FeedCacheEntry fce) throws Exception {
        switch (getConfig().getDuplicateFilter()) {
            case AUTO:
                // fallthrough, da AUTO und LINK gleichbedeutend sind
            case LINK:
                logger.debug("Checking feed '{}' entry for unique link '{}'.", getConfig().getName(), fce.getLink());
                if (StringUtils.isBlank(fce.getLink())) {
                    throw new GrabberException(getConfig().getName(), "link (should be unique) is empty");
                }

                return fce.getLink();
            case TITLE:
                logger.debug("Checking feed '{}' entry for unique title '{}'.", getConfig().getName(), fce.getTitle());
                if (StringUtils.isBlank(fce.getTitle())) {
                    throw new GrabberException(getConfig().getName(), "title (should be unique) is empty");
                }

                return FeedUtils.normalizeText(fce.getTitle());
            default:
                throw new GrabberException(getConfig().getName(), "unhandled duplicate filter setting " + getConfig().getDuplicateFilter());
       }         
    }

    /**
     * Prüft, ob ein Filter auf dem Titel greift
     * @param fce
     * @return
     */
    protected boolean contentFilterMatches(final FeedCacheEntry fce) {
        final List<String> mustHaveFilters = getConfig().getFilterConfig().getMustHaveFilters();
        final boolean hasMustHaveFilters = !mustHaveFilters.isEmpty();

        final List<String> mustNotFilters = getConfig().getFilterConfig().getMustNotFilters();
        final boolean hasMustNotFilters = !mustNotFilters.isEmpty();        

        //
        // Prüfung 3: Greift ein Filter?
        //
        if (hasMustHaveFilters || hasMustNotFilters) {
            final String compareUnit = FeedUtils.normalizeText(fce.getTitle());  

            // Einer dieser Filter, wenn hinterlegt, muss passen, sonst raus
            if (hasMustHaveFilters) {
                if (mustHaveFilters.stream().map(f -> FeedUtils.normalizeText(f)).noneMatch(f -> compareUnit.contains(f))) {
                    logger.debug("Feed '{}' entry's content misses any defined must-have-filters: '{}', ignoring feed entry.", getConfig().getName(), mustHaveFilters);
                    return true;
                }
            }

            // Einer dieser Filter, wenn hinterlegt, darf auf garkeinen Fall passen, sonst raus
            if (hasMustNotFilters) {
                if (mustNotFilters.stream().map(f -> FeedUtils.normalizeText(f)).anyMatch(f -> compareUnit.contains(f))) {
                    logger.debug("Feed '{}' entry's content matches a must-not-filter: '{}', ignoring feed entry.", getConfig().getName(), mustNotFilters);
                    return true;
                }
            }
        } 
        
        return false;
    }
    
    /**
     * Generische Abhandlungsmethode
     * @return
     * @throws GrabberException
     */
    protected FeedResult updateFeed() throws GrabberException {
        boolean error = false;
        final String name = getConfig().getName();
        final File cacheFile = getCacheFile();
        final String feedUrl = getFeedConnectionUrl();

        // Die bereits gecachten Einträge
        final List<FeedCacheEntry> cache = new ArrayList<>(25);

        // Der Cache-Index über die UUIDs zum schnellen Auffinden
        final Set<String> cacheIndex = new HashSet<>(25);

        // Neue Einträge werden hier gesammelt
        final List<FeedCacheEntry> newEntries = new ArrayList<>(5);

        // Datum, bis wann wir vergleichen
        final LocalDate maxAge = LocalDate.now().minusDays(getConfig().getMaxAge());        

        if (AppConfig.useCache()) {
            try {
                // Cache einladen
                cache.addAll(loadCache(cacheFile, maxAge));
                cacheIndex.addAll(buildCacheIndex(cache));
            } catch (final Exception ex) {
                throw new GrabberException(getConfig().getName(), String.format("Error while examining feed cache %s", getConfig().getName()), ex);
            }   
        }
        
        try {
            logger.info("Feed {} is now being processed via URL: {}...", name, feedUrl);
            for (final FeedCacheEntry workEntry: parseWorkEntries(name, getLiveFeedStream(feedUrl))) {
                //
                // Prüfung 1: Greift ein Content-Filter?
                //
                if (contentFilterMatches(workEntry)) {
                    logger.info("Entry {}: {} removed by filter", name, workEntry);
                    continue;
                }
                
                //
                // Prüfung 2: Bereits im Cache?
                if (AppConfig.useCache()) {
                    if (hasCachedEntry(workEntry, cacheIndex)) {
                        logger.info("Entry {}: {} is already in cache", name, workEntry);
                        continue;
                    }
                }
                
                //
                // Prüfung 3: Zu alt?
                //                
                if (tooOld(workEntry, maxAge)) {
                    logger.info("Entry {}: {} is too old (maxage={})", name, workEntry, maxAge);
                    continue;
                }
                
                // Nichts spricht gegen diesen Eintrag. Wir haben also einen neuen Eintrag für diesen Feed.
                // Da wird die Feedliste rückwärts durchgehen, kommen die neueren später und überschreiben die identischen
                // älteren Feeds.
                
                // Damit wir uns das fürs nächste Mal merken
                if (AppConfig.useCache()) {
                    cache.add(workEntry);
                }

                // Für die Notifications
                newEntries.add(workEntry); 

                logger.info("Entry {}: {} is news!", name, workEntry);
            }
            
            if (logger.isDebugEnabled() && newEntries.isEmpty()) {
                logger.debug("Feed '{}' has no new entries.", getConfig().getName());
            }

            // Update Cache file
            if (AppConfig.useCache()) {
                try {
                    saveCache(cacheFile, feedUrl, cache);
                } catch (final IOException e) {
                    throw new GrabberException(getConfig().getName(), String.format("Error while writing feed %s output to cache file %s", getConfig().getName(), cacheFile), e);
                }   
            }            
            
        } catch (final Exception ex) {
            logger.error("Error in feed {}", getConfig().getName(), ex);
            throw new GrabberException(getConfig().getName(), ex);
        }
        
        return new FeedResult(getConfig(), newEntries.stream()
                                                               .limit(getConfig().getMaxElementsPerFeed())
                                                               .collect(Collectors.toList()), error);
    }   
            
    @Override
    public FeedResult call() throws GrabberException {
        return updateFeed();
    }
}
