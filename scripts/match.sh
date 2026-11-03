#!/bin/sh
# Engine vs engine match from the openings in scripts/krk-openings.epd.
# Usage: [STRATEGY_A=tablebase] [STRATEGY_B=alphabeta] scripts/match.sh [games-per-opening] [cutechess-cli path]
# Strategies: tablebase (default), alphabeta, random.
DIR="$(cd "$(dirname "$0")/.." && pwd)"
ROUNDS="${1:-1}"
CLI="${2:-/Users/cbesse/git/cutechess/build/cutechess-cli}"
A="${STRATEGY_A:-tablebase}"
B="${STRATEGY_B:-tablebase}"
exec "$CLI" \
  -engine name="KRK-$A" cmd="$DIR/scripts/krk-engine.sh" option.Strategy="$A" \
  -engine name="KRK-$B" cmd="$DIR/scripts/krk-engine.sh" option.Strategy="$B" \
  -each proto=uci tc=inf st=2 timemargin=5000 \
  -openings file="$DIR/scripts/krk-openings.epd" format=epd order=sequential \
  -rounds "$ROUNDS" -games 2 -repeat \
  -pgnout "$DIR/target/match.pgn"
