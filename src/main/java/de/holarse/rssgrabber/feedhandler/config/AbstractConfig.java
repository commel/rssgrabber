package de.holarse.rssgrabber.feedhandler.config;

import de.holarse.rssgrabber.AppConfig;
import de.holarse.rssgrabber.CategoryConfig;
import static de.holarse.rssgrabber.ConfigFields.*;
import de.holarse.rssgrabber.configs.DuplicateFilterType;
import de.holarse.rssgrabber.configs.FeedType;
import de.holarse.rssgrabber.configs.FilterConfig;
import de.holarse.rssgrabber.util.FeedUtils;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Standard-Feedkonfiguration für alle Feedarten
 * @author comrad
 */
public class AbstractConfig implements Config {

    private final static Logger logger = LoggerFactory.getLogger(AbstractConfig.class);    
    
    private final String filename;
    private final String name;
    private final String source;
    private final FilterConfig filterConfig;
    private final FeedType feedType;
    private final int localMaxAge;
    private final DuplicateFilterType duplicateFilterType;
    private final boolean useFakeUserAgent;
    private final int maxElementsPerFeed;
    private final String category;
    private final boolean categoryWasDefined;
    private final String changelog;
    
    private final Map<String, String> configData;
    
    public AbstractConfig(final ConfigFile configFile, final String categoryDefaultValue) {
        this.filename = configFile.getFilename();
        configData = configFile.getConfigData();
        
        this.name = configData.get("name");
        this.source = configData.get("url");
        this.filterConfig = FeedUtils.parseFilters(configData.getOrDefault("filter", null));
        this.feedType = FeedType.valueOf(configData.getOrDefault("feedtype", "ATOM").toUpperCase());
        this.duplicateFilterType = DuplicateFilterType.valueOf(configData.getOrDefault("duplicate", "AUTO").toUpperCase());
        
        final String str_local_maxage = configData.getOrDefault(MAX_AGE, "");
        if (str_local_maxage.isBlank()) {
            localMaxAge = AppConfig.getMaxAge();
        } else {
            localMaxAge = Integer.parseInt(str_local_maxage);
            logger.debug("Feed '{}' uses local max_age {}", this.name, localMaxAge);                
        } 
        
        this.useFakeUserAgent = Boolean.parseBoolean(configData.getOrDefault("fake_useragent", "false"));
        this.maxElementsPerFeed = Integer.parseInt(configData.getOrDefault(MAX_ELEMENTS, String.valueOf(AppConfig.getMaxElementsPerFeed())));
        
        // Wurde die Konfiguration explizit angegeben?
        categoryWasDefined = configData.get(CATEGORY) != null;
        // Kategorie auflösen
        this.category = CategoryConfig.resolveCategory(configData.getOrDefault(CATEGORY, categoryDefaultValue));
        
        this.changelog = configData.getOrDefault("changelog", "");
    }

    protected Map<String, String> getConfigData() {
        return Collections.unmodifiableMap(configData);
    }

    public boolean isCategoryWasDefined() {
        return categoryWasDefined;
    }

    @Override    
    public String getFilename() {
        return filename;
    }

    @Override    
    public String getName() {
        return name;
    }

    @Override    
    public String getSource() {
        return source;
    }

    @Override    
    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    @Override    
    public FeedType getFeedType() {
        return feedType;
    }

    @Override    
    public int getMaxAge() {
        return localMaxAge;
    }

    @Override
    public DuplicateFilterType getDuplicateFilter() {
        return duplicateFilterType;
    }

    @Override
    public boolean useFakeUserAgent() {
        return useFakeUserAgent;
    }

    @Override
    public int getMaxElementsPerFeed() {
        return maxElementsPerFeed;
    }
    
    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public Optional<String> getChangelog() {
        if (!StringUtils.isBlank(changelog)) {
            return Optional.of(changelog);
        }
        
        return Optional.empty();
    }
    
}
