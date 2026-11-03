# After — the naive engine, refactored

This directory holds the King + Rook vs King engine with exactly the functionality of [`Before/`](../Before)
and the architecture of the engine at the repository root: the four rings `domain`, `application` (ports and
one use case), `adapter` (`uci`, `tablebase`) and `infrastructure`. It is the **end state** of the
identify-then-fix experiment of the paper (Section 5.5): `Before/` and `After/` differ by
refactoring only, not by functionality.

The code is the engine's initial commit (`d72da8a`), which was written before the naive version; the naive
version was then written to behave like it. Everything the root engine has beyond this, the alpha-beta and
random strategies, the heuristic, any pawnless material, the piece catalogue, the tablebase cache and the
protocol log, came afterwards as features, each inside its axis.

## Build, run, check

```sh
cd After
mvn -q package                                        # 33 tests, target/krk-after.jar
printf 'uci\nisready\nposition fen k7/7R/1K6/8/8/8/8/8 w - - 0 1\ngo\nquit\n' | java -jar target/krk-after.jar
```

Black-box equivalence with `Before/`: `scripts/Equivalence.java` drives both jars over UCI on all 175,168 legal
white-to-move KRK positions with rules and a distance-to-mate table of its own, and requires every answer to be
a legal, mate-optimal move:

```sh
java scripts/Equivalence.java Before/target/krk-naive.jar After/target/krk-after.jar
# ... positions 175168 bad 0 same-move 173365
```

Puck, with [`decouple.wld`](decouple.wld), the same clauses as the root file and the sets of this version
(`valuation` is the tablebase alone, `pieces` is empty), reports no violation: `scripts/puck-both.sh After`.
