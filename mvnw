#!/bin/sh
# Maven Wrapper script for Unix-like systems
# https://github.com/apache/maven-wrapper

set -e

if [ -z "$JAVA_HOME" ]; then
  JAVACMD="java"
else
  JAVACMD="$JAVA_HOME/bin/java"
fi

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd "$(dirname "$0")" && pwd)}"
MAVEN_WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Download maven-wrapper.jar if not present
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
  echo "Downloading Maven Wrapper..."
  if command -v curl > /dev/null 2>&1; then
    curl -sL -o "$MAVEN_WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
  elif command -v wget > /dev/null 2>&1; then
    wget -q -O "$MAVEN_WRAPPER_JAR" "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
  else
    echo "Error: curl or wget is required to download Maven Wrapper"
    exit 1
  fi
fi

exec "$JAVACMD" \
  -jar "$MAVEN_WRAPPER_JAR" \
  "$@"
