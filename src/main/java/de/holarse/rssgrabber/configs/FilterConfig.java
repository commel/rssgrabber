package de.holarse.rssgrabber.configs;

import java.util.ArrayList;
import java.util.List;

public class FilterConfig {
    // Filtereinträge, deren Vorhandensein sofortiges Ausschliessen bewirken
    private final List<String> mustNotFilters = new ArrayList<>(3);
    // Filtereinträge, die zwingend vorhanden sein müssen
    private final List<String> mustHaveFilters = new ArrayList<>(3);

    public FilterConfig() {
    }
    
    public FilterConfig(final List<String> mustNotFilters, final List<String> mustHaveFilters) {
        this.mustNotFilters.addAll(mustNotFilters);
        this.mustHaveFilters.addAll(mustHaveFilters);
    }

    public List<String> getMustNotFilters() {
        return mustNotFilters;
    }

    public List<String> getMustHaveFilters() {
        return mustHaveFilters;
    }
    
    public void merge(final FilterConfig other) {
        if (other == null) return;
        
        mustNotFilters.addAll(other.getMustNotFilters());
        mustHaveFilters.addAll(other.getMustHaveFilters());
    }
    
}
