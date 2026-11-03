#!/usr/bin/env bash
# External oracles for the architecture: compiles the engine (repository root) and After/ for Java 17 into
# Oracles/target, then runs the jMolecules classical-onion and hexagonal rules on them (Oracles/ module).
# Java 17 because the jMolecules ArchUnit rules need ArchUnit 0.23, which ignores class files newer than that.
# Usage, from anywhere: scripts/oracles.sh
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root/Oracles"
mvn -q dependency:resolve > /dev/null
m2=$(cygpath -m "$HOME/.m2/repository" 2>/dev/null || echo "$HOME/.m2/repository")
cp="$m2/org/jmolecules/jmolecules-onion-architecture/1.6.0/jmolecules-onion-architecture-1.6.0.jar"
cp="$cp;$m2/org/jmolecules/jmolecules-hexagonal-architecture/1.6.0/jmolecules-hexagonal-architecture-1.6.0.jar"
for subject in engine:.. after:../After; do
  name=${subject%%:*}; dir=${subject#*:}
  out="target/${name}17"
  rm -rf "$out"; mkdir -p "$out"
  javac --release 17 -cp "$cp" -d "$out" $(find "$dir/src/main/java" -name "*.java")
  echo "$name: $(find "$out" -name '*.class' | wc -l) classes compiled for Java 17"
done
mvn -q test
echo "oracles: ok"
