package de.holarse.rssgrabber.output;

import java.io.Serial;
import java.time.LocalDate;

public record OutputEntry(String feed,
			  String id,
			  String title,
			  String link,
			  String category,
			  String changelog,
			  LocalDate timestamp) implements java.io.Serializable {

    @Serial
    private static final long serialVersionUID = 2L;
};
