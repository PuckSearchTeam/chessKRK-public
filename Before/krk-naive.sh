#!/bin/sh
# Launcher for cutechess. Run `mvn -q package` in this directory first.
DIR="$(cd "$(dirname "$0")" && pwd)"
exec java -jar "$DIR/target/krk-naive.jar"
