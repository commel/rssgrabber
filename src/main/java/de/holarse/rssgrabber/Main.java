package de.holarse.rssgrabber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.CRC32;

import javax.mail.internet.AddressException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.holarse.rssgrabber.configs.ConfigReader;
import de.holarse.rssgrabber.feedhandler.AtomRssFeedHandler;
import de.holarse.rssgrabber.feedhandler.GitHubReleasesFeedHandler;
import de.holarse.rssgrabber.feedhandler.SteamEventsFeedHandler;
import de.holarse.rssgrabber.feedhandler.config.ConfigFile;
import de.holarse.rssgrabber.output.EmailOutputHandler;
import de.holarse.rssgrabber.output.FeedResult;
import de.holarse.rssgrabber.output.MatterbridgeOutputHandler;
import de.holarse.rssgrabber.output.OutputEntry;
import de.holarse.rssgrabber.output.OutputHandler;
import de.holarse.rssgrabber.util.GrabberException;

/**
 * Einstiegspunkt
 */
public class Main {
    
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static void main(final String[] args) throws InterruptedException  {
        logger.info("HOLARSE RSS-Feedgrabber v{}", AppConfig.VERSION);
	
        try {
            AppConfig.loadAppConfig();
        } catch (final AddressException ae) {
            logger.error("Error while loading config", ae);
            return;
        }
        
        CategoryConfig.loadConfig();

        // Verzeichnis erstellen, bevor die ganze Arbeit losgeht, falls hier was s
        // schiefläuft
	if (!Files.exists(AppConfig.getObjectDirectory().toPath())) {
	    try {
		Files.createDirectory(AppConfig.getObjectDirectory().toPath());
		logger.info("Object directory {} created", AppConfig.getObjectDirectory());
	    } catch (IOException ioex) {
		logger.error("Could not create object directory {}", AppConfig.getObjectDirectory());
		throw new RuntimeException(ioex);
	    }
	}
        
        final ExecutorService pool = Executors.newFixedThreadPool(AppConfig.getThreads());

        final List<ConfigFile> configs = ConfigReader.readAll();
        logger.info("Checking {} feeds", configs.size());

        final List<Callable<FeedResult>> tasks = new ArrayList<>(configs.size());

        for (final ConfigFile config : configs) {
            try {
                switch(config.getFeedType()) {
                case ATOM, RSS -> tasks.add(new AtomRssFeedHandler(config));
                case GITHUB -> tasks.add(new GitHubReleasesFeedHandler(config));
                case STEAM -> tasks.add(new SteamEventsFeedHandler(config));
                default -> logger.error("unhandled feedtype {} for file {}", config.getFeedType(), config.getFilename());
                }
            } catch (final Exception e) {
                logger.error("error while determining feedtype {} for file {}", config.getFeedType(), config.getFilename(), e);
            }
        }

        logger.info("Invoking all {} tasks with execution timeout of {} seconds", tasks.size(), AppConfig.getExecTimeout());
        // Darauf warten, dass alle Tasks abgearbeitet wurden
        final List<Future<FeedResult>> futureTasks = pool.invokeAll(tasks, AppConfig.getExecTimeout(), TimeUnit.SECONDS);
        // Keine neuen Tasks sind mehr zu erwarten
        logger.info("waiting for tasks to complete");
        pool.shutdown();

        final int termination_timeout = 10;
        
        logger.info("Awaiting termination within {}s", termination_timeout);
        if (!pool.awaitTermination(termination_timeout, TimeUnit.SECONDS)) {
            logger.info("Cancelling all executing tasks");
            pool.shutdownNow();
            // Wait a while for tasks to respond to being cancelled
            if (!pool.awaitTermination(termination_timeout, TimeUnit.SECONDS)) {
                logger.warn("pool did not cooperate. kill");
            }
        }

        logger.info("Recieving feed results");
        final List<FeedResult> feedResults = new ArrayList<>(futureTasks.size());

        // Feedergebnisse abfragen
        int emptyFeeds = 0;
        int canceledFeeds = 0;
        int errorFeeds = 0; 
        int updates = 0;

        final List<String> errorFeedList = new ArrayList<>(5);

        for (final Future<FeedResult> task: futureTasks) {
            try {                      
                final FeedResult fresult = task.get(AppConfig.getFeedTimeout(), TimeUnit.MILLISECONDS);
                if (fresult.getEntries().isEmpty()) {
                    emptyFeeds++;
                    continue;                    
                }

                feedResults.add(fresult);
                updates += fresult.getEntries().size();

            } catch (final InterruptedException | TimeoutException e) {
                logger.error("A Task failed to complete", e);
                canceledFeeds++;
            } catch (final ExecutionException e) {
                if (e.getCause() instanceof GrabberException ge) {
                    final String feedname = ge.getFeedname();
                    logger.error("The Task {} reported an error.", feedname, e);
                    errorFeedList.add(feedname);
                } else {
                    logger.error("A Task reported an error.", e);
                }
                errorFeeds++;
            } catch (final Exception e) {
                logger.error("A task failed hard", e);
                errorFeeds++;
            }
        }

	// Ausgaben erstmal zwischenspeichern, damit wir sie bei einem Fehlversand nicht verlieren
	// In dem Ausgabeverzeichnis werden alle gesammelt. So können auch Ergebnisse
	// die bei einem vorherigen Lauf nicht versendet werden konnten, nun
	// eventuell verschickt werden.
	for (final FeedResult fr : feedResults) {
	    for(final OutputEntry oe : fr.getEntries()) {
		var x = new CRC32();
		x.update(oe.id().getBytes());
                final File file = new File(AppConfig.getObjectDirectory(), String.format("%s-%s%s", oe.feed(), Long.toHexString(x.getValue()), AppConfig.OBJ_FILE_EXTENSION));
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
		    oos.writeObject(oe);
		} catch (IOException ioex) {
                    logger.error("Fatal problem while persisting " + file, ioex);
		}
	    }
	}

