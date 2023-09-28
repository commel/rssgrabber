package de.holarse.rssgrabber.feedhandler;

import de.holarse.rssgrabber.cache.FeedCacheEntry;
import de.holarse.rssgrabber.feedhandler.config.AbstractConfig;
import de.holarse.rssgrabber.feedhandler.config.BasicConfig;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Lädt RSS und Atom-Feeds herunter
 * @author comrad
 */
public class AtomRssFeedHandler extends AbstractFeedHandler<BasicConfig> {
    
    private final static Logger logger = LoggerFactory.getLogger(AtomRssFeedHandler.class);    
    
    public AtomRssFeedHandler(final ConfigFile configFile) throws Exception {
        super(BasicConfig.class, configFile);
    }

    private enum FeedType {
        RSS2,
        ATOM,
        UNKNOWN
    }

    @Override
    protected String getFeedConnectionUrl() {
        return getConfig().getSource();
    }    

    @Override
    protected List<FeedCacheEntry> parseWorkEntries(final String name, final InputStream is) throws Exception {
        FeedType feedType = FeedType.UNKNOWN;
        final List<FeedCacheEntry> newWorkEntries = new ArrayList<>(25);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();   
        final Document document = builder.parse(is);

        NodeList nodeList = null;

        // Erkennung siehe https://de.wikipedia.org/wiki/RSS_(Web-Feed)
        final XPath rssPath = XPathFactory.newInstance().newXPath();  
        final NodeList rssPathNodeList = (NodeList) rssPath.compile("/rss/channel/item").evaluate(document, XPathConstants.NODESET);
        if (rssPathNodeList.getLength() > 0) {
            feedType = FeedType.RSS2;
            nodeList = rssPathNodeList;
        }

        if (feedType == FeedType.UNKNOWN) {
            // Erkennung siehe https://de.wikipedia.org/wiki/Atom_(Format)
            final XPath atomPath = XPathFactory.newInstance().newXPath();  
            final NodeList atomPathNodeList = (NodeList) atomPath.compile("/feed/entry").evaluate(document, XPathConstants.NODESET);
            if (atomPathNodeList.getLength() > 0) {
                feedType = FeedType.ATOM;
                nodeList = atomPathNodeList;
            }            
        }
        
        if (nodeList == null) {
            return newWorkEntries;
        }
        
        switch(feedType) {
            case ATOM:
                // Werte auslesen und in FeedCacheEntry für workEntries umwandeln
                for (int i=0; i < nodeList.getLength(); i++) {
                    final FeedCacheEntry fce = new FeedCacheEntry();
                    
                    final Node node = nodeList.item(i);
                    final Element element = (Element) node;
    
                    //final String id = element.getElementsByTagName("id").item(0).getTextContent();
                    final String title = element.getElementsByTagName("title").item(0).getTextContent();
                    final String link = element.getElementsByTagName("link").item(0).getAttributes().getNamedItem("href").getTextContent();
    
                    String timestamp = null;
    
                    final NodeList publishedNodes = element.getElementsByTagName("published");
                    if (publishedNodes.getLength() > 0) {
                        timestamp = publishedNodes.item(0).getTextContent();
                    } else {
                        final NodeList updatedNodes = element.getElementsByTagName("updated");
                        if (updatedNodes.getLength() > 0) {
                            timestamp = updatedNodes.item(0).getTextContent();
                        } else {
                        }
                    }

                    if (StringUtils.isNotBlank(timestamp)) {
                        try {
                            fce.setTimestamp(parseDate(timestamp));
                        } catch (final Exception e) {
                            logger.warn("Datum '{}' in Atom-Feed '{}' konnte nicht geparst werden:", timestamp, name, e);
                        }
                    }                    
    
                    fce.setTitle(title);
                    fce.setLink(link);
                    fce.setUuid(getUniqueReference(fce));  // Funktioniert, sobald Title und Link gesetzt sind
                    
                    newWorkEntries.add(fce);
                }
            break;
            case RSS2:
                // Werte auslesen und in FeedCacheEntry für workEntries umwandeln
                for (int i=0; i < nodeList.getLength(); i++) {
                    final FeedCacheEntry fce = new FeedCacheEntry();
                    
                    final Node node = nodeList.item(i);
                    final Element element = (Element) node;
    
                    //final String id = element.getElementsByTagName("guid").item(0).getTextContent();
                    final String title = element.getElementsByTagName("title").item(0).getTextContent();
                    final String link = element.getElementsByTagName("link").item(0).getTextContent();
    
                    String timestamp = "";
    
                    final NodeList publishedNodes = element.getElementsByTagName("pubDate");
                    if (publishedNodes.getLength() > 0) {
                        timestamp = publishedNodes.item(0).getTextContent();
                    } else {
                        final NodeList updatedNodes = element.getElementsByTagName("updated");
                        if (updatedNodes.getLength() > 0) {
                            timestamp = updatedNodes.item(0).getTextContent();
                        }                
                    }

                    if (StringUtils.isNotBlank(timestamp)) {
                        try {
                            fce.setTimestamp(parseDate(timestamp));
                        } catch (final Exception e) {
                            logger.warn("Datum '{}' in RSS2-Feed '{}' konnte nicht geparst werden:", timestamp, name, e);
                        }
                    }
    
                    fce.setTitle(title);
                    fce.setLink(link);
                    fce.setUuid(getUniqueReference(fce)); // Funktioniert, sobald Title und Link gesetzt sind

                    newWorkEntries.add(fce);
                }            
            break;
            default:
                logger.warn("Could not determine feed type for feed {}!", name);
        } 
            
        return newWorkEntries;
    }

    protected LocalDate parseDate(final String input) {
        String timestamp = input;
        // Sourceforge gibt nur UT raus
        if (timestamp.endsWith("UT"))
        timestamp += "C";

        if (timestamp.endsWith("Z"))
            timestamp.replaceAll("Z", "+00:00");        

        // Im RFC 1123 ist nur GMT oder Offset erlaubt
        if (timestamp.endsWith("UTC"))
            timestamp = timestamp.replaceFirst("UTC", "GMT");

        if (timestamp.endsWith("EEST"))
            timestamp = timestamp.replaceFirst("EEST", "+03:00");

            if (timestamp.endsWith("EET"))
            timestamp = timestamp.replaceFirst("EEST", "+02:00");            

        // Fängt das Datum mit einem Buchstaben an? Ja, dann ists wahrscheinlich RFC_1123
        if (Character.isLetter(timestamp.charAt(0))) {
            return LocalDate.parse(timestamp, DateTimeFormatter.RFC_1123_DATE_TIME);
        } else {
            return LocalDate.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        }
    }
}
