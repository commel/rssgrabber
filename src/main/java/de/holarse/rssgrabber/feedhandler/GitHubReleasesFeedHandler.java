package de.holarse.rssgrabber.feedhandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.holarse.rssgrabber.AppConfig;
import de.holarse.rssgrabber.cache.FeedCacheEntry;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;
import de.holarse.rssgrabber.feedhandler.config.GithubConfig;
import java.io.IOException;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
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
 * Lädt die Feedeinträge anhand der GitHub-Releases
 * https://docs.github.com/en/rest/reference/releases#list-releases
 *
 * @author comrad
 */
public class GitHubReleasesFeedHandler extends AbstractFeedHandler<GithubConfig> {

    private final static Logger logger = LoggerFactory.getLogger(GitHubReleasesFeedHandler.class);

    // https://docs.github.com/en/rest/reference/releases#list-releases
    private final static String REL_FORMAT = "https://api.github.com/repos/%s/%s/releases?page=1&per_page=20";  // JSON 
    private final static String TAG_FORMAT = "https://github.com/%s/%s/tags.atom"; // XML+ATOM
    
    protected static enum GithubUrlPartial {
        RELEASES, TAGS
    }
       
    private final GithubUrlPartial githubFeedMode;

    final String basicAuthBase64 = Base64.getEncoder().encodeToString((AppConfig.getGithubUsername() + ":" + AppConfig.getGithubToken()).getBytes());

    protected static GithubUrlPartial getGithubFeedMode(final String configValue) {
        if (configValue.endsWith("tags.atom") || configValue.endsWith("/tags")) {
            return GithubUrlPartial.TAGS;
        } else {
            return GithubUrlPartial.RELEASES;
        }        
    }
    
    public GitHubReleasesFeedHandler(final ConfigFile configFile) throws Exception {
        super(GithubConfig.class, configFile);
        githubFeedMode = getGithubFeedMode(getConfig().getSource());
    }    
    
    /**
     * Hier laden wir das Release-Format aus der Github-API in JSON
     * @param name
     * @param is
     * @return 
     */
    protected List<FeedCacheEntry> parseWorkReleaseEntries(final String name, final InputStream is) throws Exception {
        // JSON einlesen
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode root = mapper.readTree(is);

        final List<FeedCacheEntry> result = new ArrayList<>(25);

        final Iterator<JsonNode> it = root.elements();
        while (it.hasNext()) {
            final JsonNode node = it.next();
            logger.trace("Parsing {}", node);
            
            //
            // Prüfung
            //
            if (node.get("prerelease").asBoolean()) {
                if (!getConfig().isPrereleasesAllowed()) {
                    logger.debug("Feed {}: Eintrag {} ignoriert weil Pre-Release", name, node.get("node_id").asText("???"));
                    continue;
                }
            }            
            
            // Daten sammeln
            
            // Prüfen, ob der Name gesetzt ist und sonst auf tag_name ausweichen
            String eventName = node.get("name").asText("");
            if (eventName.isBlank()) {
                eventName = node.get("tag_name").asText("");
            }            
            
            final String entryLink = node.get("html_url").asText("");
            final LocalDate timestamp = ZonedDateTime.parse(node.get("published_at").asText("")).toLocalDate();
            
            logger.debug("Checking live feed '{}' with entry '{}' for unique link '{}'.", name, eventName, entryLink);

            final FeedCacheEntry fce = new FeedCacheEntry();
            fce.setTitle(eventName);
            fce.setLink(entryLink);
            fce.setTimestamp(timestamp);

            fce.setUuid(getUniqueReference(fce));  // Funktioniert, sobald Title und Link gesetzt sind

            result.add(fce);
        }

        return result;        
    }
    
    /**
     * Hier laden wir den ATOM-Feed in XML. Der JSON-Feed aus der API zu den Tags enthält nur sehr wenige Informationen. Hauptsächlich fehlt auch
     * ein Feld mit dem Zeitstempel, damit wir es chronologisch einordnen können.
     * @param name
     * @param is
     * @return
     * @throws Exception 
     */
    protected List<FeedCacheEntry> parseWorkTagEntries(final String name, final InputStream is) throws Exception {
        final List<FeedCacheEntry> result = new ArrayList<>(25);   
        
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();   
        final Document document = builder.parse(is);

        final XPath atomPath = XPathFactory.newInstance().newXPath();  
        final NodeList nodeList = (NodeList) atomPath.compile("/feed/entry").evaluate(document, XPathConstants.NODESET);        
        
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

            result.add(fce);
        }        
        
        return result;
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
    
    @Override
    protected List<FeedCacheEntry> parseWorkEntries(final String name, final InputStream is) throws Exception {
        return switch (getGithubFeedMode(getConfig().getSource())) {
            case RELEASES -> parseWorkReleaseEntries(name, is);
            case TAGS -> parseWorkTagEntries(name, is);
        };
    }
    
    
    /**
     * Öffnet eine Http-Verbindung zum Feed
     * @param feedUrl
     * @return
     * @throws IOException
     * @throws java.net.URISyntaxException
     * @throws java.lang.InterruptedException
     */
    @Override
    protected InputStream getLiveFeedStream(final String feedUrl) throws IOException, IllegalArgumentException, URISyntaxException, InterruptedException {
        logger.info("Loading feed via github");
        final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                                                         .version(HttpClient.Version.HTTP_2)
                                                         .followRedirects(HttpClient.Redirect.NORMAL)
                                                         .build();
        
        final Builder builder = HttpRequest.newBuilder(URI.create(feedUrl))
                                               .GET();
        
        switch(githubFeedMode) {
            case RELEASES -> {
                builder.setHeader("Authorization", "Basic " + basicAuthBase64)
                       .setHeader("User-Agent", getHttpUserAgent())
                       .setHeader("Accept", "application/vnd.github.v3+json"); // Siehe https://docs.github.com/en/rest/overview/media-types
            }
            case TAGS -> {
                builder.setHeader("Accept", "application/xml");
            }
        }
        
        final HttpRequest request = builder.build();

        final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        logger.debug("Loading feed {} as: {} with return code {}", getConfig().getName(), request.uri(), response.statusCode());

        if (response.statusCode() >= 400) {
            logger.error("Error Status with feed {}: {}", feedUrl, response.statusCode());
            throw new IOException("Illegal feed result code 200 != " + response.statusCode());
        }
        
        return response.body();
    }    


    @Override
    protected String getFeedConnectionUrl() {
        return buildGithubUrl(getConfig().getSource(), githubFeedMode);
    }
   
    public static String buildGithubUrl(final String configValue, final GithubUrlPartial githubFeedMode) {
        String baseUrl;
        
        if (configValue.toLowerCase().startsWith("https://github.com/")) {
            baseUrl = configValue.substring(19);
        } else {
            baseUrl = configValue;
        }
        
        final String[] items = baseUrl.split("/");
        if (items.length >= 2) {
            return switch (githubFeedMode) {
                case RELEASES -> String.format(REL_FORMAT, items[0], items[1]);
                case TAGS -> String.format(TAG_FORMAT, items[0], items[1]);
            };
        }

        return null;
    }
}
