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

# 2.1 Extract version from pom.xml (e.g., 0.9-SNAPSHOT -> 0.9.0, 0.9.0-SNAPSHOT -> 0.9.0)
RAW_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
VERSION=$(echo "$RAW_VERSION" | sed 's/-SNAPSHOT//')
DOTS=$(echo "$VERSION" | tr -cd '.' | wc -c | xargs)
if [ "$DOTS" -lt 2 ] && [[ "$RAW_VERSION" == *"-SNAPSHOT"* ]]; then
    VERSION="${VERSION}.0"
fi

# 2.2 Create a minimal JRE with jlink
echo "Creating minimal runtime with jlink..."
rm -rf target/runtime
jlink \
  --add-modules java.base,java.desktop,java.management,java.naming,java.scripting,java.sql,java.xml,java.logging,jdk.jfr,jdk.unsupported \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress zip-9 \
  --output target/runtime

# 3. Create the base app-image
# This will be used as the source for all other package formats (DEB, RPM, AppImage).
echo "Creating base application image v$VERSION with minimal runtime..."
rm -rf target/dist
mkdir -p target/dist

jpackage \
  --input target \
  --main-jar vibethema.jar \
  --main-class com.vibethema.Launcher \
  --type app-image \
  --runtime-image target/runtime \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --app-version "$VERSION" \
  --vendor "Mojova" \
  --copyright "Copyright © 2026 Mojova" \
  --description "Character management for Exalted 3rd Edition" \
  --license-file LICENSE.txt \
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
  --app-version "$VERSION" \
  --vendor "Mojova" \
  --copyright "Copyright © 2026 Mojova" \
  --description "Character management for Exalted 3rd Edition" \
  --license-file LICENSE.txt \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --linux-package-deps "libgtk-3-0, libasound2, libpango-1.0-0, libfreetype6, libfontconfig1, libx11-6, libxext6, libxi6, libxrender1, libxtst6" \
  --dest target/dist \
  --file-associations src/main/resources/vbtm.properties \
  --verbose

# 5. Create RPM from the app-image
echo "Packaging native RPM from base image..."
jpackage \
  --app-image target/dist/AppDir-Base/Vibethema \
  --type rpm \
  --icon "$ICON_SOURCE" \
  --name "Vibethema" \
  --linux-package-name "vibethema" \
  --app-version "$VERSION" \
  --vendor "Mojova" \
  --copyright "Copyright © 2026 Mojova" \
  --description "Character management for Exalted 3rd Edition" \
  --license-file LICENSE.txt \
  --linux-shortcut \
  --linux-menu-group "Utility" \
  --linux-package-deps "libgtk-3-0, pango, freetype, fontconfig, alsa-lib, libX11, libXext, libXi, libXrender, libXtst" \
  --dest target/dist \
  --file-associations src/main/resources/vbtm.properties \
  --verbose

echo "Build complete. Base image: target/dist/AppDir-Base/Vibethema"
