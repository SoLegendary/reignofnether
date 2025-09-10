#!/bin/bash

# Helper script to build with Java 21
export JAVA_HOME=/opt/homebrew/opt/openjdk@21

echo "Building Reign of Alpha with Java 21..."
echo "Java version: $(java -version)"

./gradlew "$@"