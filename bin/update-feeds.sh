#!/bin/bash

RSS_TEMP_DIR=$(mktemp -d)

git clone --depth 1 --branch master --single-branch ssh://git@git.holarse-linuxgaming.de:9922/Holarse-Linuxgaming/rssgrabber.git "$RSS_TEMP_DIR"

# feeds aktualisieren
rsync -ahv "$RSS_TEMP_DIR"/feeds/ /home/drueckblick/rssgrabber/feeds --delete
# categories aktualisieren
rsync -ahv "$RSS_TEMP_DIR"/config/categories.properties /var/holarse/rssgrabber/ --delete

rm -f "$RSS_TEMP_DIR"
