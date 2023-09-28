package de.holarse.rssgrabber.output;

import de.holarse.rssgrabber.AppConfig;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author comrad
 */
public class EmailOutputHandler implements OutputHandler {

    private final static Logger logger = LoggerFactory.getLogger(EmailOutputHandler.class);    
    
    @Override
    public void send(final OutputEntry entry) throws EmailException {
        final String entryDateStr = entry.timestamp().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));                     

        // Content zusammenstellen
        final StringBuilder buffer = new StringBuilder();
        buffer.append(String.format("=== %s ===\n", entry.category()));
        buffer.append(String.format("* [[%s]] %s. [%s Link]", entry.feed(), OutputUtils.sanatize(entry.title()), entry.link()));
        if (StringUtils.isNotEmpty(entry.changelog())) {
            buffer.append(String.format(" ([%s Changelog])", entry.changelog()));
        }
        
        final Email email = new SimpleEmail();
        email.setFrom(AppConfig.getSender().getAddress(), "Holarse Drückblick");
        email.setHostName("localhost");
        email.setTo(AppConfig.getRecipients());
        email.setSubject(String.format("Drückblick-Feed-Beitrag %s: %s | #%s | %s", entry.feed(), entry.title(), AppConfig.getWeekOfYear(), entryDateStr));
        email.setMsg(buffer.toString());
        
        email.send();
        
        logger.debug("Send mail {} for feed {} with link {}", email.getSubject(), entry.feed(), entry.link());    
    }

     
}
