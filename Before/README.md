# Before — the naive starting point of the case study

This directory holds a King + Rook vs King engine written the way a competent chess programmer writes a
first draft: three classes in one package, no architecture. It is the **starting point** of the case study of
the paper (Section 5.5): the meta-principle's constraints are applied to it to *identify*
violations, which are then *fixed* by refactoring towards the clean engine in `After/`, the same functionality with the architecture of the engine at the repository root.

It plays exactly like the clean engine's `tablebase` strategy on KRK: same perfect moves, same game lengths.
Only its structure differs.

## Contents

| File | Role |
|---|---|
| `src/main/java/org/chesskrk/naive/Main.java` | UCI loop on stdin/stdout; `send(String)` writes a line to the GUI |
| `src/main/java/org/chesskrk/naive/Engine.java` | the position, the rules of KRK, FEN and move parsing, and the move choice by tablebase lookup |
| `src/main/java/org/chesskrk/naive/Tablebase.java` | the retrograde distance-to-mate table, built ply by ply; prints its progress through `Main.send` |
| `src/test/java/…/NaiveEngineTest.java` | four behavioural tests (mate in one, GUI sequence, invalid input, every KRK position won) |
| `krk-naive.sh` | launcher for cutechess |

The violations this code contains on purpose are catalogued in [VIOLATIONS.md](VIOLATIONS.md).

## Build and run

```sh
cd Before
mvn -q package                                       # 4 tests, target/krk-naive.jar
printf 'uci\nisready\nposition fen k7/7R/1K6/8/8/8/8/8 w - - 0 1\ngo\nquit\n' | java -jar target/krk-naive.jar
```

## Black-box equivalence with the clean engine

The same structure-agnostic checks pass on both jars:

```sh
scripts/audit-hung-rook.py Before/target/krk-naive.jar    # positions 175168 bad 0
cutechess-cli -engine name=naive cmd=Before/krk-naive.sh -engine name=clean cmd=scripts/krk-engine.sh \
  -each proto=uci tc=inf st=5 -openings file=scripts/krk-openings.epd format=epd -rounds 8 -games 2 -repeat
```

In the match, every opening is won by White in the same number of plies whichever engine plays White
(e.g. 25 plies from `8/8/8/3k4/8/8/4K3/R7 w`, the tablebase distance). These checks are the safety net for
the refactoring steps: they must still pass after each one.
