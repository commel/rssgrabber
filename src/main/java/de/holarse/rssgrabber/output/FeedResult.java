package de.holarse.rssgrabber.output;

import de.holarse.rssgrabber.cache.FeedCacheEntry;
import de.holarse.rssgrabber.feedhandler.config.Config;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Einfacher Container, damit wir die Daten aus dem Feed
 * auch wieder mit ein wenig Metainformationen aus
 * dem Future holen können.
 */
public class FeedResult {

	private final String name;
	private final List<OutputEntry> entries = new ArrayList<>(25);
	private final boolean error;

	public FeedResult(final Config config, final List<FeedCacheEntry> cacheEntries, final boolean error) {
		this.name = config.getName();
		this.error = error;
               
                entries.addAll(
			       cacheEntries.stream().map(ce -> new OutputEntry(name,
									       ce.getUuid(),
									       ce.getTitle(), 
									       ce.getLink(), 
									       (ce.getCategory().isEmpty() ? config.getCategory() : ce.getCategory()), 
									       config.getChangelog().orElse(null),
									       ce.getTimestamp())
							 ).collect(Collectors.toList())
			       );
	}

	public String getName() {
		return name;
	}

	public Collection<OutputEntry> getEntries() {
		return Collections.unmodifiableCollection(entries);
	}

	public boolean isError() {
		return error;
	}
}
