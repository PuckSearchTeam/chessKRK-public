#!/usr/bin/env bash
# Runs Puck on one or more project directories in a single IDE session, each twice: with its decouple.wld as
# written, and with the optional block (the lines between "// BEGIN optional" and "// END optional")
# commented out. Keeps both logs next to each file as puck-with-optional.log and puck-without-optional.log
# (puck.log is the with-optional one). Usage, from anywhere:
#   [PUCK_JDK=<jdk name or home>] scripts/puck-both.sh <project dir>...
# The JDK defaults to PUCK_JDK, then JAVA_HOME, then the name "21" registered in the sandbox.
set -euo pipefail
[ $# -ge 1 ] || { echo "usage: scripts/puck-both.sh <project dir>..." >&2; exit 1; }
puck=$(cd "$(dirname "$0")/../Puck" && pwd)
case "${OSTYPE:-}" in
  msys*|cygwin*)
    export JAVA_HOME=${JAVA_HOME:-"C:/Program Files/Java/jdk-21"}
    gradlew=./gradlew.bat
    winpath() { cygpath -m "$1" 2>/dev/null || echo "$1"; } ;;
  *)
    gradlew=./gradlew
    winpath() { echo "$1"; } ;;
esac
jdk=${PUCK_JDK:-${JAVA_HOME:-21}}
args="puck;--jdk=$(winpath "$jdk");--both"
for d in "$@"; do args="$args;$(winpath "$(cd "$d" && pwd)")"; done
(cd "$puck" && $gradlew runIde --console=plain "-PideArgs=$args" > "$puck/run-puck-both.log" 2>&1) || { echo "run failed, see $puck/run-puck-both.log" >&2; exit 1; }
for d in "$@"; do
  echo "== $d"
  if [ -f "$d/puck-without-optional.log" ]; then
    for v in with without; do echo "  $v optional: $(grep -E 'Total Number of violations' "$d/puck-$v-optional.log")"; done
  else
    echo "  $(grep -E 'Total Number of violations' "$d/puck.log") (no optional block)"
  fi
  grep -h "^Axis mismatches" "$d/puck.log" | sed 's/^/  /'
done
