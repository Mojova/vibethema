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
  --main-jar exalted-builder-1.0-SNAPSHOT.jar \
  --main-class com.vibethema.Launcher \
  --type app-image \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --dest target/dist \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --file-associations src/main/resources/vbtm.properties \
  --verbose

echo "Build complete. Check target/dist/Vibethema for the native app image."
