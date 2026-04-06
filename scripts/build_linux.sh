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

# 3. Create the base app-image
# This will be used as the source for all other package formats (DEB, RPM, AppImage).
echo "Creating base application image..."
rm -rf target/dist
mkdir -p target/dist

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

# 4. Create DEB from the app-image
echo "Packaging native DEB from base image..."
jpackage \
  --app-image target/dist/AppDir-Base/Vibethema \
  --type deb \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --linux-package-name "vibethema" \
  --app-version "1.0.0" \
  --vendor "Vibethema" \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --dest target/dist \
  --verbose

# 5. Create RPM from the app-image
echo "Packaging native RPM from base image..."
jpackage \
  --app-image target/dist/AppDir-Base/Vibethema \
  --type rpm \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --linux-package-name "vibethema" \
  --app-version "1.0.0" \
  --vendor "Vibethema" \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --dest target/dist \
  --verbose

echo "Build complete. Base image: target/dist/AppDir-Base/Vibethema"
