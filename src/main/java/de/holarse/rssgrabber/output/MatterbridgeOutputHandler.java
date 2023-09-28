package de.holarse.rssgrabber.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.holarse.rssgrabber.AppConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatterbridgeOutputHandler implements OutputHandler {

    private final static Logger logger = LoggerFactory.getLogger(MatterbridgeOutputHandler.class);        
    
    @Override
    public void send(final OutputEntry entry) throws InterruptedException, JsonProcessingException {
        final ObjectMapper jsonMapper = new ObjectMapper();
        final ObjectNode node = jsonMapper.createObjectNode();
        node.put("username", String.format("Feed-Update (%s)", entry.feed()));
        node.put("avatar", AppConfig.getMatterbridgeAvatar());
        node.put("gateway", AppConfig.getMatterbridgeGateway());
        node.put("text", String.format("%s - %s", OutputUtils.sanatize(entry.title(), TitleAdjustOption.TRIM), entry.link()));
        
        final String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        send(entry.feed(), json);        
    }

    public void send(final String feed, final String message) throws InterruptedException, JsonProcessingException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(AppConfig.getMatterbridgeDomain()))
                                               .setHeader("Content-Type", "application/json")
                                               .setHeader("Authorization", "Bearer " + AppConfig.getMatterbridgeToken())
                                               .POST(HttpRequest.BodyPublishers.ofString(message))
                                               .build();
        
        //Execute and get the response.
         final HttpClient httpClient = HttpClient.newBuilder()
                                                 .version(HttpClient.Version.HTTP_2)
                                                 .connectTimeout(Duration.ofSeconds(10))
                                                 .build();

         try {
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.debug("Response on feed {}: {}", feed, response.statusCode());
         } catch (final IOException ioex) {
             logger.error("Error while sending to matterbridge", ioex);
             throw new InterruptedException(ioex.getMessage());
         }

        logger.debug("Send matterbridge api message {} for feed {}", message, feed);
    }
    
}
