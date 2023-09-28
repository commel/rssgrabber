package de.holarse.rssgrabber.util;

import de.holarse.rssgrabber.configs.FilterConfig;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 *
 * @author comrad
 */
public class FeedUtils {
  
    public static FilterConfig parseFilters(final String filter) {
        final FilterConfig fc = new FilterConfig();
        
        if (StringUtils.isBlank(filter)) {
            return fc;
        }
      
        // Filter trennen und aufteilen in MustNot und MustHave-Filter
        for (final String f: filter.split(",")) {
            final String _f = f.trim().toLowerCase();
            if (_f.isBlank()) {
                continue;
            }
            
            if (_f.charAt(0) == '!' && _f.length() > 1) {
                fc.getMustNotFilters().add(_f.substring(1));
                continue;
            }
            
            fc.getMustHaveFilters().add(_f);
        }        
        
        return fc;
    }
    
    public static String normalizeText(final String text) {
        return StringEscapeUtils.unescapeHtml4(text.trim().toLowerCase());
    }    
    
    private FeedUtils() {}
}
