#!/bin/sh
# Launcher used by cutechess. Builds nothing: run `mvn -q package` first.
# Every UCI line exchanged with the GUI is appended to target/krk-engine.log; built tablebases are
# cached in target/tablebases. Extra arguments are passed to the engine, e.g. --strategy alphabeta
DIR="$(cd "$(dirname "$0")/.." && pwd)"
exec java -Xmx1g -jar "$DIR/target/krk-engine.jar" --log "$DIR/target/krk-engine.log" --tables "$DIR/target/tablebases" "$@"
