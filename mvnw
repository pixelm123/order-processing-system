#!/bin/sh
# Maven wrapper script for Unix/Linux/macOS/Docker
set -e

MAVEN_PROJECTBASEDIR=$(dirname "$0")
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

DISTRIBUTION_URL=$(grep distributionUrl "$WRAPPER_PROPERTIES" | cut -d= -f2)
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-3.9.9"

if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
    echo "Downloading Maven 3.9.9..."
    mkdir -p "$HOME/.m2/wrapper/dists"
    curl -fsSL "$DISTRIBUTION_URL" -o /tmp/maven.zip
    unzip -q /tmp/maven.zip -d "$HOME/.m2/wrapper/dists/"
    mv "$HOME/.m2/wrapper/dists/apache-maven-3.9.9-bin/apache-maven-3.9.9" "$MAVEN_HOME" 2>/dev/null || true
    rm /tmp/maven.zip
fi

export PATH="$MAVEN_HOME/bin:$PATH"
exec mvn "$@"
