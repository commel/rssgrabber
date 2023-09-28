package de.holarse.rssgrabber.cache;

import java.time.LocalDate;

public class FeedCacheEntry {

    // Ein formatiertes normalisiertes und was weiss ich noch Erkennungszeichen
    private String uuid  = "";    
    private LocalDate timestamp;
    private String title = "";
    private String link = "";
    private String category = "";

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public LocalDate getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final LocalDate timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return String.format("Entry %s, %s, %s, %s", title, link, category, timestamp);
    }    
    
}
