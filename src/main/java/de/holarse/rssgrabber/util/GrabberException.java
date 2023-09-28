package de.holarse.rssgrabber.util;

public class GrabberException extends Exception {
    
    private final String feedname;

    public GrabberException(final String feedname, final Throwable cause) {
        super(cause);
        this.feedname = feedname;
    }

    public GrabberException(final String feedname, final String message) {
        super(message);
        this.feedname = feedname;        
    }

    public GrabberException(final String feedname, final String message, final Throwable cause) {
        super(message, cause);
        this.feedname = feedname;        
    }

    public String getFeedname() {
        return feedname;
    }
    
}
