package de.holarse.rssgrabber.feedhandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.holarse.rssgrabber.CategoryConfig;
import de.holarse.rssgrabber.cache.FeedCacheEntry;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;
import de.holarse.rssgrabber.feedhandler.config.SteamFilterConfig;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lädt die Feedeinträge anhand der Steam-Events
 * @author comrad
 */
public class SteamEventsFeedHandler extends AbstractFeedHandler<SteamFilterConfig> {

    private final static Logger logger = LoggerFactory.getLogger(SteamEventsFeedHandler.class);        
    
    // Zu lesende Elemente
    private final static int COUNT_AFTER = 6;
    
    @Override
    protected List<FeedCacheEntry> parseWorkEntries(final String name, final InputStream is) throws Exception {
        final List<String> eventIds = getConfig().getEventIds();        

        // JSON einlesen
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(is);
        if (root.get("success").asInt() != 1) {
            throw new IllegalStateException("feed not successfull says steam");
        }
        
        final List<FeedCacheEntry> result = new ArrayList<>(25);

        final Iterator<JsonNode> it = root.get("events").elements();
        while (it.hasNext()) {
            final JsonNode node = it.next();

            // Daten sammeln
            final String eventName = node.get("event_name").asText();
            final String gid = node.get("gid").asText();
            final String postDate = node.findPath("announcement_body").get("posttime").asText();
            final String appid = node.get("appid").asText();

            final String entryLink = formatLink(appid, gid);
            final LocalDate timestamp = ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.valueOf(postDate)), ZoneOffset.UTC).toLocalDate();

            logger.debug("Checking live feed '{}' with entry '{}' for unique link '{}'.", name, eventName, entryLink);                
            
            //
            // Prüfung: Greift der EventType-Filter?
            //
            final String eventType = node.get("event_type").asText().trim();
            logger.debug("Allowing EventIds {}", eventIds.stream().collect(Collectors.joining(",")));                
            if (!eventIds.contains(eventType)) {
                logger.debug("News {} aus Feed {} ignoriert, weil falscher EventType {}", node.get("event_name"), getConfig().getName(), eventType);
                continue;
            }

            final FeedCacheEntry fce = new FeedCacheEntry();
            fce.setTitle(eventName);
            fce.setLink(entryLink);
            fce.setTimestamp(timestamp);   

            fce.setUuid(getUniqueReference(fce));  // Funktioniert, sobald Title und Link gesetzt sind

            // Die Steam-EventId in eine Drückblick-Kategorie umwandeln, wenn die Feed-Kategorie verwendet soll,
            // sonst wird die Feed-Kategorie verwendet, wie bei den anderen Feed-Typen auch
            if (getConfig().isUseFeedCategory()) {
                final String category = CategoryConfig.resolveSteamEventToCategory(eventType);
                fce.setCategory(category);
                logger.debug("Probing eventType {} into category {}", eventType, category);            
            }

            result.add(fce);
        }

        return result;
    }

    public SteamEventsFeedHandler(final ConfigFile configFile) throws Exception {
        super(SteamFilterConfig.class, configFile);
    }     
    
    @Override
    protected String getFeedConnectionUrl() {
        return getSteamEventUrl(getConfig().getSteamid(), getConfig().getSource());
    } 

    private String getSteamEventUrl(final String steamid, final String url) {
        if (StringUtils.isNotBlank(steamid)) {
            return String.format("https://store.steampowered.com/events/ajaxgetadjacentpartnerevents/?appid=%s&lang_list=1_0&count_after=%d", steamid, COUNT_AFTER);
        }
        
        return url;
    }
    
    private String formatLink(final String appid, final String gid) {
        return String.format("https://store.steampowered.com/news/app/%s/view/%s", appid, gid);
    }
    
}
