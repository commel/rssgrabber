package de.holarse.rssgrabber.output;

/**
 * Kümmert sich um den Versand der Neuigkeit an externe Stellen
 * @author comrad
 */
public interface OutputHandler {
    
    void send(final OutputEntry entry) throws Exception;
    
}
