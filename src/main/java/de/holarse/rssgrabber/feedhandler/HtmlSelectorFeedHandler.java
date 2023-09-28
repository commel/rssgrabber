package de.holarse.rssgrabber.feedhandler;

import de.holarse.rssgrabber.cache.FeedCacheEntry;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;
import de.holarse.rssgrabber.feedhandler.config.HtmlSelectorConfig;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlSelectorFeedHandler extends AbstractFeedHandler<HtmlSelectorConfig> {

    private final static Logger logger = LoggerFactory.getLogger(HtmlSelectorFeedHandler.class);            
    
    public HtmlSelectorFeedHandler(final ConfigFile configFile) throws Exception {
        super(HtmlSelectorConfig.class, configFile);
    }       
    
    @Override
    protected List<FeedCacheEntry> parseWorkEntries(final String name, final InputStream is) throws Exception {
        final Document doc = Jsoup.parse(is, null, getConfig().getSource());
        final Elements elements = doc.select(getConfig().getSelect());
        
        final List<FeedCacheEntry> entries = new ArrayList<>();
        
        for (final Element element : elements) {
            final FeedCacheEntry entry = new FeedCacheEntry();
            entry.setTitle(element.html());
            entry.setLink(getConfig().getLinkAttribute());
        }
        
        return entries;
    }

    @Override
    protected String getFeedConnectionUrl() {
        return getConfig().getSource();
    }
    
}
