package de.holarse.rssgrabber.configs;

/**
 * Filter-Typ, um Duplikate zu Feedeinträge zu finden
 * @author comrad
 */
public enum DuplicateFilterType {
    
    /** Expliziter Standardfilter */
    AUTO,
    /** Impliziter Standardfilter, wird bei AUTO verwendet */
    LINK,
    TITLE
}
