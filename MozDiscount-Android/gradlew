#!/bin/sh
# gradlew de arranque: descarrega o Gradle 8.7 (necessario para o plugin Android 8.5.2) e executa-o.
# Serve para fluxos de trabalho que fazem "chmod +x gradlew && ./gradlew ...".
set -e
GRADLE_VERSION=8.7
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/bootstrap"
DIST="$BASE/gradle-$GRADLE_VERSION"
if [ ! -x "$DIST/bin/gradle" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "A descarregar o Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then curl -fsSL "$URL" -o "$ZIP"; else wget -q "$URL" -O "$ZIP"; fi
  unzip -q -o "$ZIP" -d "$BASE"
  rm -f "$ZIP"
fi
exec "$DIST/bin/gradle" "$@"