        // Ausgaben versenden
        int notifications = 0;
        final List<OutputHandler> handlers = new ArrayList<>();
        if (AppConfig.isMailActive()) {
            handlers.add(new EmailOutputHandler());
        }
        if (AppConfig.isMatterbridgeActive()) {
            handlers.add(new MatterbridgeOutputHandler());
        }

        // Jetzt alle Dateien einladen. Es könnten neben den eben erstellten
        // ja auch noch ältere dabei sein.
	logger.info("Reading object directory {}", AppConfig.getObjectDirectory());
	final List<File> objFiles = Arrays.asList(AppConfig.getObjectDirectory().listFiles((f) -> f.getName().endsWith(AppConfig.OBJ_FILE_EXTENSION)));
	logger.info("Loaded {} object files from {}", objFiles.size(), AppConfig.getObjectDirectory());

        for (final File objFile : objFiles) {
                try (final ObjectInputStream in = new ObjectInputStream( new FileInputStream(objFile))) {
                    final OutputEntry oe = OutputEntry.class.cast(in.readObject());
		    boolean notificationWasSuccessfull = false;
                    
                    for (final OutputHandler handler : handlers) {
                        try {
			    logger.info("Versende Benachrichtigung für {} an {} aus {}", oe.feed(), handler, objFile.getName());
                            handler.send(oe);
                            notifications++;

                            // Entry nun versenden. Nur wenn Email einen Fehler wirft, dann nochmal versenden. Die anderen
                            // Handler sind nur optional.
                            if (handler instanceof EmailOutputHandler) {
                                notificationWasSuccessfull = true;
                            }
                        } catch (Exception e) {
                            logger.error("Error in sending OutputEntry {} to {}", oe, handler.getClass().getSimpleName());    
                        }
                    }
                    
                    if (notificationWasSuccessfull) {
			objFile.deleteOnExit();
                    }
                } catch (IOException ioex) {
                    logger.error("Could not open or deserialize " + objFile, ioex);
                } catch (ClassNotFoundException cnfe) {
                    logger.error("Could not deserialize " + objFile, cnfe);
                }
        }

        // Statistik
        logger.info("Es wurden {} Feeds erfolgreich abgefragt. Davon haben {} Fehler gemeldet, {} wurden abgebrochen. {} hatten keine Neuigkeiten und es gab {} neue Einträge. Es wurden {} Benachrichtigungen verschickt.", 
                        configs.size(), errorFeeds, canceledFeeds, emptyFeeds, updates, notifications);
                        
        if (errorFeeds > 0) {
            var statNotification = new MatterbridgeOutputHandler();
            final String msg = String.format("{} Feeds haben Fehler gemeldet: {}", errorFeeds, String.join(",", errorFeedList));
            try {
                statNotification.send("RSSGrabber", msg);
            } catch (final Exception e) {
                logger.error("Report could not be send", e);
            }
        }
    }
}
