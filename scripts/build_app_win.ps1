# Windows Build Script for Vibethema

$ICON_SOURCE = "src/main/resources/icon.png"
$ICO_TARGET = "src/main/resources/icon.ico"

# 1. Prepare icons (requires ImageMagick or equivalent)
if (Test-Path $ICON_SOURCE) {
    Write-Host "Converting icon to ICO..."
    # GitHub Runners have ImageMagick installed.
    # Convert PNG to ICO with multiple sizes for Windows support.
    magick convert $ICON_SOURCE -define icon:auto-resize=16,32,48,64,128,256 $ICO_TARGET
} else {
    Write-Warning "Icon source not found at $ICON_SOURCE"
}

# 2. Build the JAR
Write-Host "Building fat JAR..."
mvn clean package -DskipTests

# 2.1 Extract version from pom.xml (e.g., 0.9-SNAPSHOT -> 0.9.0, 0.9.0-SNAPSHOT -> 0.9.0)
$RAW_VERSION = (mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
$VERSION = $RAW_VERSION.Replace("-SNAPSHOT", "")
$DOTS = ($VERSION.ToCharArray() | Where-Object { $_ -eq '.' }).Count
if ($DOTS -lt 2 -and $RAW_VERSION.Contains("-SNAPSHOT")) {
    $VERSION = "${VERSION}.0"
}

# 3. Create the native app
Write-Host "Packaging native app v$VERSION with jpackage..."
if (Test-Path "target/dist") { Remove-Item -Recurse -Force "target/dist" }
New-Item -ItemType Directory -Path "target/dist" | Out-Null

jpackage `
  --input target `
  --main-jar vibethema.jar `
  --main-class com.vibethema.Launcher `
  --type msi `
  --icon $ICO_TARGET `
  --name "Vibethema" `
  --dest target/dist `
  --vendor "Mojova" `
  --copyright "Copyright © 2026 Mojova" `
  --description "Character management for Exalted 3rd Edition" `
  --app-version "$VERSION" `
  --win-shortcut `
  --win-menu `
  --win-shortcut-prompt `
  --win-dir-chooser `
  --win-upgrade-uuid "74c10a48-4c8d-4a1e-8e6d-5b3a2a1c0f9d" `
  --java-options "--enable-native-access=ALL-UNNAMED" `
  --file-associations src/main/resources/vbtm.properties `
  --verbose

Write-Host "Build complete. Check target/dist for the MSI installer."
