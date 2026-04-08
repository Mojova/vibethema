#!/bin/bash
set -e
set -o pipefail

# Create a native macOS application bundle with an icon
echo "Starting macOS build for architecture: $(uname -m)"

# 1. Prepare icons
ICON_SOURCE="src/main/resources/icon.png"
ICONSET="target/icon.iconset"
ICNS_TARGET="src/main/resources/icon.icns"

if [ -f "$ICON_SOURCE" ]; then
    echo "Creating ICNS from $ICON_SOURCE..."
    mkdir -p "$ICONSET"
    
    # Sizes for .icns
    sips -z 16 16     "$ICON_SOURCE" --out "$ICONSET/icon_16x16.png" > /dev/null 2>&1
    sips -z 32 32     "$ICON_SOURCE" --out "$ICONSET/icon_16x16@2x.png" > /dev/null 2>&1
    sips -z 32 32     "$ICON_SOURCE" --out "$ICONSET/icon_32x32.png" > /dev/null 2>&1
    sips -z 64 64     "$ICON_SOURCE" --out "$ICONSET/icon_32x32@2x.png" > /dev/null 2>&1
    sips -z 128 128   "$ICON_SOURCE" --out "$ICONSET/icon_128x128.png" > /dev/null 2>&1
    sips -z 256 256   "$ICON_SOURCE" --out "$ICONSET/icon_128x128@2x.png" > /dev/null 2>&1
    sips -z 256 256   "$ICON_SOURCE" --out "$ICONSET/icon_256x256.png" > /dev/null 2>&1
    sips -z 512 512   "$ICON_SOURCE" --out "$ICONSET/icon_256x256@2x.png" > /dev/null 2>&1
    sips -z 512 512   "$ICON_SOURCE" --out "$ICONSET/icon_512x512.png" > /dev/null 2>&1
    sips -z 1024 1024 "$ICON_SOURCE" --out "$ICONSET/icon_512x512@2x.png" > /dev/null 2>&1

    iconutil -c icns "$ICONSET" -o "$ICNS_TARGET"
    echo "Done creating $ICNS_TARGET."
else
    echo "Icon source not found at $ICON_SOURCE"
fi

# 2. Build the JAR
echo "Building fat JAR..."
mvn clean package -DskipTests -B

# 2.1 Extract version from pom.xml (e.g., 0.9-SNAPSHOT -> 0.9.0, 0.9.0-SNAPSHOT -> 0.9.0)
RAW_VERSION=$(mvn help:evaluate "-Dexpression=project.version" -q -DforceStdout -B)
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

# 3. Create the native app
echo "Packaging native app v$VERSION with jpackage and minimal runtime..."
rm -rf target/dist
mkdir -p target/dist

jpackage \
  --input target \
  --main-jar vibethema.jar \
  --main-class com.vibethema.Launcher \
  --type dmg \
  --runtime-image target/runtime \
  --icon "$ICNS_TARGET" \
  --name "Vibethema" \
  --app-version "$VERSION" \
  --vendor "Mojova" \
  --copyright "Copyright © 2026 Mojova" \
  --description "Character management for Exalted 3rd Edition" \
  --license-file LICENSE.txt \
  --dest target/dist \
  --java-options "--enable-native-access=ALL-UNNAMED" \
  --file-associations src/main/resources/vbtm.properties \
  --verbose

echo "Build complete. Check target/dist/Vibethema.app for the native bundle."
