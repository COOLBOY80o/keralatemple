#!/bin/sh
# Builds the mod jar. Needs JDK 17 and an internet connection (first build downloads Forge, about 5-10 minutes).
cd "$(dirname "$0")" || exit 1
if ! java -version 2>&1 | grep -q '"17\.'; then
  echo "This needs Java 17 (Temurin JDK 17 from https://adoptium.net). Set JAVA_HOME to it and run again."
  exit 1
fi
chmod +x gradlew
./gradlew build || { echo "The build failed. Send the error text above to Claude to fix."; exit 1; }
echo
echo "Done. Your mod is build/libs/keralatemple-1.0.0.jar"
echo "Copy it into the 'mods' folder of a Minecraft 1.20.1 Forge installation."
