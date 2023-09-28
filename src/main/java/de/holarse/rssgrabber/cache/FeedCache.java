package de.holarse.rssgrabber.cache;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;


public class FeedCache {
    
    private String name;
    private String feedUrl;

    @JsonFormat(shape=JsonFormat.Shape.STRING) 
    private LocalDateTime lastUpdate;
    private List<FeedCacheEntry> entries = new ArrayList<>();

    public FeedCache() {
    }

    public FeedCache(final String name, final String feedUrl, final LocalDateTime lastUpdate) {
        this.name = name;
        this.feedUrl = feedUrl;
        this.lastUpdate = lastUpdate;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }
    
    public List<FeedCacheEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<FeedCacheEntry> entries) {
        this.entries = entries;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
}
