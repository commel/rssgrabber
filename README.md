# Holarse RSS-Grabber

Ein RSS/Atom-Feed-Grabber, der uns per Mail bei Neuigkeiten benachrichtigt. Dient als Ersatz für [IFTTT](https://ifttt.com/). Es ist als Aufruf-Task konzipiert und nicht als laufender Dauerprozess. Das Feed-Parsing übernahm zuerst die Bibliothek [ROME](https://rometools.github.io/rome/), die verschiedenen RSS- und Atom-Versionen konsumieren kann, aber auch Steam-JSON-Feeds. Jetzt machen wir das über einfaches XML-Parsing mit xpath über jackson. Auch json wird darüber geparst.

## Basis-Konfiguration
Folgenden wird die BAsis-Konfiguration erläutert. Alle Werten mit "Optional" enthalten ihren Standardwert, Werte ohne diese Erwähnung sind Pflichtwerte und die Werte ein Vorschlag.


    # Pflicht, Mail-Adresse an die die Benachrichtigung geschickt wird; kann durch Semikolon getrennt erweitert werden.
    recipients=contact@holarse-linuxgaming.de
    
    # Optional, Absender-Adresse
    sender = noreply@holarse-linuxgaming.de

    # Optional, Maximale parallele Prozesse
    parallel=5

    # Verzeichnis mit den Konfigurationsdateien
    config_dir=feeds

    # Verzeichnis mit den zwischengespeicherten Feeds
    cache_dir=cache

    # Optinal, Wenn true, werden Mails verschickt
    mail_active=false

    # Optionaler Wert, um beim Testen den Cache zu ignorieren
    use_cache=true

    # Optinal, Matterbridge-Anbindung fürs Posten ins IRC/Discord
    matterbridgeActive=false
    matterbridgeAvatar=https://holarse.de/misc/feed.png
    matterbridgeDomain=127.0.0.1:4242/api/message
    matterbridgeGateway=Bridgette
    matterbridgeToken=

    # Optional, Timeout je Einzeltask in Millisekunden
    feed_timeout=250

    # Optional, Gesamte Feedausführung pro Aufruf per systemd-Timer, in Sekunden
    exec_timeout=60

    # Globale Filter, getrennt durch Kommata. Negierung durch vorangestelltes Ausrufezeichen
    filter=!Steam Awards

    # Maximale Anzahl an neuen Elementen je Feed
    max_elements = 3

    # Maximales Alter von News-Beiträgen in Tagen. 
    max_age = 3

    # Standard Event-ID Filter für Steam
    steam_eventids=10,12,13,14,15

    # Github-API
    github_user=xxx
    github_token=ghp_abcd....
    github_defaultcategory=OPENSOURCE

## Kategorie-Konfiguration
Jeder Feed kann in eine bestimmte Kategorie gesteckt werden. Dazu trägt man in der Feed-Konfiguration im Feld ```category``` den Key der Kategorie ein. Diese wird 
dann über die ```categories.properties``` in einen entsprechenden Namen für das Pad übersetzt.

Die Steam-Feeds haben EventIds, diese werden über ```STEAM_$EVENTID``` in einen Kategorie-Key gemappt und dann entsprechend in einen Namen für das Pad übersetzt.

## Feeds
### Allgemein
Jede Webseite erhält ihre eigene Datei. Der Dienst lädt alle Dateien der Reihe nach und prüft die enthaltenen URLs. Eine Datei enthält folgendes Property-Schema:

    name=Name des Eintrags im Wiki
    url=Adresse RSS-Feed
    filter=optionaler Textfilter, kann weggelassen werden.
    duplicate=AUTO|LINK|TITLE (optional, standardmäßig ist AUTO, was auf LINK zeigt)
    feedtype=AUTO (optional, wird ermittelt)
    fake_useragent=false (optional, bei true gibt sich der RSSGrabber als Firefox aus)
    max_elements=3 (optional, limitiert neue Einträge)
    max_age= (optinal, maximales Alter von News in Tagen. Wenn hier angegeben überschreibt es den Standard auf der Basis-Konfiguration)
    category= (optional, Standard aus CategoryConfiguration)
    changelog= (optional, Adresse wird in der Mail mit angegeben, wenn gesetzt)

Die Eintragung der Filter erfolgt kommagetrennt. Der Filter wird in zwei Gruppen aufgeteilt. Erstmal sind alle eingetragenen Werte sind ```must-have```-filter. Diese müssen also im Feedeintrag (aktuell nur der Titel) vorkommen. Steht dem Filtereintrag ein
Ausrufezeichen (!) vor, so wird dieser Eintrag in die zweite Gruppe, die ```must-not```-Filter übernommen. Wenn dieser Wert auftaucht, dann wird der Feedeintrag verworfen.

Bei ```max_elements``` werden nur die ersten n Elemente der neuen Elemente berücksichtigt und ggf. benachrichtigt. Wichtig ist, dass bei Atom und Steam oben die neusten Einträge stehen. Diese werden hier dann benutzt.

Die ```category``` ist optional und kann angegeben werden, um den Feed für den Drückblick unterstützend in eine Kategorie einzusortieren.

Das Namensschema lautet ```NAME.properties```. Es werden beim Einlesen nur Dateien mit der Endung *.properties* berücksichtigt. So können durch Umbenennung einzelne Feeds deaktiviert werden.

Die Feeds werden bei Programmstart eingelesen. Wird ein neuer Feed hinterlegt, oder ein Feed deaktiviert, wird diese Änderung erst beim nächsten Programmstart berücksichtigt werden.

Der Feedtype kann angegeben werden, sonst wird er durch die Anwesenheit von spezifischen Keys ermittelt.

Der Name des Eintrags kann auch als das Innere eines Wiki-Links interpretiert werden. Nutzt dazu zum Beispiel folgende Syntax:

    name= Ein Titel: Mit Doppelpunkt

oder auch mit alternativem Namen

    name=Wikiname|Label

### GitHub
Um einen GitHub-Eintrag anzulegen, reicht es, wenn ihr folgende Definition hinterlegt:

    name=Name des Eintrags
    url=https://github.com/owner/repository

Es gibt noch die Konfigurationsoption

    allow_prereleases=false

Dieser Wert ist optional, standardmäßig ist immer "false". Sollen prereleases mit berücksichtigt werden, muss der Wert auf ```true``` gestellt werden. 

Es werden allerdings auch die alten releases.atom-Urls unterstützt. Hier muss also nichts geändert werden. Diese Konfiguration wird immer erkannt, wenn ```github.com``` in der URL enthalten ist.

### Steam
Um einen Steam-Eintrag anzulegen, reicht es, wenn ihr folgende Definition hinterlegt:

    name=Name des Eintrags
    steamid=1234567

optional könnt ihr hier auch die event-ids hinterlegen:

    eventids=10,12,13,14,15 (optional, Standardwerte)

Diese Event-Ids werden immer standardmäßig hinterlegt. Wenn ihr Event-Ids angebt, dann ersetzen die immer vollständig den Eintrag, eine zusätzliche Event-Id 28 müsste also wie folgt angelegt werden:

    eventids=10,12,13,14,15,28

| EventID | Bedeutung |
|:-------:|-----------|
| 10 | Game Release |
| 11 | Live-Stream / Broadcast |
| 12 | Small Update / Patch Notes |
| 13 | Regular Update |
| 14 | Major Update |
| 15 | DLC Release |
| 20 | Game Discount |
| 21 | Item or DLC Discount |
| 26 | Contest |
| 28 | News |
| 29 | Beta-Veröffentlichung |
| 30 | Game Discount |
| 31 | Free Trial |
| 34 | Übergreifende Aktion |
| 35 | Event im Spiel |

Die EventId wird in eine Kategorie umgemünzt, die dann als Kategorie für das Pad verwendet werden kann. Wenn das nicht passieren soll und man z.B. die
statische Feed-Kategorie der Feed-Propertydatei verwenden will, muss man für diesen Feed

    use_feedcategory=false

setzen. Wird hier ein "category" gesetzt, so überschreibt dieser Wert den Wert aus dem Steam-Feed.

### HTML-Selector
Dokumente, die Werte über einen JQuery-ähnlichen Selector ermitteln, benötigen als 
zusätzlichen Wert noch das Feld ```select```. Die Syntax für das Select ist der JSoup-
Dokumentation zu entnehmen: [Link](https://jsoup.org/cookbook/extracting-data/selector-syntax)

    name=Name des Eintrags
    url=https://....
    select=img[src$=.png]
    linkAttr=abs:src

Über das ```linkAttr``` wird innerhalb eines ermittelten Eintrag festgelegt, was den Link darstellen soll.

Die Konfiguration wird anhand des select=-Keys erkannt und automatisch zugeordnet.

## Auswertung
Wird ein Feed zum ersten Mal heruntergeladen, so existiert noch keine Cache-Datei. Daher werden alle Einträge, die die Filterung und die Altersprüfung überstehen, als neu verstanden. Beim zweiten Durchlauf, sofern dann eine Cache-Datei vorliegt, werden die jeweiligen Unique Identifiers des Feeds mit den UUID-Werten im Cache verglichen. Tritt der Eintrag dort nicht auf, so ist er noch nicht ermittelt worden und wird zur Benachrichtigung vorgesehen.

Im Anschluss wird die Cache-Datei mit den alten und neuen Werten zurückgeschrieben.

## Release
Ein Release erzeugt man, indem in der pom.xml die neue Versionsnummer hinterlegt wird. Die fertig erstelle Jar-Datei muss dann auf den Server übertragen werden. Der Build wird angestossen mit:

	mvn clean package

