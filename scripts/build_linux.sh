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
# 3. Create native packages (DEB and RPM)
echo "Packaging native DEB..."
jpackage \
  --input target \
  --main-jar vibethema.jar \
  --main-class com.vibethema.Launcher \
  --type deb \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --linux-package-name "vibethema" \
  --app-version "1.0.0" \
  --vendor "Vibethema" \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --dest target/dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --verbose

echo "Packaging native RPM..."
jpackage \
  --input target \
  --main-jar vibethema.jar \
  --main-class com.vibethema.Launcher \
  --type rpm \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --linux-package-name "vibethema" \
  --app-version "1.0.0" \
  --vendor "Vibethema" \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --dest target/dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --verbose

# 4. Create AppImage (requires appimagetool on Runner)
# We need the app-image structure first
echo "Creating app-image for AppImage conversion..."
jpackage \
  --input target \
  --main-jar vibethema.jar \
  --main-class com.vibethema.Launcher \
  --type app-image \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --dest target/dist/AppDir-Base \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --verbose

echo "Build complete. Check target/dist/Vibethema for the native app image."
