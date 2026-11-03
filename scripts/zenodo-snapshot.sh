#!/usr/bin/env bash
# The snapshot archived on Zenodo: the committed tree of this repository at a commit, as a zip. Run it in the public
# repository, so that the archive holds exactly what is published there.
# Usage, from anywhere: scripts/zenodo-snapshot.sh [commit]   (default HEAD; the tree must be committed)
# Writes target/zenodo/chessKRK-<short commit>.zip with a top-level chessKRK/ directory.
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root"
commit=$(git rev-parse --short "${1:-HEAD}")
mkdir -p target/zenodo
out="target/zenodo/chessKRK-$commit.zip"
git archive --format=zip --prefix=chessKRK/ -o "$out" "$commit"
echo "$out: $(unzip -l "$out" | tail -1 | awk '{print $2}') files, $(wc -c < "$out") bytes"
