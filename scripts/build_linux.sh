#!/bin/bash

# Linux Build Script for Vibethema

# 1. Icons
# Linux jpackage uses PNG directly for the desktop entry and taskbar.
ICON_SOURCE="src/main/resources/icon.png"

if [ ! -f "$ICON_SOURCE" ]; then
    echo "Icon source not found at $ICON_SOURCE"
    exit 1
fi

# 2. Build the JAR
echo "Building fat JAR..."
mvn clean package -DskipTests

# 3. Create the native app
echo "Packaging native app with jpackage..."
rm -rf target/dist
mkdir -p target/dist

# On Linux, jpackage --type app-image creates a directory structure.
# We include --linux-shortcut to ensure meta-data for desktop integration is ready.
jpackage \
  --input target \
  --main-jar vibethema.jar \
  --main-class com.vibethema.Launcher \
  --type app-image \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --dest target/dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --verbose

echo "Build complete. Check target/dist/Vibethema for the native app image."
